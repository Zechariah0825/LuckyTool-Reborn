package com.luckyzyx.luckytool.hook.scopes.browser

import java.lang.reflect.Modifier

/** Verified Dock DirectHomePage (type 7), not the separate MultiSearchPage tab. */
internal data class BrowserHomeSearchRoutes(
    val selectSearch: String,
    val isSearch: String,
    val backToMajor: String,
    val directPageClass: String,
) {
    fun matches(home: Class<*>, direct: Class<*>): Boolean =
        direct.name == directPageClass &&
            instanceBoolean(home, selectSearch, Boolean::class.javaPrimitiveType!!) &&
            instanceBoolean(home, isSearch) && instanceBoolean(home, backToMajor) && instanceBoolean(home, "onBackPressed") &&
            runCatching { direct.getDeclaredMethod("getType").returnType == Int::class.javaPrimitiveType }.getOrDefault(false)

    private fun instanceBoolean(owner: Class<*>, name: String, vararg params: Class<*>): Boolean =
        runCatching { owner.getDeclaredMethod(name, *params).let {
            it.returnType == Boolean::class.javaPrimitiveType && !Modifier.isStatic(it.modifiers)
        } }.getOrDefault(false)

    companion object {
        fun select(versionName: String?, versionCode: Long, create: String): BrowserHomeSearchRoutes? = when {
            versionName == "40.10.21.9.6" && versionCode == 10210906L && create == "xh" ->
                BrowserHomeSearchRoutes("t2", "x1", "U1", "uq1.c")
            versionName == "40.10.25.2" && versionCode == 10250200L && create == "yh" ->
                BrowserHomeSearchRoutes("u2", "y1", "U1", "lv1.c")
            else -> null
        }
    }
}
