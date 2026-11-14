package com.luckyzyx.luckytool.hook.scopes.browser

import java.lang.ref.WeakReference

/** Ownership comes from the module's exact native LoadParams, never a matching URL. */
internal class BrowserHomeBoundary {
    class Request(tab: Any, home: Any, val ticket: Long) {
        val tab = WeakReference(tab)
        val home = WeakReference(home)
    }

    // WeakHashMap uses equals(), which is insufficient for request ownership.
    private val requests = mutableListOf<Pair<WeakReference<Any>, Request>>()
    private val roots = mutableListOf<Pair<WeakReference<Any>, WeakReference<Any>>>()

    fun track(params: Any, request: Request) {
        requests.removeAll { it.first.get() == null || it.first.get() === params }
        requests += WeakReference(params) to request
    }
    fun request(params: Any): Request? {
        requests.removeAll { it.first.get() == null }
        return requests.firstOrNull { it.first.get() === params }?.second
    }

    fun capture(request: Request, tab: Any, home: Any, page: Any, pageIndex: Int): Boolean {
        if (request.tab.get() !== tab || request.home.get() !== home || page === home || pageIndex != 1) return false
        roots.removeAll { it.first.get() == null || it.second.get() == null || it.first.get() === home }
        roots += WeakReference(home) to WeakReference(page)
        return true
    }

    fun forget(home: Any) {
        roots.removeAll { it.first.get() == null || it.first.get() === home }
    }

    fun isRoot(home: Any, page: Any, pageIndex: Int, active: Boolean): Boolean =
        active && pageIndex == 1 && roots.any { it.first.get() === home && it.second.get() === page }
}
