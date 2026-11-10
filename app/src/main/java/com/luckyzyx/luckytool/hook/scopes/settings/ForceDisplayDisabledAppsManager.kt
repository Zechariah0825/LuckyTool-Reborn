package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayDisabledAppsManager : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("force_display_disabled_apps_manager", true)
        feature.setup {
            //Source DisabledAppsPreferenceController
            "com.android.settings.applications.disableapps.DisabledAppsPreferenceController".toClass()
                .resolve().apply {
                    firstMethod { name = "getAvailabilityStatus" }.hook {
                        interceptObserved(feature, 0)
                    }
                }
        }
    }
}
