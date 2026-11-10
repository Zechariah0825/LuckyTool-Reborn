package com.luckyzyx.luckytool.hook.scopes.health

import android.app.Activity
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveHealthRootCheck : YukiBaseHooker() {
    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("remove_health_root_check_dialog", true)
        feature.setup {
            //Source SafetyCheckManager
            "com.heytap.health.safety.safetycheck.SafetyCheckManager".toClass().resolve().apply {
                firstMethod { parameters(Activity::class) }.hook {
                    interceptObserved(feature)
                }
            }
        }
    }
}
