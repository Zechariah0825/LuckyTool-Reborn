package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveLowBatteryDialogWarning : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_low_battery_dialog_warning", true)
        feature.setup {
            //Source OplusPowerNotificationWarnings
            VariousClass(
                "com.oplusos.systemui.notification.power.OplusPowerNotificationWarnings", //C13
                "com.oplus.systemui.statusbar.notification.power.OplusPowerNotificationWarnings" //C14
            ).toClass().resolve().apply {
                firstMethodOrNull { name = "createSavePowerDialog" }?.hook {
                    interceptObserved(feature)
                }
                firstMethodOrNull { name = "createSuperSavePowerDialog" }?.hook {
                    interceptObserved(feature)
                }
                firstMethodOrNull { name = "showLowBatteryWarning" }?.hook {
                    interceptObserved(feature, false)
                }
            }
        }
    }
}
