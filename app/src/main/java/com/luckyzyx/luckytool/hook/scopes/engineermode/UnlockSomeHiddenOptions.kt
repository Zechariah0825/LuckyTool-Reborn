package com.luckyzyx.luckytool.hook.scopes.engineermode

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object UnlockSomeHiddenOptions : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("unlock_some_hidden_options", true)
        feature.setup {
            //Source SecrecyServiceHelper
            "com.oplus.engineermode.impl.SecrecyServiceHelper".toClass().resolve().apply {
                firstMethod { name = "isSecrecySupported" }.hook {
                    interceptObserved(feature, true)
                }
                firstMethod { name = "getSecrecyState" }.hook {
                    interceptObserved(feature, false)
                }
            }
        }
    }
}
