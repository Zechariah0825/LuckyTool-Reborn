package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveLockScreenCloseNotificationButton : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_lock_screen_close_notification_button", true)
        feature.setup {
            //Source NotificationPanelViewExt
            VariousClass(
                "com.oplusos.systemui.notification.extend.NotificationPanelViewExt",  //C12 C13
                "com.oplus.systemui.notification.extend.OplusNotificationCloseButtonImp"  //C14
            ).toClass().resolve().apply {
                firstMethod { name = "setNotificationCloseButton" }.hook {
                    interceptObserved(feature)
                }
            }
        }
    }
}
