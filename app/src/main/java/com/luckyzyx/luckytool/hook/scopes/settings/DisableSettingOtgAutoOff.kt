package com.luckyzyx.luckytool.hook.scopes.settings

import android.content.Context
import android.os.customize.OplusCustomizeRestrictionManager
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableSettingOtgAutoOff : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("disable_otg_auto_off", true)
        feature.setup {
            //Source OtgConnectionOpenedPreferenceController
            VariousClass(
                "com.oplus.settings.feature.othersettings.controller.OtgConnectionOpenedPreferenceController",  //C14
                "com.oplus.settings.feature.spfunction.OtgConnectionOpenedPreferenceController"  //C14.1 C15
            ).toClass().resolve().apply {
                firstMethod { name = "isPreferenceSupport" }.hook {
                    beforeObserved(feature) {
                        val context = firstArg().get<Context>() ?: return@beforeObserved
                        val isUSBOtgDisabled =
                            OplusCustomizeRestrictionManager.getInstance(context).isUSBOtgDisabled
                        result = !isUSBOtgDisabled
                    }
                }
            }
        }
    }
}
