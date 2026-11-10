package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HidePanoramicAodStatusBar : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("hide_panoramic_aod_status_bar", true)
        feature.setup {
            //Source AodData
            val aodData = "com.oplus.systemui.aod.aodclock.constant.AodData".toClass()

            //Source KeyguardStatusBarViewExImpl
            "com.oplus.systemui.statusbar.phone.KeyguardStatusBarViewExImpl".toClass().resolve().apply {
                firstMethod {
                    name = "hookDozingState"
                    parameters(Boolean::class)
                    returnType = Boolean::class
                }.hook {
                    beforeObserved(feature) {
                        val context = firstField { type = Context::class }.of(instance).get<Context>()
                            ?: return@beforeObserved
                        val aodDataInstance = aodData.resolve().firstMethod {
                            name = "getInstance"
                            parameters(Context::class)
                        }.invoke(context) ?: return@beforeObserved
                        val isPanoramicAod = aodDataInstance.asResolver().firstMethod {
                            name = "isPanoramicAod"
                        }.invoke<Boolean>() ?: return@beforeObserved
                        if (arg(0).get<Boolean>() ?: false && isPanoramicAod) result = false
                    }
                }
            }
        }
    }
}
