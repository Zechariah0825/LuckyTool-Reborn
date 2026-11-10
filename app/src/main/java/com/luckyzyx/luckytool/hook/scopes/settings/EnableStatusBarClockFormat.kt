package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableStatusBarClockFormat : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("enable_statusbar_clock_format", true)
        feature.setup {
            //Source RmStatusbarClockPreferenceController
            "com.oplus.settings.feature.notification.controller.RmStatusbarClockPreferenceController".toClass()
                .resolve().apply {
                    firstMethod { name = "getAvailabilityStatus" }.hook {
                        interceptObserved(feature, 0)
                    }
                }
        }
    }
}
