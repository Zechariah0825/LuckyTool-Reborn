package com.luckyzyx.luckytool.hook.scopes.browser

import android.app.Application
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class)
class BrowserSearchLandingTest {
    private val part = BrowserSearchLandingPolicy.PART
    private val history = BrowserSearchLandingPolicy.Card(4, BrowserSearchLandingPolicy.HISTORY_ITEM, part)

    @Test fun `only original history survives card registry filtering`() {
        val cards = listOf(history,
            BrowserSearchLandingPolicy.Card(6, "rank", part),
            BrowserSearchLandingPolicy.Card(8, "banner", part),
            BrowserSearchLandingPolicy.Card(9, "clipboard", part),
            BrowserSearchLandingPolicy.Card(10, "recommendation", part))
        val removed = BrowserSearchLandingPolicy.removableKeys(cards)!!
        val survivors = cards.filterNot { it.key in removed }
        assertEquals(listOf(6, 8, 9, 10), removed)
        assertSame(history, survivors.single())
    }
    @Test fun `missing or changed history mapping leaves registry untouched`() {
        assertNull(BrowserSearchLandingPolicy.removableKeys(emptyList()))
        assertNull(BrowserSearchLandingPolicy.removableKeys(listOf(history.copy(key = 7))))
        assertNull(BrowserSearchLandingPolicy.removableKeys(listOf(history.copy(className = "unknown"))))
    }
    @Test fun `typed suggestions and unrelated owners fail open`() {
        assertNull(BrowserSearchLandingPolicy.removableKeys(listOf(history.copy(ownerClass =
            "com.heytap.browser.search.suggest.local.part.impl.NativeSugPart"))))
        assertNull(BrowserSearchLandingPolicy.removableKeys(listOf(history, history.copy(key = 8, ownerClass = "other"))))
    }
    @Test fun `repeated initialization keeps existing history without replacements`() {
        assertEquals(emptyList<Int>(), BrowserSearchLandingPolicy.removableKeys(listOf(history)))
        assertEquals(emptyList<Int>(), BrowserSearchLandingPolicy.removableKeys(listOf(history)))
    }

    private fun layout(): BrowserSearchLandingViews.Layout {
        val context = RuntimeEnvironment.getApplication()
        val root = FrameLayout(context)
        val header = LinearLayout(context)
        val history = LinearLayout(context)
        root.addView(header)
        root.addView(View(context)) // Tab selector and ranking body.
        header.addView(View(context)) // Advertisement/recommendation.
        header.addView(history)
        header.addView(View(context)) // Async recommendation placeholder.
        return BrowserSearchLandingViews.Layout(root, header, history)
    }
    @Test fun `history rows delete and outside input controls remain usable`() {
        val layout = layout()
        val context = layout.root.context
        val page = LinearLayout(context)
        val input = TextView(context).apply { text = "Synthetic query" }
        page.addView(input); page.addView(layout.root)
        var cleared = false
        val delete = View(context).apply { setOnClickListener { cleared = true } }
        (layout.history as LinearLayout).addView(delete)
        assertTrue(BrowserSearchLandingViews.clean(layout))
        assertEquals(View.VISIBLE, input.visibility)
        assertEquals(View.VISIBLE, layout.history.visibility)
        assertEquals(View.VISIBLE, delete.visibility)
        assertEquals(View.GONE, layout.root.getChildAt(1).visibility)
        assertEquals(View.GONE, layout.header.getChildAt(0).visibility)
        delete.performClick()
        assertTrue(cleared)
    }
    @Test fun `async layout additions and reentry are hidden without changing history`() {
        val layout = layout()
        BrowserSearchLandingViews.clean(layout)
        layout.header.getChildAt(0).visibility = View.VISIBLE
        val lateAd = View(layout.root.context)
        layout.header.addView(lateAd)
        BrowserSearchLandingViews.clean(layout)
        assertEquals(View.GONE, lateAd.visibility)
        assertEquals(View.GONE, layout.header.getChildAt(0).visibility)
        assertEquals(View.VISIBLE, layout.history.visibility)
    }
    @Test fun `empty history and incognito visibility is not forced visible`() {
        val layout = layout()
        layout.history.visibility = View.GONE
        BrowserSearchLandingViews.clean(layout)
        assertEquals(View.GONE, layout.history.visibility)
        layout.history.visibility = View.INVISIBLE
        BrowserSearchLandingViews.clean(layout)
        assertEquals(View.INVISIBLE, layout.history.visibility)
    }
    @Test fun `changed hierarchy and unrelated roots are not cleaned`() {
        val layout = layout()
        layout.header.removeView(layout.history)
        assertFalse(BrowserSearchLandingViews.clean(layout))
        assertEquals(View.VISIBLE, layout.root.getChildAt(1).visibility)
        assertNull(BrowserSearchLandingViews.resolve(layout.root))
    }
    @Test fun `native incognito registry item survives without preserving similarly keyed ads`() {
        val incognito = BrowserSearchLandingPolicy.Card(8, BrowserSearchLandingPolicy.INCOGNITO_ITEM, part)
        val ad = BrowserSearchLandingPolicy.Card(9, "ad", part)
        assertEquals(listOf(9), BrowserSearchLandingPolicy.removableKeys(listOf(history, incognito, ad)))
        assertEquals(listOf(8), BrowserSearchLandingPolicy.removableKeys(listOf(history, incognito.copy(className = "ad"))))
        assertEquals(listOf(9), BrowserSearchLandingPolicy.removableKeys(listOf(history, incognito.copy(key = 9))))
    }
    @Test fun `dynamic native incognito notices keep native visible and hidden states`() {
        val layout = layout()
        BrowserSearchLandingViews.clean(layout)
        val notice = com.heytap.browser.search.suggest.view.IncognitoView(layout.root.context)
        val head = com.heytap.browser.search.suggest.view.IncognitoViewHead(layout.root.context)
        layout.root.addView(notice, 0)
        layout.header.addView(head)
        listOf(View.VISIBLE, View.GONE, View.INVISIBLE).forEach { nativeVisibility ->
            notice.visibility = nativeVisibility
            head.visibility = nativeVisibility
            BrowserSearchLandingViews.clean(layout)
            assertEquals(nativeVisibility, notice.visibility)
            assertEquals(nativeVisibility, head.visibility)
            assertEquals(View.VISIBLE, layout.history.visibility)
        }
    }
    @Test fun `incognito exemptions are exact classes at verified parent placements only`() {
        val layout = layout()
        val wrongRoot = com.heytap.browser.search.suggest.view.IncognitoViewHead(layout.root.context)
        val wrongHeader = com.heytap.browser.search.suggest.view.IncognitoView(layout.root.context)
        val unknownPlacement = com.heytap.browser.search.suggest.view.IncognitoViewSlogan(layout.root.context)
        layout.root.addView(wrongRoot)
        layout.header.addView(wrongHeader)
        layout.header.addView(unknownPlacement)
        BrowserSearchLandingViews.clean(layout)
        assertEquals(View.GONE, wrongRoot.visibility)
        assertEquals(View.GONE, wrongHeader.visibility)
        assertEquals(View.GONE, unknownPlacement.visibility)
    }

}
