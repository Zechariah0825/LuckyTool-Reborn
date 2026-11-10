package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableCustomAppLanguage : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("enable_custom_app_language", true)
        feature.setup {
            //Source AppLocaleUtil
            "com.android.settings.applications.AppLocaleUtil".toClass().resolve().apply {
                firstMethod { name = "canDisplayLocaleUi" }.hook {
                    interceptObserved(feature, true)
                }
            }
            //Source AppLocalePreferenceController
            "com.android.settings.applications.appinfo.AppLocalePreferenceController".toClass()
                .resolve().apply {
                firstMethod { name = "getAvailabilityStatus" }.hook {
                    interceptObserved(feature, 0)
                }
            }
        }
    }
}
