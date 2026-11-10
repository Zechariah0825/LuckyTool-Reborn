package com.luckyzyx.luckytool.hook.scopes.games

import android.app.Activity
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableDeveloperPage : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("enable_developer_page", true)
        feature.setup {
            //Source GameDevelopOptionsActivity
            ("business.compact.activity.GameDevelopOptionsActivity".toClassOrNull() ?: run {
                feature.failed(ClassNotFoundException("business.compact.activity.GameDevelopOptionsActivity"))
                return
            }).resolve().apply {
                firstMethod {
                    name = "onCreate"
                    parameterCount = 1
                }.hook {
                    beforeObserved(feature) {
                        instance<Activity>().intent.apply {
                            putExtra("gameDevelopOptions", "GameDevelopOptionsActivity")
                            putExtra("openAutomation", -1)
                        }
                        firstArg().set(null)
                    }
                }
            }
        }
    }
}
