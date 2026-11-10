package com.luckyzyx.luckytool.hook.scopes.browser

import android.app.Application
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class)
class BrowserHomeContentTest {
    @Test fun `history return restores native content not an empty launcher page`() {
        val context = RuntimeEnvironment.getApplication()
        val root = FrameLayout(context)
        val native = View(context)
        val initiallyGone = View(context).apply { visibility = View.GONE }
        val initiallyInvisible = View(context).apply { visibility = View.INVISIBLE }
        val replacement = FrameLayout(context)
        listOf(native, initiallyGone, initiallyInvisible, replacement).forEach(root::addView)
        val content = BrowserHomeContent()
        repeat(3) {
            content.show(root, replacement, false)
            assertEquals(View.INVISIBLE, native.visibility)
            assertEquals(View.VISIBLE, replacement.visibility)
            content.show(root, replacement, true)
            assertEquals(View.VISIBLE, native.visibility)
            assertEquals(View.GONE, replacement.visibility)
            assertEquals(View.GONE, initiallyGone.visibility)
            assertEquals(View.INVISIBLE, initiallyInvisible.visibility)
            assertEquals(4, root.childCount)
        }
    }

    @Test fun `restoration does not override later native removal or gone decision`() {
        val context = RuntimeEnvironment.getApplication()
        val root = FrameLayout(context)
        val gone = View(context)
        val removed = View(context)
        val replacement = View(context)
        listOf(gone, removed, replacement).forEach(root::addView)
        val content = BrowserHomeContent()
        content.show(root, replacement, false)
        gone.visibility = View.GONE
        root.removeView(removed)
        content.show(root, replacement, true)
        assertEquals(View.GONE, gone.visibility)
        assertNull(removed.parent)
        assertEquals(View.INVISIBLE, removed.visibility)
    }
}
