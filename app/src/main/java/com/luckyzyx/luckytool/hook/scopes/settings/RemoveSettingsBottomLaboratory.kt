package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveSettingsBottomLaboratory : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_settings_bottom_laboratory", true)
        feature.setup {
            //Source TopLevelLaboratoryPreferenceController
            "com.oplus.settings.feature.homepage.TopLevelLaboratoryPreferenceController".toClass()
                .resolve().apply {
                    firstMethod { name = "getAvailabilityStatus" }.hook {
                        interceptObserved(feature, 3)
                    }
                }
        }
    }
}
