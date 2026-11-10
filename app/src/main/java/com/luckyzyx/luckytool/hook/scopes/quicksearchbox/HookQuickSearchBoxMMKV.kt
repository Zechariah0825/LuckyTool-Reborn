package com.luckyzyx.luckytool.hook.scopes.quicksearchbox

import android.util.ArrayMap
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookQuickSearchBoxMMKV(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val enabled = preferences(ModulePrefs).getBoolean("remove_searchbox_uninstalled_app_suggestions", false)
        val feature = ObservedFeature("remove_searchbox_uninstalled_app_suggestions", enabled)
        val map = ArrayMap<String, Any>().apply {
            if (enabled) {
                put("new_suggest_app_card", false)
            }
        }
        loadHooker(HookMMKVManager(dexKitBridge, map, feature))
    }

    @Obfuscate
    class HookMMKVManager(val dexKitBridge: DexKitBridge, val map: ArrayMap<String, Any>, val feature: ObservedFeature)  : YukiBaseHooker() {
        override fun onHook() {
            feature.setup {
                //Source MMKVManager
                dexKitBridge.findClass {
                    matcher {
                        className("com.heytap.quicksearchbox.common.manager.MMKVManager")
                    }
                }.apply {
                    checkDataList("HookMMKV find clazz")
                    findMethod {
                        matcher {
                        paramTypes(classOf<String>(), classOf<String>())
                            returnType(classOf<String>())
                            usingStrings("getString")
                        }
                    }.apply {
                        checkDataList("HookMMKV find getString")
                        single().className.toClass().resolve().apply {
                            firstMethod {
                                name = single().methodName
                                parameters(String::class, String::class)
                                returnType = String::class
                            }.hook {
                                before {
                                    val key = firstArg().get<String>()
                                    if (key.isNullOrBlank()) return@before
                                    if (key != "new_suggest_app_card" || !map.containsKey(key)) return@before
                                    feature.callback {
                                    when (val value = map[key]) {
                                        null -> return@callback
                                        is Boolean -> result = value.toString()
                                        is String -> result = value
                                        is Int -> result = value
                                    }
                                    }
                                }
                            }
                        }
                    }
                    findMethod {
                        matcher {
                        paramTypes(classOf<String>(), classOf<Boolean>())
                            returnType(classOf<Boolean>())
                            usingStrings("getBoolean")
                        }
                    }.apply {
                        checkDataList("HookMMKV find getBoolean")
                        single().className.toClass().resolve().apply {
                            firstMethod {
                                name = single().methodName
                                parameters(String::class, Boolean::class)
                                returnType = Boolean::class
                            }.hook {
                                before {
                                    val key = firstArg().get<String>()
                                    if (key.isNullOrBlank()) return@before
                                    if (key != "new_suggest_app_card" || !map.containsKey(key)) return@before
                                    feature.callback {
                                    when (val value = map[key]) {
                                        null -> return@callback
                                        "1" -> result = true
                                        "0" -> result = false
                                        "true" -> result = true
                                        "false" -> result = false
                                        is Boolean -> result = value
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
