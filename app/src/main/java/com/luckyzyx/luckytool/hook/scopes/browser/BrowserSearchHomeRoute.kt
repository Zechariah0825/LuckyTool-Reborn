package com.luckyzyx.luckytool.hook.scopes.browser

/** Only explicit Home from the verified Search+ page takes the history-preserving route. */
internal object BrowserSearchHomeRoute {
    fun shouldPreserve(pageClass: String?, active: Boolean, explicitHome: Boolean): Boolean =
        active && explicitHome && pageClass == "com.heytap.browser.ai.page.multi.MultiSearchPage"
}
