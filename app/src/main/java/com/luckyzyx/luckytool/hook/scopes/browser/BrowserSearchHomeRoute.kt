package com.luckyzyx.luckytool.hook.scopes.browser

import java.lang.ref.WeakReference

/** A synchronous native Home -> DirectHomePage transaction, not a web URL/history rewrite. */
internal class BrowserSearchHomeRoute {
    enum class Phase { RETURNING, FAILED, SEARCH }
    private class Entry(tab: Any, home: Any, var phase: Phase) {
        val tab = WeakReference(tab)
        val home = WeakReference(home)
    }
    private val entries = mutableListOf<Entry>()
    private fun entry(tab: Any, home: Any): Entry? {
        entries.removeAll { it.tab.get() == null || it.home.get() == null }
        return entries.firstOrNull { it.tab.get() === tab && it.home.get() === home }
    }
    fun phase(tab: Any, home: Any): Phase? = entry(tab, home)?.phase
    fun cancel(home: Any) { entries.removeAll { it.home.get() == null || it.home.get() === home } }

    /** True means this child transition is not permission to auto-open a URL.
     * The transaction's temporary Major selection must not cancel itself. After
     * completion, a real departure relinquishes our special Search Back behavior.
     */
    fun childChanged(tab: Any, home: Any, search: Boolean, major: Boolean): Boolean {
        val phase = phase(tab, home) ?: return false
        if ((phase == Phase.SEARCH && !search) || (phase == Phase.FAILED && !major)) cancel(home)
        return true
    }

    /** The verified non-animated pager and child selector dispatch synchronously.
     * Never leave a queued selector that could mutate a newly active tab later.
     * A failed attempt remains retryable, not a URL reload or exit request.
     */
    fun restore(
        tab: Any, home: Any,
        returnHome: () -> Unit,
        isActiveHome: () -> Boolean,
        selectSearch: () -> Boolean,
    ): Boolean {
        if (phase(tab, home) == Phase.RETURNING) return false
        cancel(home)
        val request = Entry(tab, home, Phase.RETURNING).also(entries::add)
        return try {
            returnHome()
            val success = entry(tab, home) === request && isActiveHome() && selectSearch() && isActiveHome() && entry(tab, home) === request
            if (entry(tab, home) === request) request.phase = if (success) Phase.SEARCH else Phase.FAILED
            success
        } catch (error: Exception) {
            if (entry(tab, home) === request) request.phase = Phase.FAILED
            throw error
        }
    }
}
