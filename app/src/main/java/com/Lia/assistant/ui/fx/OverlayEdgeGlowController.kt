package com.Lia.assistant.ui.fx

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

/**
 * Custom lifecycle owner so Jetpack ComposeView hosted in WindowManager
 * from a Service has valid ViewTreeLifecycleOwner, ViewTreeSavedStateRegistryOwner,
 * and ViewTreeViewModelStoreOwner. Prevents fatal IllegalStateException crash on attach.
 */
private class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun destroy() {
        try {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        } catch (_: Throwable) {}
    }
}

object OverlayEdgeGlowController {

    private const val TAG = "OverlayController"
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var progressObserverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun isOverlayPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Settings.canDrawOverlays(context)
            } catch (_: Throwable) {
                false
            }
        } else true
    }

    @SuppressLint("InflateParams")
    fun start(context: Context) {
        if (!isOverlayPermissionGranted(context)) {
            Log.d(TAG, "Overlay permission not granted. Running in-app UI only.")
            return
        }

        if (overlayView != null) return

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val wm = windowManager ?: return

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 24
                y = 120
            }

            val owner = OverlayLifecycleOwner()
            lifecycleOwner = owner

            val container = FrameLayout(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            container.setViewTreeLifecycleOwner(owner)
            container.setViewTreeSavedStateRegistryOwner(owner)
            container.setViewTreeViewModelStoreOwner(owner)

            val composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setContent {
                    NovaOrb(
                        state = com.Lia.assistant.voice.NovaOrbState.THINKING,
                        size = 56.dp,
                        assistantName = "Lia"
                    )
                }
            }

            container.addView(composeView)
            wm.addView(container, params)
            overlayView = container
            Log.i(TAG, "Floating Overlay Orb added to WindowManager with valid LifecycleOwner")

        } catch (e: Throwable) {
            Log.w(TAG, "Failed to display floating overlay: ${e.message}")
            try {
                lifecycleOwner?.destroy()
            } catch (_: Throwable) {}
            lifecycleOwner = null
            overlayView = null
        }
    }

    fun stop(context: Context) {
        progressObserverJob?.cancel()
        progressObserverJob = null
        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
                Log.i(TAG, "Floating Overlay Orb removed from WindowManager")
            } catch (e: Throwable) {
                Log.w(TAG, "Error removing overlay view: ${e.message}")
            }
            overlayView = null
        }
        try {
            lifecycleOwner?.destroy()
        } catch (_: Throwable) {}
        lifecycleOwner = null
    }
}
