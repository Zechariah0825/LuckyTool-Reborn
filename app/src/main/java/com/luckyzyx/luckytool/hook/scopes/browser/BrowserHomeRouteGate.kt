package com.luckyzyx.luckytool.hook.scopes.browser

import java.util.WeakHashMap

/** Main-thread home activation guard. Do not redirect again when Back returns home. */
internal class BrowserHomeRouteGate {
    private val opened = WeakHashMap<Any, Boolean>()

    /** Only a verified explicit Home click may rearm this particular tab. */
    fun rearm(fragment: Any) {
        opened.remove(fragment)
    }

    fun openOnce(fragment: Any, load: () -> Boolean): Boolean {
        if (opened.containsKey(fragment)) return false
        opened[fragment] = true
        return try {
            load().also { if (!it) opened.remove(fragment) }
        } catch (error: Exception) {
            opened.remove(fragment)
            throw error
        }
    }
}
