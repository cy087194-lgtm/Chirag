package com.Lia.assistant.action

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import java.net.URLEncoder

object CommIntents {

    @SuppressLint("MissingPermission")
    fun placeCall(context: Context, phoneNumber: String): String {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        return if (hasCallPermission) {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(callIntent)
                "calling"
            } catch (_: Exception) {
                fallbackDial(context, cleanNumber)
            }
        } else {
            fallbackDial(context, cleanNumber)
        }
    }

    private fun fallbackDial(context: Context, cleanNumber: String): String {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            "dialer_opened_no_permission"
        } catch (_: Exception) {
            "failed_to_open_dialer"
        }
    }

    fun openSmsComposer(context: Context, recipient: String, message: String): String {
        return try {
            val cleanRecipient = recipient.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanRecipient")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "composer_opened"
        } catch (_: Exception) {
            "sms_failed"
        }
    }

    fun openWhatsAppComposer(context: Context, recipient: String, message: String): String {
        return try {
            val digitsOnly = recipient.filter { it.isDigit() }
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = if (digitsOnly.isNotEmpty()) {
                "https://wa.me/$digitsOnly?text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
            val installedWa = waPackages.firstOrNull { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            }

            if (installedWa != null) {
                intent.setPackage(installedWa)
            }

            context.startActivity(intent)
            "composer_opened"
        } catch (_: Exception) {
            // Fallback to SMS composer if WhatsApp fails
            openSmsComposer(context, recipient, message)
        }
    }
}
