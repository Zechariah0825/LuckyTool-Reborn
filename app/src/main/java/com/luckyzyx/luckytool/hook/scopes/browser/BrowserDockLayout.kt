package com.luckyzyx.luckytool.hook.scopes.browser

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/** Applied before native measurement. Preserve safe-area margins and hover position. */
internal object BrowserDockLayout {
    fun expand(dock: View, buttons: LinearLayout) {
        if (buttons.orientation != LinearLayout.HORIZONTAL) return
        val visible = (0 until buttons.childCount).map(buttons::getChildAt)
            .filter { it.visibility != View.GONE }
        if (visible.isEmpty() || visible.any { it.layoutParams !is LinearLayout.LayoutParams }) return
        dock.layoutParams?.let { params ->
            // Native onMeasure is already in progress; mutate without requesting
            // another layout on each pass (native d restores its ideal width).
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        buttons.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        buttons.weightSum = visible.size.toFloat()
        visible.forEach { child ->
            (child.layoutParams as LinearLayout.LayoutParams).apply {
                width = 0
                weight = 1f
                leftMargin = 0
                rightMargin = 0
                marginStart = 0
                marginEnd = 0
            }
        }
    }
}
