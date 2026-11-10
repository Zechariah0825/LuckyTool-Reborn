package com.luckyzyx.luckytool.hook.scopes.securitypermission

import android.app.Activity
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AppStartDialogUseOldVersion : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("app_start_dialog_use_old_version", true)
        feature.setup {
            //Source AppStartConfirmDialogActivity
            "com.oplusos.securitypermission.permission.ui.AppStartConfirmDialogActivity".toClass()
                .resolve().apply {
                    firstMethod { name = "onCreate" }.hook {
                        beforeObserved(feature) {
                            val activity = instance<Activity>()
                            activity.intent.putExtra("activity_start_confirm_version", 0)
                        }
                    }
                }
        }
    }
}
