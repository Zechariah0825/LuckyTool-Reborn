package com.luckyzyx.luckytool.hook.scopes.externalstorage

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveStorageLimit : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_storage_limit", true)
        feature.setup {
            //Source ExternalStorageProvider
            "com.android.externalstorage.ExternalStorageProvider".toClass().resolve().apply {
                firstMethodOrNull { name = "shouldBlockDirectoryFromTree" }?.hook {
                    interceptObserved(feature, false)
                }
            }
        }
    }
}
