package com.luckyzyx.luckytool.hook.scopes.browser

import java.security.MessageDigest

/** A module selection is an apply-once request, never a permanent native-default override. */
object BrowserDefaultEnginePolicy {
    const val PREFS = "luckytool_reborn_search"
    const val APPLIED_REQUEST = "applied_default_request"

    fun requestToken(engine: RebornBrowserConfig.SearchEngine?, revision: String): String? = engine?.let {
        MessageDigest.getInstance("SHA-256")
            .digest("${it.id}\n${it.urlTemplate}\n${revision.take(128)}".toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    fun shouldApply(request: String?, applied: String?): Boolean = request != null && request != applied
}
