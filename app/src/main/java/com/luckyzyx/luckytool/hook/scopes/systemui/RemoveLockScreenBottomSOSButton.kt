package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveLockScreenBottomSOSButton : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_lock_screen_bottom_sos_button", true)
        feature.setup {
            //Source OplusEmergencyButtonControllExImpl
            VariousClass(
                "com.oplus.systemui.keyguard.OplusEmergencyButtonControllExImpl", //C13
                "com.oplus.keyguard.OplusEmergencyButtonExImpl" //C14 C15
            ).toClass().resolve().apply {
                firstMethodOrNull { name = "disableShowEmergencyButton" }?.hook {
                    interceptObserved(feature, true)
                } ?: firstMethodOrNull { name = "shouldUpdateEmergencyCallButton" }?.hook {
                    beforeObserved(feature) {
                        firstField { name = "mEmergencyButton" }.of(instance).get<View>()
                            ?.isVisible = false
                        result = true
                    }
                } ?: firstMethod { name = "updateEmergencyCallButton" }.hook {
                    beforeObserved(feature) {
                        lastArg().set(false)
                    }
                }
            }
        }
    }
}
