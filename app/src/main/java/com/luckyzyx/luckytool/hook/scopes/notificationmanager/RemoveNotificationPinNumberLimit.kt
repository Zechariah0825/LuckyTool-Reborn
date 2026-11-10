package com.luckyzyx.luckytool.hook.scopes.notificationmanager

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hookAll
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveNotificationPinNumberLimit : YukiBaseHooker() {
    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("remove_notification_pin_number_limit", true)
        feature.setup {
            //Source AppNotificationTopController
            "com.oplus.notificationmanager.property.uicontroller.AppNotificationTopController".toClass()
                .let {
                    it.resolve().apply {
                        method {
                            parameters(it, "androidx.preference.Preference", Any::class)
                            returnType = Boolean::class
                        }.hookAll {
                            beforeObserved(feature) {
                                val controller = firstArg().get() ?: return@beforeObserved
                                val bool = lastArg().get<Boolean>() ?: false
                                controller.asResolver().firstMethod { name = "onChange";superclass() }
                                    .invoke(bool)
                                result = true
                            }
                        }
                    }
                }
        }
    }
}
