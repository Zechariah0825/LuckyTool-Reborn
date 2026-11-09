package com.luckyzyx.luckytool.hook.scopes.browser

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

/**
 * Targeted to the five-item bottom navigation of OPPO Browser 40.10.25.2.
 * Filter the model BEFORE it is rendered (not just the TextView) so positions reflow.
 * Unknown/changed list schemas pass through unchanged instead of corrupting navigation.
 */
@Obfuscate
class BrowserBottomTabs : YukiBaseHooker() {
    override fun onHook() {
        val classesAndMethods = listOf(
            "com.heytap.browser.trait.tab.BottomTabDataDefault" to listOf("e", "f", "g"),
            "com.heytap.browser.trait.tab.BottomTabManager" to listOf("l"),
        )
        classesAndMethods.forEach { (name, methods) ->
            val klass = name.toClassOrNull() ?: return@forEach
            methods.forEach { method ->
                try {
                    klass.resolve().firstMethod { this.name = method }.hook {
                        after {
                            val original = result as? List<*> ?: return@after
                            val validTypes = setOf(
                                "com.heytap.browser.trait.tab.d",
                                "com.heytap.browser.tab_bottom.BottomTabModel",
                            )
                            val filtered = RebornBrowserConfig.filterFiveBottomTabs(original) {
                                it != null && it.javaClass.name in validTypes
                            } ?: return@after
                            result = ArrayList(filtered)
                        }
                    }
                } catch (error: Throwable) {
                    YLog.error("Reborn: bottom tab schema mismatch at $name.$method", error)
                }
            }
        }
    }
}
