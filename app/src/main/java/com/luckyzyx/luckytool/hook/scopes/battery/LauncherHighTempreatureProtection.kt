package com.luckyzyx.luckytool.hook.scopes.battery

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import android.content.BroadcastReceiver
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.PowerManager
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class LauncherHighTempreatureProtection(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    val key = "LauncherHighTempreatureProtection"
    override fun onHook() {
        val feature = ObservedFeature("remove_battery_temperature_control", true)
        feature.setup {
            //Source ThermalHandler high_temperature_shutdown_message / high_temperature_dialog_auto
            //Key oplus_settings_hightemp_protect 1004
            dexKitBridge.findClass {
                matcher {
                    fields {
                        addForType(classOf<Int>())
                        addForType(classOf<Context>())
                        addForType(classOf<Handler>())
                        addForType(classOf<PowerManager>())
                        addForType(classOf<SharedPreferences>())
                        addForType(classOf<BroadcastReceiver>())
                    }
                    methods {
                        add { name("handleMessage") }
                        add { paramTypes(classOf<Context>()) }
                        add { paramTypes(classOf<Int>(), classOf<Int>()) }
                    }
                }
            }.apply {
                checkDataList("LauncherHighTempreatureProtection")
                single().name.toClass().resolve().apply {
                    firstConstructor { parameterCount = 3 }.hook {
                        interceptObserved(feature)
                    }
                }
            }
        }
    }
}
