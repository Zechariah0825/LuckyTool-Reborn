package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveDeviceNameChangeLimit : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_device_name_change_limit", true)
        feature.setup {
            //Source PhoneNameSettingsActivity -> AlertDialog

            //Source PhoneNameVerifyUtil
            "com.oplus.settings.feature.deviceinfo.aboutphone.PhoneNameVerifyUtil".toClass().resolve()
                .apply {
                    firstMethod { name = "activeVerifyPhoneName" }.hook {
                        beforeObserved(feature) {
                            val callback = lastArg().get() ?: return@beforeObserved
                            callback.asResolver().firstMethod { name = "onSuccess" }.invoke(null)
                            result = null
                        }
                    }
                    firstMethod { name = "timeScheduleVerifyPhoneName" }.hook {
                        interceptObserved(feature)
                    }
                }

            //Source WirelessDeviceVerifyUtils
            "com.oplus.settings.utils.WirelessDeviceVerifyUtils".toClass().resolve().apply {
                firstMethod { name = "activeVerifyPhoneName" }.hook {
                    beforeObserved(feature) {
                        val callback = lastArg().get() ?: return@beforeObserved
                        callback.asResolver().firstMethod { name = "onSuccess" }.invoke(null)
                        result = null
                    }
                }
            }

            //Source OplusDeviceInfoUtils
            "com.oplus.settings.utils.OplusDeviceInfoUtils".toClass().resolve().apply {
                firstMethod { name = "getVerifyNameCondition" }.hook {
                    interceptObserved(feature, false)
                }
            }
        }
    }
}
