package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import android.content.Context
import android.media.AudioManager
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableHeadphoneHighVolumeWarning : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("disable_headphone_high_volume_warning", true)
        feature.setup {
            //Sourcce VolumeDialogImplEx
            VariousClass(
                "com.oplusos.systemui.volume.VolumeDialogImplEx", //C13
                "com.oplus.systemui.volume.OplusVolumeDialogImpl" //C14
            ).toClass().resolve().apply {
                firstMethod { name = "init" }.hook {
                    afterObserved(feature) {
                        val mContext = firstField { name = "mContext" }.of(instance).get<Context>()
                            ?: return@afterObserved
                        val audioManager = mContext.getSystemService(classOf<AudioManager>())
                            ?: return@afterObserved
                        audioManager.asResolver().firstMethod { name = "disableSafeMediaVolume" }.invoke()
                    }
                }
            }
        }
    }
}
