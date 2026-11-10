package com.luckyzyx.luckytool.hook.scopes.camera

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveFilterModelLimit : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_filter_model_limit", true)
        feature.setup {
            //Source SystemUtil
            "com.oplus.ocs.camera.ipusdk.processunit.filter.list.SystemUtil".toClass().resolve().apply {
                firstMethod { name = "isMarketNameContainSeriesNum" }.hook {
                    interceptObserved(feature, true)
                }
            }
        }
    }
}
