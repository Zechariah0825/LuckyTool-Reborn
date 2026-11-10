package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AllowDisablingSystemApps : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("allow_disabling_system_apps", true)
        feature.setup {
            //Source AppButtonsPreferenceControllerAdaptor
            "com.oplus.settings.adaptor.AppButtonsPreferenceControllerAdaptor".toClass().resolve().apply {
                firstMethod { name = "setUninstallButtonEnabled" }.hook {
                    beforeObserved(feature) {
                        firstArg().set(true)
                    }
                }
            }
        }
    }
}
