package com.luckyzyx.luckytool.hook.statusbar

import android.os.Handler
import android.os.Looper
import android.view.View

/** One main-thread ticker per view; detach cancels queued work, reattach resumes it. */
internal class ViewSecondTicker(
    private val view: View,
    private val wallClockMillis: () -> Long = System::currentTimeMillis,
    private val tick: () -> Unit,
) : View.OnAttachStateChangeListener, Runnable {
    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    fun start() {
        if (started) return
        started = true
        view.addOnAttachStateChangeListener(this)
        if (view.isAttachedToWindow) schedule()
    }

    override fun onViewAttachedToWindow(v: View) = schedule()

    override fun onViewDetachedFromWindow(v: View) {
        handler.removeCallbacks(this)
    }

    override fun run() {
        if (!view.isAttachedToWindow) return
        tick()
        if (view.isAttachedToWindow) schedule()
    }

    private fun schedule() {
        handler.removeCallbacks(this)
        handler.postDelayed(this, 1000L - Math.floorMod(wallClockMillis(), 1000L))
    }
}
