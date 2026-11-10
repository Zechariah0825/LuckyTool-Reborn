package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import android.view.OplusWindowManager
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AutoWakeUpFaceUnlockNotification : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("auto_wake_up_face_unlock_notification", true)
        feature.setup {
            //Source WakeupScreenHelper
            VariousClass(
                "com.oplusos.systemui.notification.helper.WakeupScreenHelper", //C12.1
                "com.oplus.systemui.statusbar.notification.helper.WakeupScreenHelper", //C13
                "com.oplus.systemui.notification.interruption.wakeup.WakeupScreenHelper" //C14 C15
            ).toClass().resolve().apply {
                firstMethod { name = "powerOnScreen" }.hook {
                    afterObserved(feature) {
                        OplusWindowManager().requestKeyguard("android.policy:POWER")
                    }
                }
            }
        }
    }
}
