package com.luckyzyx.luckytool.hook.scopes.settings

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableSwipeUpNavigationGesture : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("enable_swipe_up_navigation_gesture", true)
        feature.setup {
            //Source NavBarSettingsValueUtil
            "com.oplus.settings.feature.navbar.NavBarSettingsValueUtil".toClass().resolve().apply {
                firstMethod {
                    name = "getGestureUpModeAvailable"
                    parameters(Context::class)
                    returnType = Int::class
                }.hook {
                    interceptObserved(feature, 0)
                }
            }
        }
    }
}
