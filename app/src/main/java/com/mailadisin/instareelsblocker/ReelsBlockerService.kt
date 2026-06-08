package com.mailadisin.instareelsblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class ReelsBlockerService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onServiceConnected() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName != INSTAGRAM_PACKAGE) {
            removeOverlay()
            return
        }

        if (!isBlockerEnabled()) {
            removeOverlay()
            return
        }

        val root = rootInActiveWindow ?: return
        try {
            updateOverlay(root)
        } finally {
            root.recycle()
        }
    }

    private fun updateOverlay(root: AccessibilityNodeInfo) {
        val reelsNode = findReelsNode(root)
        if (reelsNode == null) {
            removeOverlay()
            return
        }
        try {
            val bounds = Rect()
            reelsNode.getBoundsInScreen(bounds)
            if (bounds.isEmpty) {
                removeOverlay()
                return
            }
            if (overlayView == null) addOverlay(bounds) else moveOverlay(bounds)
        } finally {
            reelsNode.recycle()
        }
    }

    private fun addOverlay(bounds: Rect) {
        val view = View(this).apply {
            setBackgroundColor(Color.BLACK)
            setOnClickListener {
                Toast.makeText(
                    this@ReelsBlockerService,
                    "Reels is blocked",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        val params = WindowManager.LayoutParams(
            bounds.width(),
            bounds.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = bounds.left
            y = bounds.top
        }
        windowManager?.addView(view, params)
        overlayView = view
    }

    private fun moveOverlay(bounds: Rect) {
        val view = overlayView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        params.x = bounds.left
        params.y = bounds.top
        params.width = bounds.width()
        params.height = bounds.height()
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: IllegalArgumentException) {
            overlayView = null
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: IllegalArgumentException) {}
        }
        overlayView = null
    }

    // Returns an obtained (caller must recycle) node for the Reels tab, or null.
    private fun findReelsNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        if ((desc.contains("reel") || text.contains("reel")) && node.isClickable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findReelsNode(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun isBlockerEnabled() = prefs.getBoolean(KEY_ENABLED, true)

    override fun onInterrupt() = removeOverlay()

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        const val PREFS_NAME = "reels_blocker_prefs"
        const val KEY_ENABLED = "blocker_enabled"
    }
}
