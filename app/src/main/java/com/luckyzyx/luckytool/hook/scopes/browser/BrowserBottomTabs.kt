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
    private var reportedDockError = false
    override fun onHook() {
        hookDockWidth()
        val classesAndMethods = listOf(
            "com.heytap.browser.trait.tab.BottomTabDataDefault" to listOf("e", "f", "g"),
            "com.heytap.browser.trait.tab.BottomTabManager" to listOf("l"),
        )
        classesAndMethods.forEach { (name, methods) ->
            val klass = name.toClassOrNull() ?: return@forEach
            methods.forEach { method ->
                try {
                    klass.resolve().firstMethod { this.name = method; parameters() }.hook {
                        after {
                            val original = result as? List<*> ?: return@after
                            val filtered = BrowserBottomTabRules.filter(original) { item ->
                                when {
                                    // Default lists are Integers, not BottomTabModel objects.
                                    item is Int -> item
                                    item?.javaClass?.name == "com.heytap.browser.trait.tab.d" -> {
                                        try {
                                            // The supplied builds have different obfuscated getters.
                                            val old = item.javaClass.methods.any {
                                                it.name == "k" && it.parameterCount == 0 && it.returnType == Int::class.javaPrimitiveType
                                            }
                                            val native = item.javaClass.getMethod(if (old) "m" else "l").invoke(item) == 0
                                            if (native) item.javaClass.getMethod(if (old) "k" else "j").invoke(item) as? Int else null
                                        } catch (_: ReflectiveOperationException) { null }
                                    }
                                    else -> null
                                }
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

    private fun hookDockWidth() {
        try {
            val toolbar = "com.heytap.browser.root.ToolBar".toClassOrNull()
                ?: error("ToolBar unavailable")
            // Both supplied APKs call d(width) before FrameLayout.onMeasure.
            // Native d uses itemCount * idealItemWidth, which shrinks 3 tabs.
            toolbar.resolve().firstMethod {
                name = "d"; parameters(Int::class); returnType = Void.TYPE
            }.hook {
                after {
                    try {
                        val dock = toolbar.getMethod("getUiAdapterContainer").invoke(instance) as? android.view.View
                            ?: return@after
                        val buttons = toolbar.getMethod("getNewsLayout").invoke(instance) as? android.widget.LinearLayout
                            ?: return@after
                        BrowserDockLayout.expand(dock, buttons)
                    } catch (error: Exception) {
                        if (!reportedDockError) {
                            reportedDockError = true
                            YLog.error("Reborn: browser dock layout unavailable", error)
                        }
                    }
                }
            }
            YLog.info("Reborn: full-width browser dock hook mapping configured (ToolBar.d)")
        } catch (error: Exception) {
            YLog.error("Reborn: browser dock width signature unavailable", error)
        }
    }

}
