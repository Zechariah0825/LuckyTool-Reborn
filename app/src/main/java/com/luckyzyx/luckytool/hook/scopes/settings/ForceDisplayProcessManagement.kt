package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayProcessManagement : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("force_display_process_management", true)
        feature.setup {
            //com.oplus.settings.feature.process.RunningApplicationActivity
            //Source RunningApplicationsPreferenceController
            VariousClass(
                "com.oplus.settings.feature.othersettings.controller.RunningApplicationsPreferenceController", //C13 C14
                "com.oplus.settings.feature.spfunction.RunningApplicationsPreferenceController" //C14.1
            ).toClass().resolve().apply {
                firstMethod { name = "getAvailabilityStatus" }.hook {
                    interceptObserved(feature, 0)
                }
            }
            //Source RunningApplicationsNewPreferenceController
            "com.oplus.settings.feature.appmanager.controller.RunningApplicationsNewPreferenceController".toClass()
                .resolve().apply {
                    firstMethod { name = "getAvailabilityStatus" }.hook {
                        interceptObserved(feature, 0)
                    }
                }
        }
    }
}
