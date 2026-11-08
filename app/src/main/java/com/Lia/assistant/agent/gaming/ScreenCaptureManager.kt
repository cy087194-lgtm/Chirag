package com.Lia.assistant.agent.gaming

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import com.Lia.assistant.accessibility.LiaAccessibilityService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

sealed class FrameResult {
    data class Fresh(val bitmap: Bitmap, val jpegBytes: ByteArray, val diffPercent: Float, val adaptiveIntervalMs: Long) : FrameResult()
    data class Unchanged(val diffPercent: Float, val adaptiveIntervalMs: Long) : FrameResult()
    object BlockedOrEmpty : FrameResult()
    object CaptureFailed : FrameResult()
}

object ScreenCaptureManager {
    private const val TAG = "ScreenCaptureManager"
    private const val TARGET_WIDTH = 768

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var previousHash16x16: IntArray? = null
    private val screenshotExecutor = Executors.newSingleThreadExecutor()

    fun hasActiveMediaProjection(): Boolean {
        return mediaProjection != null && imageReader != null
    }

    fun initMediaProjection(resultCode: Int, data: android.content.Intent, context: Context) {
        try {
            val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? android.media.projection.MediaProjectionManager
            val mp = mpManager?.getMediaProjection(resultCode, data)
            setMediaProjection(mp, context)
            Log.i(TAG, "MediaProjection successfully initialized with virtual display")
        } catch (e: Throwable) {
            Log.e(TAG, "initMediaProjection failed: ${e.message}", e)
        }
    }

    fun setMediaProjection(mp: MediaProjection?, context: Context) {
        releaseMediaProjection()
        mediaProjection = mp
        if (mp != null) {
            setupVirtualDisplay(context, mp)
        }
    }

    private fun setupVirtualDisplay(context: Context, mp: MediaProjection) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealMetrics(metrics) ?: run {
                metrics.widthPixels = 1080
                metrics.heightPixels = 2400
                metrics.densityDpi = DisplayMetrics.DENSITY_DEFAULT
            }

            val width = 720
            val height = (width * (metrics.heightPixels.toFloat() / max(1, metrics.widthPixels))).roundToInt()
            val density = metrics.densityDpi

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mp.createVirtualDisplay(
                "CoachScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            Log.i(TAG, "VirtualDisplay created: ${width}x$height @ $density dpi")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to setup VirtualDisplay: ${e.message}", e)
        }
    }

    /**
     * Captures a fresh frame. Never returns a cached image.
     */
    suspend fun captureFrame(context: Context): FrameResult = withContext(Dispatchers.Default) {
        val rawBitmap = acquireRawBitmap(context)
        if (rawBitmap == null) {
            return@withContext FrameResult.CaptureFailed
        }

        // 1. Check for empty / black frame (FLAG_SECURE or failed render)
        if (isFrameBlackOrEmpty(rawBitmap)) {
            Log.w(TAG, "screen capture blocked or failed")
            return@withContext FrameResult.BlockedOrEmpty
        }

        // 2. Compute 16x16 grayscale hash & perceptual diff against previous frame
        val currentHash = compute16x16Hash(rawBitmap)
        val diffPercent = computeHashDiffPercent(previousHash16x16, currentHash)
        previousHash16x16 = currentHash

        // Adaptive interval: 1000ms if fast changing, up to 3000ms if calm
        val adaptiveIntervalMs = when {
            diffPercent > 15f -> 1000L
            diffPercent > 7f -> 1800L
            else -> 2800L
        }

        // If screen has not changed meaningfully (< 3.5%), return Unchanged
        if (diffPercent < 3.5f) {
            return@withContext FrameResult.Unchanged(diffPercent, adaptiveIntervalMs)
        }

        // 3. Downscale to ~768px width keeping aspect ratio for speed & efficiency
        val scaledBitmap = if (rawBitmap.width > TARGET_WIDTH) {
            val aspect = rawBitmap.height.toFloat() / rawBitmap.width
            val targetHeight = (TARGET_WIDTH * aspect).roundToInt()
            Bitmap.createScaledBitmap(rawBitmap, TARGET_WIDTH, targetHeight, true)
        } else {
            rawBitmap
        }

        // 4. Compress to JPEG
        val stream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val jpegBytes = stream.toByteArray()

        // Update debug holder preview
        CoachDebugHolder.updateFrame(scaledBitmap, diffPercent)

        FrameResult.Fresh(
            bitmap = scaledBitmap,
            jpegBytes = jpegBytes,
            diffPercent = diffPercent,
            adaptiveIntervalMs = adaptiveIntervalMs
        )
    }

    private suspend fun acquireRawBitmap(context: Context): Bitmap? {
        // Try ImageReader first if MediaProjection is active
        val reader = imageReader
        if (reader != null) {
            try {
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val bmp = imageToBitmap(image)
                    image.close()
                    if (bmp != null) return bmp
                }
            } catch (e: Throwable) {
                Log.w(TAG, "acquireLatestImage error: ${e.message}")
            }
        }

        // Fallback to LiaAccessibilityService.takeScreenshot on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val service = LiaAccessibilityService.instance
            if (service != null) {
                return captureFromAccessibilityService(service)
            }
        }

        return null
    }

    private suspend fun captureFromAccessibilityService(service: LiaAccessibilityService): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val deferred = CompletableDeferred<Bitmap?>()

        try {
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                screenshotExecutor,
                object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: android.accessibilityservice.AccessibilityService.ScreenshotResult) {
                        try {
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val hwBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                            val softwareBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                            hardwareBuffer.close()
                            deferred.complete(softwareBitmap)
                        } catch (e: Throwable) {
                            deferred.complete(null)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        deferred.complete(null)
                    }
                }
            )
        } catch (e: Throwable) {
            return null
        }

        return withTimeoutOrNull(2000L) {
            deferred.await()
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            // Crop out padding if necessary
            if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            } else {
                bitmap
            }
        } catch (e: Throwable) {
            Log.w(TAG, "imageToBitmap error: ${e.message}")
            null
        }
    }

    private fun isFrameBlackOrEmpty(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return true

        var totalLum = 0L
        val samplePoints = 25
        val stepX = max(1, w / 5)
        val stepY = max(1, h / 5)

        for (x in 0 until 5) {
            for (y in 0 until 5) {
                val px = bitmap.getPixel(x * stepX, y * stepY)
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)
                totalLum += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
            }
        }
        val avgLum = totalLum / samplePoints
        return avgLum < 6 // Less than 6 brightness is considered black / blank screen
    }

    private fun compute16x16Hash(bitmap: Bitmap): IntArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        val hash = IntArray(256)
        var idx = 0
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val px = scaled.getPixel(x, y)
                val lum = (0.299 * Color.red(px) + 0.587 * Color.green(px) + 0.114 * Color.blue(px)).toInt()
                hash[idx++] = lum
            }
        }
        return hash
    }

    fun computeHashDiffPercent(prev: IntArray?, curr: IntArray): Float {
        if (prev == null || prev.size != curr.size) return 100f
        var diffSum = 0.0
        for (i in prev.indices) {
            diffSum += abs(prev[i] - curr[i])
        }
        val maxDiff = prev.size * 255.0
        return ((diffSum / maxDiff) * 100.0).toFloat()
    }

    fun resetHash() {
        previousHash16x16 = null
    }

    fun releaseMediaProjection() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
            previousHash16x16 = null
            Log.i(TAG, "Screen capture resources released")
        } catch (e: Throwable) {
            Log.w(TAG, "Error releasing MediaProjection: ${e.message}")
        }
    }
}
