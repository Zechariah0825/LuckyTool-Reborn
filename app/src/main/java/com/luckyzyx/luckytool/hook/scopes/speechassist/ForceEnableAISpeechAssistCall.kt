package com.luckyzyx.luckytool.hook.scopes.speechassist

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceEnableAISpeechAssistCall : YukiBaseHooker() {
    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("force_enable_ai_speechassist_call", true)
        feature.setup {
            //Source AiCallCommonBean
            "com.heytap.speechassist.aicall.setting.config.AiCallCommonBean".toClass().resolve().apply {
                firstMethod { name = "getSupportAiCall" }.hook {
                    interceptObserved(feature, true)
                }
                firstMethod { name = "getSupportAiCallV2" }.hook {
                    interceptObserved(feature, true)
                }
            }
        }
    }
}
