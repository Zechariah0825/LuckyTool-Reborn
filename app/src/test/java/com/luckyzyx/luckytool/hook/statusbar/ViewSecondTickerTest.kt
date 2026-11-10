package com.luckyzyx.luckytool.hook.statusbar

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.os.SystemClock
import android.widget.FrameLayout
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ViewSecondTickerTest {
    @Test
    fun `ticker waits for attachment stops on detach and resumes without duplication`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            val parent = FrameLayout(activity)
            activity.setContentView(parent)
            val view = TextView(activity)
            var count = 0
            // The paused looper advances virtual uptime, not the host JVM wall
            // clock. Advance both together so second-boundary scheduling is real.
            val ticker = ViewSecondTicker(view, wallClockMillis = { SystemClock.uptimeMillis() }) {
                assertEquals(Looper.getMainLooper(), Looper.myLooper())
                count++
            }
            ticker.start()
            ticker.start()
            val looper = shadowOf(Looper.getMainLooper())
            looper.idleFor(Duration.ofSeconds(2))
            assertEquals(0, count)
            parent.addView(view)
            looper.idleFor(Duration.ofSeconds(2))
            assertEquals(2, count)
            parent.removeView(view)
            looper.idleFor(Duration.ofSeconds(2))
            assertEquals(2, count)
            parent.addView(view)
            looper.idleFor(Duration.ofSeconds(2))
            assertEquals(4, count)
            assertTrue(view.isAttachedToWindow)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
