package com.luckyzyx.luckytool.hook.scopes.games

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableXModeFeature : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("enable_x_mode_feature", true)
        feature.setup {
            //Source CoolingBackClipHelper / CoolingBackClipFeature
            VariousClass(
                "business.module.perfmode.CoolingBackClipHelper", //V8
                "business.module.perfmode.CoolingBackClipFeature" //V9.0.0
            ).toClass().resolve().apply {
                firstMethod { parameterCount = 1;returnType = Any::class }.hook {
                    afterObserved(feature) {
                        result = true
                    }
                }
            }
        }
    }
}
