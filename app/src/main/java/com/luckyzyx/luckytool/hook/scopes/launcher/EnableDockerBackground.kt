package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableDockerBackground : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("enable_docker_background", true)
        feature.setup {
            //Source ScreenUtils
            "com.android.common.util.ScreenUtils".toClass().resolve().apply {
                firstMethod { name = "isSupportDockerExpandScreen" }.hook {
                    interceptObserved(feature, true)
                }
                //OplusTaskHeaderView showSplitWindowIcon
    //            firstMethod { name = "hasLargeDisplayFeatures" }.hook {
    //                replaceToTrue()
    //            }
            }
        }
    }
}
