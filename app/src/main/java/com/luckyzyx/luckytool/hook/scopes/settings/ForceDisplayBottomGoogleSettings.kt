package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayBottomGoogleSettings : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("force_display_bottom_google_settings", true)
        feature.setup {
            //Source GooglePreferenceController
            "com.oplus.settings.feature.homepage.controller.GooglePreferenceController".toClass()
                .resolve().apply {
                    firstMethod { name = "getAvailabilityStatus" }.hook {
                        interceptObserved(feature, 0)
                    }
                }
        }
    }
}
