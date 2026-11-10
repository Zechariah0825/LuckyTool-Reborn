package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableVolumeBarThicknessEffect : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("disable_volume_bar_thickness_effect", true)
        feature.setup {
            //Source OplusVolumeDialogImpl C14+
            "com.oplus.systemui.volume.OplusVolumeDialogImpl".toClass().resolve().apply {
                firstMethod { name = "startThickAnim" }.hook {
                    interceptObserved(feature)
                }
            }
        }
    }
}
