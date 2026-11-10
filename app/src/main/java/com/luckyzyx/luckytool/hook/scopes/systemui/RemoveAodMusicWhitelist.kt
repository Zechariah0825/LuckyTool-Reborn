package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveAodMusicWhitelist : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_aod_music_whitelist", true)
        feature.setup {
            //Source AodMediaDataListener
            "com.oplusos.systemui.aod.mediapanel.AodMediaDataListener\$Companion".toClass().resolve().apply {
                firstMethod { name = "isAodMediaSupport" }.hook {
                    interceptObserved(feature, true)
                }
                firstMethod { name = "isAodMediaSupportWithoutFeature" }.hook {
                    interceptObserved(feature, true)
                }
            }
        }
    }
}
