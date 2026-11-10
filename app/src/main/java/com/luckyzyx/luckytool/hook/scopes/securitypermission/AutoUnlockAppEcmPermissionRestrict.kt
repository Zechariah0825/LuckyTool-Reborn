package com.luckyzyx.luckytool.hook.scopes.securitypermission

import android.app.Activity
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import com.luckyzyx.luckytool.utils.EcmUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AutoUnlockAppEcmPermissionRestrict : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("auto_unlock_app_ecm_permission_restrict", true)
        feature.setup {
            //Source PermissionGroupsActivity
            "com.oplusos.securitypermission.permission.PermissionGroupsActivity".toClass().resolve()
                .apply {
                    firstMethod { name = "onCreate" }.hook {
                        beforeObserved(feature) {
                            val activity = instance<Activity>()
                            val packName = activity.intent.getStringExtra("packageName")
                                ?: activity.intent.getStringExtra("mPackageName")
                                ?: return@beforeObserved
                            EcmUtils(activity).autoUnlockRestrictedSettings(packName)
                        }
                    }
                }
        }
    }
}
