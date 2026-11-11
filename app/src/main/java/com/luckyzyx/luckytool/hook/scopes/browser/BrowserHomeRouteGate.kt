package com.luckyzyx.luckytool.hook.scopes.browser

import java.util.WeakHashMap

/** Main-thread activation state. Back does not authorize a new Home request. */
internal class BrowserHomeRouteGate {
    private class State(
        var generation: Long = 0, var opened: Boolean = false,
        var leftHome: Boolean = false, var recovery: Boolean = false,
        var openedAt: Long = -1,
    )
    private val states = WeakHashMap<Any, State>()
    private fun state(fragment: Any) = states.getOrPut(fragment) { State() }

    fun ticket(fragment: Any): Long = state(fragment).generation

    /** Cancel queued work without authorizing a second redirect when Back returns. */
    fun invalidate(fragment: Any) {
        state(fragment).apply {
            generation++
            // A deferred native permission result is no longer allowed to take
            // over this Home. Returning to the tab must expose retry, not a spinner.
            if (opened && !leftHome) recovery = true
        }
    }

    /** Explicit Home or a real non-home -> home subpage transition. */
    fun rearm(fragment: Any) {
        state(fragment).apply { generation++; opened = false; openedAt = -1; leftHome = false; recovery = false }
    }

    /** Repeated taps while one native dispatch is pending must not push duplicate pages. */
    fun requestHome(fragment: Any) {
        val current = state(fragment)
        if (current.opened && current.openedAt == current.generation &&
            !current.leftHome && !current.recovery) return
        rearm(fragment)
    }

    fun leftHome(fragment: Any) {
        states[fragment]?.let { if (it.opened) it.leftHome = true }
    }

    fun returnedHome(fragment: Any) {
        states[fragment]?.let { if (it.opened && it.leftHome) it.recovery = true }
    }

    fun showRecovery(fragment: Any): Boolean = states[fragment]?.recovery == true

    fun failedDispatch(fragment: Any, ticket: Long): Boolean {
        val current = state(fragment)
        if (current.generation != ticket) return false
        current.opened = true
        current.openedAt = ticket
        current.recovery = true
        return true
    }

    fun ownsDispatch(fragment: Any, ticket: Long): Boolean = states[fragment]?.let {
        it.generation == ticket && it.openedAt == ticket && it.opened && !it.leftHome
    } == true

    fun needsRecovery(fragment: Any, ticket: Long): Boolean =
        ownsDispatch(fragment, ticket) && states[fragment]?.recovery != true

    /** A native permission flow may accept loadUrl without ever changing page. */
    fun recoverUndispatched(fragment: Any, ticket: Long): Boolean {
        if (!needsRecovery(fragment, ticket)) return false
        state(fragment).recovery = true
        return true
    }

    fun openOnce(fragment: Any, load: () -> Boolean): Boolean =
        openOnce(fragment, ticket(fragment), load)

    fun openOnce(fragment: Any, ticket: Long, load: () -> Boolean): Boolean {
        val state = state(fragment)
        if (state.generation != ticket || state.opened) return false
        state.opened = true
        state.openedAt = ticket
        return try {
            load().also { if (!it && state.generation == ticket) state.opened = false }
        } catch (error: Exception) {
            if (state.generation == ticket) state.opened = false
            throw error
        }
    }
}
