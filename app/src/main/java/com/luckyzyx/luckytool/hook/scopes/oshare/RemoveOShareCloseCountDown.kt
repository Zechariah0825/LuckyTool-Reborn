package com.luckyzyx.luckytool.hook.scopes.oshare

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveOShareCloseCountDown(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("remove_oshare_close_countdown", true)
        feature.setup {
            val osCode = getOSVersionCode

            if (osCode >= 27) loadHooker(HookOShareFeatureConfig(dexKitBridge))
            loadHooker(HookOShareSpUtils(dexKitBridge))
        }
    }

    @Obfuscate
    class HookOShareFeatureConfig(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            // Loaded only when this exact switch is enabled.
            val feature = ObservedFeature("remove_oshare_close_countdown", true)
            feature.setup {
                //Source OShareFeatureConfig
                dexKitBridge.findClass {
                    matcher {
                        usingStrings("OShareFeatureConfig")
                    }
                }.apply {
                    checkDataList("OShareFeatureConfig")
                    findMethod {
                        matcher {
                        paramTypes(classOf<Context>())
                            returnType(classOf<Long>())
                            usingStrings("getSwitchTimeOut")
                        }
                    }.apply {
                        checkDataList("OShareFeatureConfig getSwitchTimeOut")
                        single().className.toClass().resolve().apply {
                            firstMethod {
                                name = single().name
                                parameters(Context::class)
                                returnType = Long::class
                            }.hook {
                                interceptObserved(feature, 0L)
                            }
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    class HookOShareSpUtils(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            // Loaded only when this exact switch is enabled.
            val feature = ObservedFeature("remove_oshare_close_countdown", true)
            feature.setup {
                //Source SpUtils
                dexKitBridge.findClass {
                    matcher {
                        usingStrings("SpUtils", "share_config")
                    }
                }.apply {
                    checkDataList("SpUtils")
                    findMethod {
                        matcher {
                        paramTypes(classOf<Context>(), classOf<Long>())
                            usingStrings("updateLastTurnOnTime", "key_last_turn_on_time")
                        }
                    }.apply {
                        checkDataList("SpUtils updateLastTurnOnTime")
                        single().className.toClass().resolve().apply {
                            firstMethod {
                                parameters(Context::class, Long::class)
                            }.hook {
                                beforeObserved(feature) {
                                    lastArg().set(0L)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
