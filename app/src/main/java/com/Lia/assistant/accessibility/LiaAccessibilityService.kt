package com.Lia.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccessibleNode(
    val text: String,
    val contentDescription: String,
    val viewId: String,
    val className: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val bounds: Rect
)

class LiaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        Log.i(TAG, "LiaAccessibilityService connected successfully")

        // Ensure proper flags are set at runtime
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track active window package if useful
        val pkg = event?.packageName?.toString()
        if (!pkg.isNullOrEmpty()) {
            _currentPackage.value = pkg
        }

        // PART Z: Teach by Demonstration recorder hook
        if (event != null) {
            try {
                com.Lia.assistant.agent.demonstration.DemonstrationRecorder.onAccessibilityEvent(event, this)
            } catch (_: Throwable) {}
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "LiaAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
            _isConnected.value = false
        }
        Log.i(TAG, "LiaAccessibilityService destroyed")
    }

    /**
     * Traverses the active window and collects accessible UI nodes.
     */
    fun readScreen(): List<AccessibleNode> {
        val root = rootInActiveWindow ?: return emptyList()
        val nodes = mutableListOf<AccessibleNode>()
        traverseNode(root, nodes)
        return nodes
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, list: MutableList<AccessibleNode>) {
        if (node == null) return

        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""
        val rect = Rect()
        node.getBoundsInScreen(rect)

        if (text.isNotEmpty() || desc.isNotEmpty() || node.isClickable || node.isEditable) {
            list.add(
                AccessibleNode(
                    text = text,
                    contentDescription = desc,
                    viewId = viewId,
                    className = className,
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    isScrollable = node.isScrollable,
                    bounds = rect
                )
            )
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), list)
        }
    }

    /**
     * Taps a node matching text or description. If the node isn't directly clickable,
     * checks clickable parents or falls back to a coordinate gesture.
     */
    fun tapText(targetText: String): String {
        val root = rootInActiveWindow ?: return "no_screen_content"
        val query = targetText.trim().lowercase()
        if (query.isEmpty()) return "empty_query"

        val matchingNode = findNodeMatchingText(root, query)
        if (matchingNode != null) {
            // 1. Try ACTION_CLICK on the node or its clickable parents
            var current: AccessibilityNodeInfo? = matchingNode
            while (current != null) {
                if (current.isClickable) {
                    val clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return "tapped"
                }
                current = current.parent
            }

            // 2. Fall back to gesture tap on the node's center coordinates
            val rect = Rect()
            matchingNode.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                val cx = rect.centerX().toFloat()
                val cy = rect.centerY().toFloat()
                val gestureSuccess = tapCoordinates(cx, cy)
                if (gestureSuccess) return "tapped_gesture"
            }
        }

        return "not_found"
    }

    private fun findNodeMatchingText(root: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (root == null) return null

        val text = root.text?.toString()?.lowercase() ?: ""
        val desc = root.contentDescription?.toString()?.lowercase() ?: ""

        if (text.contains(query) || desc.contains(query) || query.contains(text) && text.length > 2) {
            return root
        }

        for (i in 0 until root.childCount) {
            val childMatch = findNodeMatchingText(root.getChild(i), query)
            if (childMatch != null) return childMatch
        }
        return null
    }

    /**
     * Performs a touch tap gesture at screen coordinates (x, y).
     */
    fun tapCoordinates(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * Types text into the currently focused or editable field.
     */
    fun typeText(text: String): String {
        val root = rootInActiveWindow ?: return "no_screen_content"
        val target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditableNode(root)
            ?: return "no_editable_field"

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val success = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        return if (success) "typed" else "type_failed"
    }

    private fun findFirstEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isEditable) return root
        for (i in 0 until root.childCount) {
            val editable = findFirstEditableNode(root.getChild(i))
            if (editable != null) return editable
        }
        return null
    }

    /**
     * Scrolls the active window or scrollable container forward or backward.
     */
    fun scroll(direction: String): String {
        val root = rootInActiveWindow ?: return "no_screen_content"
        val scrollable = findFirstScrollableNode(root) ?: root
        val action = if (direction.equals("backward", ignoreCase = true) || direction.equals("up", ignoreCase = true)) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        val success = scrollable.performAction(action)
        return if (success) "scrolled" else "scroll_failed"
    }

    private fun findFirstScrollableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = findFirstScrollableNode(root.getChild(i))
            if (child != null) return child
        }
        return null
    }

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun performNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun performQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    fun findNode(query: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findNodeMatchingText(root, query.lowercase().trim())
    }

    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                val ok = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (ok) return true
            }
            current = current.parent
        }
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() > 0 && rect.height() > 0) {
            return tapCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat())
        }
        return false
    }

    fun setTextOnNode(node: AccessibilityNodeInfo, text: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun findNodeById(viewId: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val query = viewId.trim()
        if (query.isEmpty()) return null
        val direct = root.findAccessibilityNodeInfosByViewId(query)
        if (!direct.isNullOrEmpty()) {
            return direct[0]
        }
        return findNodeByIdRecursive(root, query.lowercase())
    }

    private fun findNodeByIdRecursive(node: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val resName = node.viewIdResourceName?.lowercase() ?: ""
        if (resName.contains(query)) return node
        for (i in 0 until node.childCount) {
            val match = findNodeByIdRecursive(node.getChild(i), query)
            if (match != null) return match
        }
        return null
    }

    fun findNodeByDesc(query: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val q = query.lowercase().trim()
        return findNodeByDescRecursive(root, q)
    }

    private fun findNodeByDescRecursive(node: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (desc.contains(query)) return node
        for (i in 0 until node.childCount) {
            val match = findNodeByDescRecursive(node.getChild(i), query)
            if (match != null) return match
        }
        return null
    }

    /**
     * Automatically dismisses ads, cookie banners, system dialogs, and rating popups.
     * Returns true if a popup or ad button was detected and dismissed.
     */
    fun dismissPopupsAndAds(): Boolean {
        val root = rootInActiveWindow ?: return false
        val popupKeywords = listOf(
            "skip ad", "skip ads", "skip in 5s", "close ad", "close", "skip",
            "not now", "no thanks", "allow while using the app", "allow",
            "got it", "accept", "dismiss", "update later", "cancel", "ok"
        )

        for (kw in popupKeywords) {
            val match = findNodeMatchingText(root, kw)
            if (match != null) {
                val clicked = clickNode(match)
                if (clicked) {
                    Log.i(TAG, "dismissPopupsAndAds: dismissed popup matching \"$kw\"")
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if a target action (like, subscribe) is already in the completed state.
     */
    fun isAlreadyDone(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val lower = target.lowercase().trim()
        if (lower.contains("subscribe")) {
            val subscribedNode = findNodeMatchingText(root, "subscribed")
                ?: findNodeMatchingText(root, "सदस्यता ली गई")
                ?: findNodeMatchingText(root, "unsubscribe")
            if (subscribedNode != null) return true
        } else if (lower.contains("like")) {
            val likedNode = findNodeMatchingText(root, "unlike")
                ?: findNodeMatchingText(root, "liked")
            if (likedNode != null) return true

            // Check if like button is selected
            val likeBtn = findNodeMatchingText(root, "like")
            if (likeBtn != null && (likeBtn.isSelected || likeBtn.isChecked)) return true
        }
        return false
    }

    /**
     * Clicks the first video result in YouTube or video search list.
     */
    fun clickFirstVideoItem(): Boolean {
        val root = rootInActiveWindow ?: return false
        // 1. Try finding video item containers by ID
        val videoIds = listOf(
            "com.google.android.youtube:id/thumbnail",
            "com.google.android.youtube:id/video_title",
            "com.google.android.youtube:id/compact_video_item",
            "thumbnail",
            "video_item"
        )
        for (id in videoIds) {
            val node = findNodeById(id)
            if (node != null) {
                val ok = clickNode(node)
                if (ok) return true
            }
        }

        // 2. Find first clickable child with video-like dimensions below search bar (y > 200)
        val candidate = findFirstVideoNodeRecursive(root)
        if (candidate != null) {
            return clickNode(candidate)
        }
        return false
    }

    private fun findFirstVideoNodeRecursive(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val rect = Rect()
        node.getBoundsInScreen(rect)
        // Video thumbnail/item is typically wide, substantial height, and below top bar
        if (node.isClickable && rect.top > 250 && rect.width() > 300 && rect.height() > 120) {
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val full = "$text $desc".lowercase()
            // Avoid clicking the search bar or tab bar
            if (!full.contains("search") && !full.contains("filter") && !full.contains("tab")) {
                return node
            }
        }
        for (i in 0 until node.childCount) {
            val match = findFirstVideoNodeRecursive(node.getChild(i))
            if (match != null) return match
        }
        return null
    }

    /**
     * Toggles fullscreen for current video playback.
     */
    fun toggleFullscreen(): Boolean {
        val root = rootInActiveWindow ?: return false
        val fsBtn = findNodeByDesc("full screen")
            ?: findNodeByDesc("enter full screen")
            ?: findNodeByDesc("fullscreen")
            ?: findNode("full screen")
        if (fsBtn != null) {
            return clickNode(fsBtn)
        }
        // If not immediately visible, tap screen to reveal controls then tap fullscreen
        val w = rootInActiveWindow?.let {
            val r = Rect()
            it.getBoundsInScreen(r)
            r.width()
        } ?: 1080
        tapCoordinates(w / 2f, 500f)
        return false
    }

    companion object {
        private const val TAG = "LiaAccessibility"

        var instance: LiaAccessibilityService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _currentPackage = MutableStateFlow("")
        val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()

        /**
         * Checks whether LiaAI's AccessibilityService is enabled in Android Settings.
         */
        fun isEnabledInSettings(context: Context): Boolean {
            val expectedComponentName = ComponentName(context, LiaAccessibilityService::class.java).flattenToString()
            val expectedShortName = "${context.packageName}/${LiaAccessibilityService::class.java.simpleName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = enabledServices.split(":")
            for (service in colonSplitter) {
                if (service.equals(expectedComponentName, ignoreCase = true) ||
                    service.equals(expectedShortName, ignoreCase = true) ||
                    service.contains(context.packageName) && service.contains("LiaAccessibilityService")
                ) {
                    return true
                }
            }
            return false
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
