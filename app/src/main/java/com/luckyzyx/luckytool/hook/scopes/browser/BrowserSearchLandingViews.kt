package com.luckyzyx.luckytool.hook.scopes.browser

import android.view.View
import android.view.ViewGroup

internal object BrowserSearchLandingViews {
    data class Layout(val root: ViewGroup, val header: ViewGroup, val history: View)

    fun resolve(root: View): Layout? {
        if (root.javaClass.name != BrowserSearchLandingPolicy.ROOT || root !is ViewGroup) return null
        fun id(name: String) = root.resources.getIdentifier(name, "id", "com.heytap.browser")
        val headerId = id("ll_header")
        val historyId = id("card_history")
        val bodyId = id("browser_search_main_layout")
        if (headerId == 0 || historyId == 0 || bodyId == 0) return null
        val header = root.findViewById<View>(headerId) as? ViewGroup ?: return null
        val history = root.findViewById<View>(historyId) ?: return null
        val body = root.findViewById<View>(bodyId) ?: return null
        if (header.javaClass.name != BrowserSearchLandingPolicy.HEADER ||
            header.parent !== root || history.parent !== header || body.parent !== root) return null
        return Layout(root, header, history)
    }

    /** Only direct siblings in the verified landing layout. Never touch history descendants. */
    fun clean(layout: Layout): Boolean {
        val (root, header, history) = layout
        if (header.parent !== root || history.parent !== header) return false
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child !== header && child.javaClass.name != BrowserSearchLandingPolicy.INCOGNITO_VIEW &&
                child.visibility != View.GONE) child.visibility = View.GONE
        }
        for (i in 0 until header.childCount) {
            val child = header.getChildAt(i)
            if (child !== history && child.javaClass.name != BrowserSearchLandingPolicy.INCOGNITO_HEAD &&
                child.visibility != View.GONE) child.visibility = View.GONE
        }
        // Keep native history and exact incognito notice visibility: no rows/no-history or incognito stays native.
        return true
    }
}
