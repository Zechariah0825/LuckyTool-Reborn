package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveUSBConnectDialog : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_usb_connect_dialog", true)
        feature.setup {
            //Source UsbService
            VariousClass(
                "com.coloros.systemui.notification.usb.UsbService", //A11
                "com.oplusos.systemui.notification.usb.UsbService",
                "com.oplus.systemui.usb.UsbService" //C14 C15
            ).toClass().resolve().apply {
                (firstMethodOrNull { name = "onUsbConnected" }
                    ?: firstMethod { name { it.contains("onUsbConnected") } }).hook {
                    beforeObserved(feature) {
                        val instance = instanceOrNull ?: firstArg().get()
                        val context = lastArg().get<Context>() ?: return@beforeObserved
                        firstMethod { name = "onUsbSelect" }.of(instance).invoke(1)
                        firstMethod { name = "updateAdbNotification" }.of(instance).invoke(context)
                        firstMethod { name = "updateUsbNotification" }.let {
                            val contextIndex = it.self.parameterTypes.indexOf(classOf<Context>())
                            if (contextIndex == 0) it.of(instance).invoke(context, 1)
                            else it.of(instance).invoke(1, context)
                        }
                        firstMethod { name = "changeUsbConfig" }.let {
                            val contextIndex = it.self.parameterTypes.indexOf(classOf<Context>())
                            if (contextIndex == 0) it.of(instance).invoke(context, 1)
                            else it.of(instance).invoke(1, context)
                        }
                        result = null
                    }
                }
                firstMethod { name = "updateUsbNotification" }.hook {
                    beforeObserved(feature) {
                        firstField {
                            name { it.contains("NeedShowUsbDialog", true) }
                        }.of(instance).set(false)
                    }
                }
            }
        }
    }
}
