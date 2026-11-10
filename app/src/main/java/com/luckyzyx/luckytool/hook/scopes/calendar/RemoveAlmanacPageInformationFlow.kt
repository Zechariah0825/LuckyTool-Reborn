package com.luckyzyx.luckytool.hook.scopes.calendar

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveAlmanacPageInformationFlow : YukiBaseHooker() {
    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("remove_almanac_page_information_flow", true)
        feature.setup {
            //Source AlmanacPagesAdapter -> H5InterfaceHelper getAlmanacUrl
            "com.android.calendar.module.subscription.almanac.adapter.AlmanacPagesAdapter".toClass()
                .resolve().apply {
                    firstMethod { name = "onCreateViewHolder" }.hook {
                        beforeObserved(feature) {
                            lastArg().set(0)
                        }
                    }
                }
        }
    }
}
