package com.luckyzyx.luckytool.hook.scopes.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AutoJumpAccessibilitySettings : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("auto_jump_accessibility_settings", true)
        feature.setup {
            //Source SettingsActivity
            "com.android.settings.SettingsActivity".toClass().resolve().apply {
                firstMethod { name = "onCreate" }.hook {
                    before {
                        val activity = instance<Activity>()
                        val uri = activity.referrer
                        val packName = uri?.host ?: return@before

                        val intent = firstMethod { name = "getIntent" }.of(instance).invoke<Intent>()
                            ?: return@before
                        if (intent.action != Settings.ACTION_ACCESSIBILITY_SETTINGS) return@before
                        feature.callback {

                            val helper =
                                "com.oplus.settings.feature.accessibility.controller.AccessibilityDataHelper".toClass()
                            val helperInstance = helper.createInstance(activity, null)

                            val allInfos = helperInstance.asResolver()
                                .firstMethod { name = "loadAccessibilityInfos" }
                                .invoke<java.util.HashMap<String, Bundle>>() ?: return@callback

                            val bundle = allInfos.filterKeys { it.startsWith(packName) }.let {
                                it[it.keys.first()]
                            } ?: return@callback

                            activity.startActivity(Intent(Intent.ACTION_MAIN).apply {
                                component = ComponentName(
                                    "com.android.settings", "com.android.settings.SubSettings"
                                )
                                putExtra(
                                    ":settings:show_fragment",
                                    "com.oplus.settings.feature.accessibility.OplusToggleAccessibilityServicePreferenceFragment"
                                )
                                putExtra(":settings:show_fragment_args", bundle)
                            })
                        }
                    }
                }
            }
        }
    }
}
