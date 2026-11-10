package com.luckyzyx.luckytool.hook.scopes.browser

import android.view.View
import android.view.ViewGroup
import java.util.WeakHashMap

/** Keep native Home usable when history returns to its permanent anchor. */
internal class BrowserHomeContent {
    private val hidden = WeakHashMap<View, Boolean>()

    fun show(root: ViewGroup, replacement: View, native: Boolean) {
        if (native) {
            replacement.visibility = View.GONE
            hidden.keys.toList().forEach { child ->
                // Do not undo a later native GONE decision or restore detached views.
                if (child.parent === root && child.visibility == View.INVISIBLE) child.visibility = View.VISIBLE
            }
            hidden.clear()
        } else {
            replacement.visibility = View.VISIBLE
            for (index in 0 until root.childCount) {
                val child = root.getChildAt(index)
                if (child !== replacement && child.visibility == View.VISIBLE) {
                    hidden[child] = true
                    child.visibility = View.INVISIBLE
                }
            }
            if (root.childCount > 0 && root.getChildAt(root.childCount - 1) !== replacement) replacement.bringToFront()
        }
    }
}
