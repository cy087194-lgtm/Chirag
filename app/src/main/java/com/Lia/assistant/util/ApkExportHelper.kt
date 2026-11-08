package com.Lia.assistant.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ApkExportHelper {
    private const val TAG = "ApkExportHelper"

    /**
     * Shares the current app's APK installer via standard Android share chooser
     * (WhatsApp, Telegram, Google Drive, Bluetooth, Gmail, etc.)
     */
    fun shareApk(context: Context): Boolean {
        return try {
            val sourceApk = File(context.applicationInfo.sourceDir)
            if (!sourceApk.exists()) {
                Log.e(TAG, "Source APK not found at ${sourceApk.absolutePath}")
                Toast.makeText(context, "APK file not found on system", Toast.LENGTH_SHORT).show()
                return false
            }

            val sharedDir = File(context.cacheDir, "shared")
            if (!sharedDir.exists()) sharedDir.mkdirs()

            val targetApk = File(sharedDir, "LiaAI_v1.0.0.apk")
            sourceApk.copyTo(targetApk, overwrite = true)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetApk
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LiaAI APK Installer")
                putExtra(Intent.EXTRA_TEXT, "LiaAI Screen & Voice Assistant APK. Install to get full phone control and gaming coach features.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(sendIntent, "Share / Send LiaAI APK").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share APK: ${e.message}", e)
            Toast.makeText(context, "Could not export APK: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Saves a copy of the APK to the public Downloads folder if accessible.
     */
    fun saveToDownloads(context: Context): String? {
        return try {
            val sourceApk = File(context.applicationInfo.sourceDir)
            if (!sourceApk.exists()) return null

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                val destFile = File(downloadsDir, "LiaAI_Assistant_v1.0.0.apk")
                sourceApk.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy to Downloads directly: ${e.message}")
            null
        }
    }

    /**
     * Returns clear, comprehensive Hindi & English troubleshooting instructions
     * for downloading and installing the APK.
     */
    fun getTroubleshootingGuide(): String {
        return """
📱 **LiaAI APK Download & Install Fix Guide**:

1️⃣ **AI Studio se APK kaise download karein**:
• AI Studio ke top-right corner mein **Settings (gear icon)** ya **Export** menu par click karein.
• **"Download APK"** select karein. `app-debug.apk` aapke computer/phone mein download ho jayegi.

2️⃣ **"File might be harmful" Warning**:
• Play Store ke alawa kisi bhi direct APK par Chrome ye warning deta hai.
• Bina dare **"Download anyway"** par tap karein.

3️⃣ **"Blocked by Play Protect" Warning**:
• Install karte waqt agar Play Protect block kare, to:
  1. **"More details"** (या 'अधिक विवरण') par tap karein.
  2. Neeche **"Install anyway"** (या 'फिर भी इंस्टॉल करें') par tap karein.

4️⃣ **Android 13, 14, 15 "Restricted Setting" Fix**:
• App install hone ke baad Phone Settings → Apps → LiaAI kholein.
• Top-right corner ke **3 dots (⋮)** par tap karein → **"Allow restricted settings"** enable karein.
• Ab Phone Settings → Accessibility → Downloaded Apps mein jakar **LiaAI** ko ON kar dein.
        """.trimIndent()
    }
}
