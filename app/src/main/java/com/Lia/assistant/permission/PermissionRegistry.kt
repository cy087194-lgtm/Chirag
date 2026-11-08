package com.Lia.assistant.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.Lia.assistant.accessibility.LiaAccessibilityService

enum class PermissionType {
    RUNTIME,
    SPECIAL_SETTINGS
}

enum class PermissionState(val label: String) {
    GRANTED("Granted"),
    NOT_GRANTED("Not granted"),
    REQUIRES_SETTINGS("Requires system settings"),
    RESTRICTED("Restricted by Android"),
    NOT_SUPPORTED("Not supported on this device")
}

data class PermissionItemModel(
    val id: String,
    val name: String,
    val category: String, // "CORE" or "SPECIAL"
    val purpose: String,
    val dependentFeatures: List<String>,
    val minSdk: Int = 26,
    val permissionType: PermissionType,
    val manifestPermission: String? = null,
    val settingsAction: String? = null,
    val troubleshooting: String
)

object PermissionRegistry {

    fun getItems(): List<PermissionItemModel> {
        return listOf(
            // SPECIAL ACCESS
            PermissionItemModel(
                id = "accessibility",
                name = "Accessibility Service",
                category = "SPECIAL",
                purpose = "Allows LiaAI to assist with hands-free phone control, reading screen elements, and tapping UI buttons by voice.",
                dependentFeatures = listOf("Voice Phone Control", "Read Screen", "Tap Buttons by Voice", "Recent Apps & Back Navigation"),
                minSdk = 26,
                permissionType = PermissionType.SPECIAL_SETTINGS,
                settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
                troubleshooting = "Go to Settings -> Accessibility -> Downloaded Apps / Installed Apps -> Select 'LiaAI' and toggle ON."
            ),
            PermissionItemModel(
                id = "overlay",
                name = "Display Over Other Apps",
                category = "SPECIAL",
                purpose = "Enables floating assistant orb and overlay glowing indicators on top of other running apps.",
                dependentFeatures = listOf("Floating Assistant Orb", "Overlay Glow", "Background Voice Feedback"),
                minSdk = 26,
                permissionType = PermissionType.SPECIAL_SETTINGS,
                settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                troubleshooting = "Grant 'Allow display over other apps' so LiaAI can remain visible while you use other apps."
            ),
            PermissionItemModel(
                id = "battery",
                name = "Battery Optimization Exemption",
                category = "SPECIAL",
                purpose = "Prevents Android from killing the background voice listening service during long-running tasks.",
                dependentFeatures = listOf("Always-On Voice Listening", "Background Task Execution", "Foreground Service Reliability"),
                minSdk = 26,
                permissionType = PermissionType.SPECIAL_SETTINGS,
                settingsAction = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                troubleshooting = "Select 'Don't optimize' or 'Unrestricted' battery usage for LiaAI."
            ),

            // CORE PERMISSIONS
            PermissionItemModel(
                id = "microphone",
                name = "Microphone",
                category = "CORE",
                purpose = "Captures your voice input in real-time for Gemini Live full-duplex conversations.",
                dependentFeatures = listOf("Gemini Live Voice Mode", "Voice Commands", "Audio IO"),
                minSdk = 26,
                permissionType = PermissionType.RUNTIME,
                manifestPermission = Manifest.permission.RECORD_AUDIO,
                troubleshooting = "Microphone access is required for voice conversation. Tap 'Grant' to allow."
            ),
            PermissionItemModel(
                id = "contacts",
                name = "Contacts",
                category = "CORE",
                purpose = "Resolves contact names and phone numbers when you ask LiaAI to call or message someone.",
                dependentFeatures = listOf("Call by Name ('Rahul ko call karo')", "WhatsApp Messaging by Contact Name"),
                minSdk = 26,
                permissionType = PermissionType.RUNTIME,
                manifestPermission = Manifest.permission.READ_CONTACTS,
                troubleshooting = "Allow Contacts permission so LiaAI can look up phone numbers by contact name."
            ),
            PermissionItemModel(
                id = "phone_call",
                name = "Direct Phone Calls",
                category = "CORE",
                purpose = "Places phone calls directly to contacts without manually tapping the dialer.",
                dependentFeatures = listOf("Voice Calling ('Call Rahul')", "Emergency / Quick Dialing"),
                minSdk = 26,
                permissionType = PermissionType.RUNTIME,
                manifestPermission = Manifest.permission.CALL_PHONE,
                troubleshooting = "Required if you want LiaAI to directly place voice calls on command."
            ),
            PermissionItemModel(
                id = "notifications",
                name = "Notifications",
                category = "CORE",
                purpose = "Displays foreground service alerts, task progress, and assistant status notifications.",
                dependentFeatures = listOf("Foreground Voice Service", "Task Completion Alerts"),
                minSdk = 33, // Android 13+
                permissionType = PermissionType.RUNTIME,
                manifestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else null,
                troubleshooting = "Enable Notifications so the assistant foreground service status remains visible."
            ),
            PermissionItemModel(
                id = "camera",
                name = "Camera Hardware Access",
                category = "CORE",
                purpose = "Controls phone flashlight (torch) and camera photo capture upon voice request.",
                dependentFeatures = listOf("Flashlight / Torch Toggle ('torch on/off')", "Quick Camera Capture ('camera khol')"),
                minSdk = 26,
                permissionType = PermissionType.RUNTIME,
                manifestPermission = Manifest.permission.CAMERA,
                troubleshooting = "Camera permission enables the flashlight controller and camera launcher."
            )
        )
    }

    fun checkStatus(context: Context, item: PermissionItemModel): PermissionState {
        if (Build.VERSION.SDK_INT < item.minSdk && item.minSdk > 26) {
            return PermissionState.NOT_SUPPORTED
        }

        return when (item.id) {
            "accessibility" -> {
                val isServiceConnected = LiaAccessibilityService.instance != null
                val isEnabledInSettings = LiaAccessibilityService.isEnabledInSettings(context)
                if (isServiceConnected || isEnabledInSettings) {
                    PermissionState.GRANTED
                } else {
                    PermissionState.REQUIRES_SETTINGS
                }
            }
            "overlay" -> {
                if (Settings.canDrawOverlays(context)) {
                    PermissionState.GRANTED
                } else {
                    PermissionState.REQUIRES_SETTINGS
                }
            }
            "battery" -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm?.isIgnoringBatteryOptimizations(context.packageName) == true) {
                    PermissionState.GRANTED
                } else {
                    PermissionState.REQUIRES_SETTINGS
                }
            }
            else -> {
                val perm = item.manifestPermission
                if (perm == null) {
                    PermissionState.NOT_SUPPORTED
                } else if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                    PermissionState.GRANTED
                } else {
                    PermissionState.NOT_GRANTED
                }
            }
        }
    }

    fun openSettingsForItem(context: Context, item: PermissionItemModel) {
        try {
            when (item.id) {
                "accessibility" -> {
                    LiaAccessibilityService.openAccessibilitySettings(context)
                }
                "overlay" -> {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                }
                "battery" -> {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                else -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            }
        } catch (_: Exception) {
            // Fallback to app details settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
