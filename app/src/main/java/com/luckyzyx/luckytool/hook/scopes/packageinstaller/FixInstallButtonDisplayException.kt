package com.luckyzyx.luckytool.hook.scopes.packageinstaller

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate
import java.security.SecureRandom

@Obfuscate
object FixInstallButtonDisplayException : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("fix_install_button_display_exception", true)
        feature.setup {
            //Source ConfusedButton
            "com.android.packageinstaller.oplus.view.ConfusedButton".toClass().resolve().apply {
                firstMethod { name = "getAccessibilityViewId" }.hook {
                    beforeObserved(feature) {
                        firstMethod { name = "setCts" }.of(instance).invoke(true)
                        firstField { type = SecureRandom::class }.of(instance).set(SecureRandom())
                    }
                }
                firstMethodOrNull { name = "getText" }?.hook {
                    beforeObserved(feature) {
                        firstMethod { name = "setCts" }.of(instance).invoke(true)
                        firstField { type = SecureRandom::class }.of(instance).set(SecureRandom())
                    }
                }
            }
            //Source ConfusedTextView
            "com.android.packageinstaller.oplus.view.ConfusedTextView".toClass().resolve().apply {
                firstMethod { name = "getAccessibilityViewId" }.hook {
                    beforeObserved(feature) {
                        firstMethod { name = "setCts" }.of(instance).invoke(true)
                        firstField { type = SecureRandom::class }.of(instance).set(SecureRandom())
                    }
                }
                firstMethodOrNull { name = "getText" }?.hook {
                    beforeObserved(feature) {
                        firstMethod { name = "setCts" }.of(instance).invoke(true)
                        firstField { type = SecureRandom::class }.of(instance).set(SecureRandom())
                    }
                }
            }
        }
    }
}
