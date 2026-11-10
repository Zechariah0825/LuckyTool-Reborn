package com.luckyzyx.luckytool.hook.scopes.notificationmanager

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveNotificationManagerLimit : YukiBaseHooker() {
    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("remove_notification_manager_limit", true)
        feature.setup {
            //Source ControllerChannelGroup$AppItemListener -> 通知渠道列表允许通知锁
            "com.oplus.notificationmanager.property.uicontroller.ControllerChannelGroup\$AppItemListener".toClass()
                .resolve().apply {
                    firstMethod { name = "isSwitchEnabled" }.hook {
                        interceptObserved(feature, true)
                    }
                }
            //Source ControllerAllowNotificationChannel -> 通知渠道内允许通知锁
            "com.oplus.notificationmanager.property.uicontroller.ControllerAllowNotificationChannel".toClass()
                .resolve().apply {
                    firstMethod { name = "isNormAppEnabled" }.hook {
                        interceptObserved(feature, true)
                    }
                }
            //Source ControllerUnimportantChannel -> 通知渠道内不重要通知锁
            "com.oplus.notificationmanager.property.uicontroller.ControllerUnimportantChannel".toClass()
                .resolve().apply {
                    firstMethod { name = "isNormAppEnabled" }.hook {
                        interceptObserved(feature, true)
                    }
                }
            //Source ControllerAllowNotificationPkg -> 应用内允许通知锁
            "com.oplus.notificationmanager.property.uicontroller.ControllerAllowNotificationPkg".toClass()
                .resolve().apply {
                    firstMethod { name = "isNormAppEnabled" }.hook {
                        interceptObserved(feature, true)
                    }
                }
//
//        //Source ControllerAppLockBannerBubble -> 应用内气泡锁
//        "com.oplus.notificationmanager.property.uicontroller.ControllerAppLockBannerBubble".toClass()
//            .apply {
//                method { name = "isBubbleEnabled" }.hook {
//                    replaceToTrue()
//                }
//            }
//        //Source ControllerAppLockBannerBubble ->
//        "com.oplus.notificationmanager.property.uicontroller.ControllerChannelLockBannerBubble".toClass()
//            .apply {
//                method { name = "isBubbleEnabled" }.hook {
//                    replaceToTrue()
//                }
//            }
//        //Source ControllerAppLockBannerBubble ->
//        "com.oplus.notificationmanager.property.uicontroller.ControllerConversationLockBannerBubble".toClass()
//            .apply {
//                method { name = "isBubbleEnabled" }.hook {
//                    replaceToTrue()
//                }
//            }
        }
    }
}
