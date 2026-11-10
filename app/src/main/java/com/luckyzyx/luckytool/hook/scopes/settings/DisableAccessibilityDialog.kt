package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableAccessibilityDialog : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("disable_accessibility_warning_dialog", true)
        feature.setup {
            //Source SpecialPermRiskConfirmHelper
            "com.oplus.settings.applications.specialaccess.SpecialPermRiskConfirmHelper".toClass()
                .resolve().apply {
                    firstMethod {
                        name = "show"
                        returnType = Boolean::class
                    }.intercept {
                        feature.callback {
                            val runnale = lastArg() as? Runnable
                            runnale?.run()
                            true
                        }
                    }
                }
        }
    }
}
