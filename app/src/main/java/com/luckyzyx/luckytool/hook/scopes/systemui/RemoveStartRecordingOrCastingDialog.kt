package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveStartRecordingOrCastingDialog : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_start_recording_or_casting_dialog", true)
        feature.setup {
            //Source MediaProjectionServiceHelper
            "com.android.systemui.mediaprojection.MediaProjectionServiceHelper".toClass().resolve()
                .apply {
                    firstMethod {
                        name = "hasProjectionPermission"
                        parameters(Int::class, String::class)
                        returnType = Boolean::class
                    }.hook {
                        interceptObserved(feature, true)
                    }
                }
        }
    }
}
