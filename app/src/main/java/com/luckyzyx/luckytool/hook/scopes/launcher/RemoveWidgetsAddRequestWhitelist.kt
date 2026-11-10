package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveWidgetsAddRequestWhitelist : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_widgets_add_request_whitelist", true)
        feature.setup {
            //Source WidgetControlHelper
            "com.android.launcher3.widget.WidgetControlHelper".toClassOrNull() ?: run {
                feature.failed(ClassNotFoundException("WidgetControlHelper"))
                return
            }
            //Source AddItemActivity
            "com.android.launcher3.dragndrop.AddItemActivity".toClass().resolve().apply {
                firstMethodOrNull {
                    name = "isAllowedAddWidget"
                    parameterCount { it in 1..2 }
                }?.hook {
                    interceptObserved(feature, true)
                }
            }
        }
    }
}
