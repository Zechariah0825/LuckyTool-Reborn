package com.luckyzyx.luckytool.hook.scopes.weather

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RestoreRainfallCloudMapPage : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("restore_rainfall_cloud_map_page", true)
        feature.setup {
            //Source IndexOperationsManager
            "com.oplus.weather.indexoperations.IndexOperationsManager".toClassOrNull()?.resolve()
                ?.apply {
                    firstMethod { name = "supportIndexOperationsFeature" }.hook {
                        interceptObserved(feature, false)
                    }
                }
        }
    }
}
