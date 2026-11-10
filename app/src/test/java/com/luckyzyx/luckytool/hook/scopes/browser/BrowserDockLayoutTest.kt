package com.luckyzyx.luckytool.hook.scopes.browser

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class)
class BrowserDockLayoutTest {
    private fun verify(count: Int, hidden: Set<Int> = emptySet(), width: Int = 1000) {
        val context = RuntimeEnvironment.getApplication()
        val root = FrameLayout(context)
        val dock = FrameLayout(context)
        val params = FrameLayout.LayoutParams(300, 80).apply {
            leftMargin = 20; rightMargin = 20; bottomMargin = 30
        }
        root.addView(dock, params)
        val buttons = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 5f }
        dock.addView(buttons, FrameLayout.LayoutParams(-1, -1))
        repeat(count) { index ->
            buttons.addView(View(context).apply { if (index in hidden) visibility = View.GONE },
                LinearLayout.LayoutParams(80, -1).apply { weight = .3f; leftMargin = 3; rightMargin = 7 })
        }
        repeat(2) {
            // Browser applies its ideal width each pass, including after rotation.
            params.width = 300
            BrowserDockLayout.expand(dock, buttons)
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, width, 200)
            assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.width)
            assertEquals(width - 40, dock.measuredWidth)
            assertEquals(30, params.bottomMargin)
            val visible = (0 until count).filter { it !in hidden }.map(buttons::getChildAt)
            assertEquals(visible.size.toFloat(), buttons.weightSum, 0f)
            assertEquals(width - 40, visible.sumOf { it.measuredWidth })
            assertTrue(visible.maxOf { it.measuredWidth } - visible.minOf { it.measuredWidth } <= 1)
        }
    }

    @Test fun `three model filtered items fill dock equally`() = verify(3)
    @Test fun `visual fallback with two gone children divides only survivors`() = verify(5, setOf(1, 3))
    @Test fun `five items also fill available width`() = verify(5)
    @Test fun `rotation uses current parent width not display metrics`() = verify(3, width = 1601)
}
