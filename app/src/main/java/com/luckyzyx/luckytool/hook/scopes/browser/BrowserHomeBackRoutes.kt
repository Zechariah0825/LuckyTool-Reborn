package com.luckyzyx.luckytool.hook.scopes.browser

import java.lang.reflect.Modifier

/** New Back/load mappings require fresh DEX evidence, independently of v0.7 routing. */
internal data class BrowserHomeBackRoutes(
    val permissionLoad: String,
    val dispatchLoad: String,
    val showPage: String,
    val backwardPage: String,
) {
    fun matches(tab: Class<*>, page: Class<*>, params: Class<*>, controller: Class<*>): Boolean {
        val boolean = Boolean::class.javaPrimitiveType!!
        val int = Int::class.javaPrimitiveType!!
        return method(tab, permissionLoad, Void.TYPE, params, boolean) &&
            method(tab, dispatchLoad, Void.TYPE, params, boolean, requireFinal = true) &&
            method(tab, showPage, Void.TYPE, page, boolean) &&
            method(tab, backwardPage, boolean, boolean, boolean, requireFinal = true) &&
            method(controller, "E", int) && method(controller, "B", int, page)
    }

    private fun method(
        owner: Class<*>, name: String, result: Class<*>, vararg parameters: Class<*>,
        requireFinal: Boolean = false,
    ): Boolean = runCatching {
        owner.getDeclaredMethod(name, *parameters).let {
            it.returnType == result && !Modifier.isStatic(it.modifiers) &&
                (!requireFinal || Modifier.isFinal(it.modifiers))
        }
    }.getOrDefault(false)

    companion object {
        fun select(versionName: String?, code: Long, create: String): BrowserHomeBackRoutes? = when {
            versionName == "40.10.21.9.6" && code == 10210906L && create == "xh" ->
                BrowserHomeBackRoutes("x9", "P", "C8", "M")
            versionName == "40.10.25.2" && code == 10250200L && create == "yh" ->
                BrowserHomeBackRoutes("s9", "Q", "A8", "N")
            else -> null
        }
    }
}
