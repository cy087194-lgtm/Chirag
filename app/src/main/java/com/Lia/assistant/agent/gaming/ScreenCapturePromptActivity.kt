package com.Lia.assistant.agent.gaming

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.Lia.assistant.agent.core.LiveStatusHolder

class ScreenCapturePromptActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ScreenCapturePrompt"

        fun requestScreenCapture(context: Context) {
            val intent = Intent(context, ScreenCapturePromptActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Log.i(TAG, "MediaProjection permission granted by user")
            val data = result.data!!
            ScreenCaptureManager.initMediaProjection(result.resultCode, data, this)
            AllRounderCoachService.startWithTokens(this, result.resultCode, data)
            LiveStatusHolder.updatePermissions(
                accessibility = LiveStatusHolder.accessibilityActive.value,
                capture = true,
                contacts = LiveStatusHolder.contactsGranted.value,
                overlay = LiveStatusHolder.overlayGranted.value
            )
            Toast.makeText(this, "Screen Coach Active: Watching screen", Toast.LENGTH_SHORT).show()
        } else {
            Log.w(TAG, "MediaProjection permission denied or cancelled")
            LiveStatusHolder.updatePermissions(
                accessibility = LiveStatusHolder.accessibilityActive.value,
                capture = false,
                contacts = LiveStatusHolder.contactsGranted.value,
                overlay = LiveStatusHolder.overlayGranted.value
            )
            AllRounderCoachEngine.notifyCaptureMissing(this)
            Toast.makeText(this, "Screen capture permission band hai", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (mpManager != null) {
                captureLauncher.launch(mpManager.createScreenCaptureIntent())
            } else {
                Toast.makeText(this, "MediaProjection not supported on this device", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to launch screen capture intent: ${e.message}", e)
            finish()
        }
    }
}
