package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveBackGestureConfirmationLimit : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_back_gesture_confirmation_limit", true)
        feature.setup {
            //Source SideGestureDetector
            "com.oplus.systemui.navigationbar.gesture.sidegesture.SideGestureDetector".toClass()
                .resolve().apply {
                firstMethod {
                    name = "shouldRespondToGesture"
                    emptyParameters()
                    returnType = Boolean::class
                }.hook {
                    beforeObserved(feature) {
                        firstField {
                            name = "mIsExitMisTouchPreventionFlag"
                            type = Boolean::class
                        }.of(instance).set(true)
                    }
                }
                firstMethod {
                    name = "shouldInjectToGestureMode"
                    emptyParameters()
                    returnType = Boolean::class
                }.hook {
                    beforeObserved(feature) {
                        firstField {
                            name = "mIsFirstGestureInGameMode"
                            type = Boolean::class
                        }.of(instance).set(false)
                    }
                }
            }
        }
    }
}
