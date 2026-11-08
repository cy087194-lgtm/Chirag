package com.Lia.assistant.agent.demonstration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.Lia.assistant.R
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.agent.core.LiveStatusHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

object DemonstrationRecorder {
    private const val TAG = "DemonstrationRecorder"
    private const val NOTIFICATION_ID = 3003
    private const val CHANNEL_ID = "lia_demonstration_channel"

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedCount = MutableStateFlow(0)
    val recordedCount: StateFlow<Int> = _recordedCount.asStateFlow()

    private val rawActions = mutableListOf<RecordedAction>()
    private var lastRecordedTime = 0L
    private var tts: TextToSpeech? = null

    // Sensitive & Protected Packages (Banking, UPI, Auth)
    private val SENSITIVE_PACKAGES = setOf(
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user",
        "in.org.npci.upiapp",
        "com.sbi.upi",
        "com.msf.kbank.mobile",
        "com.icicibank.mobile",
        "com.axis.mobile",
        "com.snapwork.hdfc",
        "com.google.android.gms.auth"
    )

    fun isPackageSensitive(pkg: String): Boolean {
        return SENSITIVE_PACKAGES.any { pkg.contains(it) }
    }

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("hi", "IN")
                }
            }
        }
    }

    private fun speakDirect(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "demo_speech")
            LiveStatusHolder.updateSpokenTip(text)
        } catch (_: Throwable) {}
    }

    fun startLearning(context: Context): String {
        initTts(context)
        val service = LiaAccessibilityService.instance
        if (service == null) {
            val msg = "Seekhne ke liye LiaAI ki Accessibility permission enable honi chahiye."
            speakDirect(msg)
            return msg
        }

        synchronized(rawActions) {
            rawActions.clear()
            _recordedCount.value = 0
            _isRecording.value = true
        }

        showLearningNotification(context)
        val startMsg = "Main dekh rahi hoon. Jo sikhana hai, screen par perform karein."
        speakDirect(startMsg)
        LiveStatusHolder.recordToolExecution("demonstration_recorder", "ALL", "RECORDING_STARTED", 0L)
        return startMsg
    }

    fun stopLearning(context: Context): Pair<List<RecordedAction>, String> {
        dismissLearningNotification(context)

        val captured: List<RecordedAction>
        synchronized(rawActions) {
            _isRecording.value = false
            captured = ArrayList(rawActions)
        }

        if (captured.isEmpty()) {
            val emptyMsg = "Recording mein koi action capture nahi hua."
            speakDirect(emptyMsg)
            return Pair(emptyList(), emptyMsg)
        }

        val stopMsg = "Maine ${captured.size} steps record kar liye hain. Routine compile kar rahi hoon..."
        speakDirect(stopMsg)
        LiveStatusHolder.recordToolExecution("demonstration_recorder", "ALL", "RECORDING_STOPPED", captured.size.toLong())
        return Pair(captured, stopMsg)
    }

    fun onAccessibilityEvent(event: AccessibilityEvent, service: LiaAccessibilityService) {
        if (!_isRecording.value) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg == "com.Lia.assistant") return // Ignore our own app internal touches

        // 1. Privacy Guard: Banking, UPI, and sensitive screens
        if (SENSITIVE_PACKAGES.any { pkg.contains(it) }) {
            Log.w(TAG, "Sensitive package detected ($pkg). Skipping demonstration record for security.")
            return
        }

        // 2. Privacy Guard: Password and PIN fields
        if (event.isPassword) {
            Log.w(TAG, "Password field detected. Skipping recording.")
            return
        }

        val source = event.source
        if (source != null && source.isPassword) {
            Log.w(TAG, "Source node is marked as password. Skipping recording.")
            return
        }

        val now = System.currentTimeMillis()

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (now - lastRecordedTime < 180) return // Debounce rapid multi-touches
                lastRecordedTime = now

                val text = source?.text?.toString()?.trim()
                val desc = source?.contentDescription?.toString()?.trim()
                val viewId = source?.viewIdResourceName
                val rect = Rect()
                source?.getBoundsInScreen(rect)

                val action = RecordedAction(
                    type = RecordedActionType.CLICK,
                    packageName = pkg,
                    text = text?.ifEmpty { null },
                    contentDescription = desc?.ifEmpty { null },
                    viewId = viewId?.ifEmpty { null },
                    boundsCenterX = rect.centerX().toFloat(),
                    boundsCenterY = rect.centerY().toFloat(),
                    timestamp = now
                )

                recordAction(action)
            }

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val enteredText = event.text.joinToString("") { it.toString() }
                if (enteredText.isNotEmpty()) {
                    synchronized(rawActions) {
                        // Merge with previous typing step if for same package within 2 seconds
                        val last = rawActions.lastOrNull()
                        if (last != null && last.type == RecordedActionType.TYPE_TEXT && last.packageName == pkg && now - last.timestamp < 3000) {
                            rawActions.removeAt(rawActions.lastIndex)
                        }
                    }

                    val action = RecordedAction(
                        type = RecordedActionType.TYPE_TEXT,
                        packageName = pkg,
                        inputText = enteredText,
                        viewId = source?.viewIdResourceName,
                        timestamp = now
                    )
                    recordAction(action)
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (now - lastRecordedTime < 500) return
                lastRecordedTime = now

                val action = RecordedAction(
                    type = RecordedActionType.SCROLL_FORWARD,
                    packageName = pkg,
                    timestamp = now
                )
                recordAction(action)
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // If app package changed, record launch
                synchronized(rawActions) {
                    val last = rawActions.lastOrNull()
                    if (last == null || last.packageName != pkg) {
                        rawActions.add(
                            RecordedAction(
                                type = RecordedActionType.LAUNCH_APP,
                                packageName = pkg,
                                timestamp = now
                            )
                        )
                        _recordedCount.value = rawActions.size
                    }
                }
            }
        }
    }

    private fun recordAction(action: RecordedAction) {
        synchronized(rawActions) {
            rawActions.add(action)
            _recordedCount.value = rawActions.size
            Log.i(TAG, "Recorded action #${rawActions.size}: ${action.type} in ${action.packageName}")
        }
    }

    private fun showLearningNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "LiaAI Teach Mode",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows when LiaAI is recording a demonstrated routine"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("LiaAI Teach Mode Active (Learning)")
                .setContentText("Recording steps on screen. Say 'seekhna band karo' when done.")
                .setOngoing(true)
                .setColor(0xFFFF4444.toInt()) // Red indicator
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Throwable) {}
    }

    private fun dismissLearningNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Throwable) {}
    }
}
