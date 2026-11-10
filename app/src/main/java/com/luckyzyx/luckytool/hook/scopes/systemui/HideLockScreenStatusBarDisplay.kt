package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HideLockScreenStatusBarDisplay : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("hide_lock_screen_status_bar_display", true)
        feature.setup {
            //Source KeyguardStatusBarView
            VariousClass(
                "com.android.systemui.statusbar.phone.KeyguardStatusBarView",  //C12.1 C13.1
                "com.android.systemui.statusbar.phone.KeyguardStatusBarView"  //C14
            ).toClass().resolve().apply {
                firstMethod { name = "setVisibility" }.hook {
                    beforeObserved(feature) {
                        firstArg().set(View.INVISIBLE)
                    }
                }
            }
        }
    }
}
