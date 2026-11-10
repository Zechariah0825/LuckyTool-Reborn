package com.luckyzyx.luckytool.hook.scopes.browser

import java.lang.reflect.Modifier

/** A version whitelist, not a best-effort search for similarly shaped methods. */
internal data class BrowserHomeNativeRoutes(
    val returnHome: String,
    val pageChanged: String,
) {
    fun matches(tab: Class<*>, page: Class<*>): Boolean =
        isInstanceVoid(tab, returnHome, Boolean::class.javaPrimitiveType!!) &&
            isInstanceVoid(tab, pageChanged, page, page)

    private fun isInstanceVoid(owner: Class<*>, name: String, vararg parameters: Class<*>): Boolean =
        runCatching {
            owner.getDeclaredMethod(name, *parameters).let {
                it.returnType == Void.TYPE && !Modifier.isStatic(it.modifiers)
            }
        }.getOrDefault(false)

    companion object {
        fun select(versionName: String?, versionCode: Long, fragmentCreate: String): BrowserHomeNativeRoutes? =
            when {
                versionName == "40.10.21.9.6" && versionCode == 10210906L && fragmentCreate == "xh" ->
                    BrowserHomeNativeRoutes("g1", "g0")
                versionName == "40.10.25.2" && versionCode == 10250200L && fragmentCreate == "yh" ->
                    BrowserHomeNativeRoutes("h1", "h0")
                else -> null
            }
    }
}
