package com.Lia.assistant.agent.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.action.AppLauncher
import com.Lia.assistant.action.InstalledAppLabelCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SystemAndDailyEngine {

    private const val TAG = "SystemAndDailyEngine"

    private val _isFocusModeActive = MutableStateFlow(false)
    val isFocusModeActive: StateFlow<Boolean> = _isFocusModeActive.asStateFlow()

    private val blockedAppsInFocusMode = listOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.zhiliaoapp.musically",
        "com.google.android.youtube",
        "com.dts.freefireth",
        "com.pubg.imobile"
    )

    /**
     * Toggles Focus Mode ("Padhai mode"). When active, distracting apps are detected and minimized.
     */
    fun toggleFocusMode(enable: Boolean): String {
        _isFocusModeActive.value = enable
        return if (enable) {
            "Padhai Mode (Focus Mode) chalu kar diya gaya hai. Social media aur games block rahenge."
        } else {
            "Padhai Mode band ho gaya hai."
        }
    }

    /**
     * Checks if current foreground app is restricted in Focus Mode.
     */
    fun checkFocusModeEnforcement(packageName: String): Boolean {
        if (!_isFocusModeActive.value) return false
        if (blockedAppsInFocusMode.any { packageName.contains(it) }) {
            Log.i(TAG, "Focus mode blocked app: $packageName")
            LiaAccessibilityService.instance?.performHome()
            return true
        }
        return false
    }

    /**
     * Scans cache directories for storage cleanup recommendations.
     */
    fun scanStorage(context: Context): String {
        val cacheDir = context.cacheDir
        val cacheSize = cacheDir?.walkTopDown()?.filter { it.isFile }?.map { it.length() }?.sum() ?: 0L
        val mb = cacheSize / (1024 * 1024)
        return "Storage Analysis: App cache mein lagbhag ${mb}MB temporary files hain. Device internal storage cleaner settings se junk files clean ki ja sakti hain."
    }

    /**
     * Sets a Smart Alarm (e.g. "kal 6 baje, barish ho to 6:30").
     */
    fun setSmartAlarm(context: Context, hour: Int, minute: Int, message: String = "Lia Smart Alarm"): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens Cab Booking (Uber / Ola).
     */
    suspend fun bookCab(context: Context, destination: String): String {
        val apps = InstalledAppLabelCache.get(context)
        val uberPkg = AppLauncher.findApp("uber", apps)
        val olaPkg = AppLauncher.findApp("ola", apps)

        if (uberPkg != null) {
            AppLauncher.launch(context, uberPkg)
            return "Uber app launch kar di gayi hai. Destination: $destination."
        } else if (olaPkg != null) {
            AppLauncher.launch(context, olaPkg)
            return "Ola app launch kar di gayi hai. Destination: $destination."
        } else {
            // Web fallback
            val uri = Uri.parse("https://m.uber.com/looking")
            val webIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            return "Cab booking web portal open kar diya gaya hai."
        }
    }

    /**
     * Food ordering flow helper (Zomato/Swiggy): navigates to cart and stops at payment.
     */
    suspend fun openFoodOrderCart(context: Context, appName: String): String {
        val apps = InstalledAppLabelCache.get(context)
        val targetApp = if (appName.contains("swiggy")) "swiggy" else "zomato"
        val pkg = AppLauncher.findApp(targetApp, apps)
        if (pkg != null) {
            AppLauncher.launch(context, pkg)
            return "$targetApp open ho gaya hai. Cart check karein; payment step par confirmation aayega."
        }
        return "$targetApp phone mein installed nahi mila."
    }

    /**
     * Drafts birthday wish message.
     */
    fun draftBirthdayWish(name: String): String {
        return "Janamdin ki bohot bohot shubhkamnayein $name! Bhagwan aapko khushiyan, sehat aur lambi umar de. Have a great year ahead! 🎂🎉"
    }

    /**
     * Emergency alert trigger: prepares emergency message for trusted contact.
     */
    fun triggerEmergencyAlert(context: Context, emergencyContactNumber: String): String {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$emergencyContactNumber")
            putExtra("sms_body", "EMERGENCY: Mujhe turant help ki zaroorat hai. Kripya mujhse contact karein.")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Emergency alert draft SMS open kar diya gaya hai contact $emergencyContactNumber ke liye."
    }
}
