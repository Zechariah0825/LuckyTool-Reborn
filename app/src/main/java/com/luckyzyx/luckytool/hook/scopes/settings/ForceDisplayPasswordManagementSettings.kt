package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayPasswordManagementSettings : YukiBaseHooker() {

    override fun onHook() {
        val feature = ObservedFeature("force_display_password_management_settings", true)
        feature.setup {
            if (getOSVersionCode >= 30) loadHooker(PasswordManagementSettings)
            else loadHooker(PasswordManagementSettingsV13)
        }
    }

    @Obfuscate
    object PasswordManagementSettings : YukiBaseHooker() {
        override fun onHook() {
            val feature = ObservedFeature("force_display_password_management_settings", true)
            feature.setup {
                //Source PasswordManagerPreferenceController
                "com.oplus.settings.feature.password.controller.PasswordManagerPreferenceController".toClass()
                    .resolve().apply {
                        firstMethod { name = "isPreferenceNotAvailable" }.hook {
                            interceptObserved(feature, false)
                        }
                    }
            }
        }
    }

    @Obfuscate
    object PasswordManagementSettingsV13 : YukiBaseHooker() {
        override fun onHook() {
            val feature = ObservedFeature("force_display_password_management_settings", true)
            feature.setup {
                //Source PasswordManagerPreferenceController
                "com.oplus.settings.feature.password.controller.PasswordManagerPreferenceController".toClass()
                    .resolve().apply {
                        firstMethod { name = "displayPreference" }.hook {
                            afterObserved(feature) {
                                val preferenceScreen = firstArg().get() ?: return@afterObserved
                                val preference = preferenceScreen.asResolver().firstMethod {
                                    name = "findPreference"
                                    parameters(CharSequence::class)
                                    superclass()
                                }.invoke("key_password_manager") ?: return@afterObserved
                                preference.asResolver().firstMethod {
                                    name = "setVisible"
                                    parameters(Boolean::class)
                                    superclass()
                                }.invoke(true)
                            }
                        }
                        firstMethod { name = "updateState" }.hook {
                            afterObserved(feature) {
                                val preference = firstArg().get() ?: return@afterObserved
                                preference.asResolver().firstMethod {
                                    name = "setVisible"
                                    parameters(Boolean::class)
                                    superclass()
                                }.invoke(true)
                            }
                        }
                    }
            }
        }
    }
}
