package com.luckyzyx.luckytool.hook.scopes.launcher

import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveLauncherCardName : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_launcher_card_name", true)
        feature.setup {
            val osCode = getOSVersionCode
            if (osCode >= 30) loadHooker(LauncherCardName)
            else loadHooker(LauncherCardNameV13)
        }
    }

    @Obfuscate
    object LauncherCardName : YukiBaseHooker() {
        override fun onHook() {
            val feature = ObservedFeature("remove_launcher_card_name", true)
            feature.setup {
                //Source CardNameHelper
                "com.android.launcher3.card.utils.CardNameHelper".toClass().resolve().apply {
                    firstMethod { name = "initCardName" }.hook {
                        afterObserved(feature) {
                            val cardName = firstArg().get<View>() ?: return@afterObserved
                            cardName.isVisible = false
                            cardName.asResolver().firstMethod {
                                name = "setTextVisibility"; parameters(Boolean::class)
                            }.invoke(false)

                            val card = firstField { name = "card" }.of(instance).get<View>()
                                ?: return@afterObserved
                            card.asResolver().firstMethodOrNull {
                                name = "setTextVisible"; superclass()
                            }?.invoke(false)
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    object LauncherCardNameV13 : YukiBaseHooker() {
        override fun onHook() {
            val feature = ObservedFeature("remove_launcher_card_name", true)
            feature.setup {
                //Source TitleCardView
                "com.android.launcher3.card.TitleCardView".toClass().resolve().apply {
                    firstMethod { name = "initCardName" }.hook {
                        afterObserved(feature) {
                            val cardName = firstField { name = "cardName" }.of(instance).get<View>()
                                ?: return@afterObserved
                            cardName.isVisible = false
                            cardName.asResolver().firstMethod {
                                name = "setTextVisibility"; parameters(Boolean::class)
                            }.invoke(false)
                        }
                    }
                }

                //Source USCardContainerView
                "com.android.launcher3.card.uscard.USCardContainerView".toClass().resolve().apply {
                    firstMethod { name = "initCardName" }.hook {
                        afterObserved(feature) {
                            val cardName = firstField { name = "cardName" }.of(instance).get<View>()
                                ?: return@afterObserved
                            cardName.isVisible = false
                            cardName.asResolver().firstMethod {
                                name = "setTextVisibility"; parameters(Boolean::class)
                            }.invoke(false)
                        }
                    }
                }
            }
        }
    }
}
