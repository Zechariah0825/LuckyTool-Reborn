package com.luckyzyx.luckytool.hook.scopes.pictorial

import android.widget.LinearLayout
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hookAll
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveVideoSaveWaterMark : YukiBaseHooker() {
    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("remove_video_save_watermark", true)
        feature.setup {
            //Source VideoWaterMarkView -> view_video_water_mark
            "com.heytap.pictorial.data.VideoWaterMarkView".toClass().resolve().apply {
                constructor {}.hookAll {
                    afterObserved(feature) {
                        instance<LinearLayout>().removeAllViews()
                    }
                }
            }
        }
    }
}
