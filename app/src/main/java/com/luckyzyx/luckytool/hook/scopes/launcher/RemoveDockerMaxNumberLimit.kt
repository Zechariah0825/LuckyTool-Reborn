package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import com.luckyzyx.luckytool.hook.utils.launcher.LauncherAppStateUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveDockerMaxNumberLimit : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_docker_max_number_limit", true)
        feature.setup {
            //Source ExpandConfig
            "com.android.launcher3.hotseat.expand.ExpandConfig".toClass().resolve().apply {
                firstMethod {
                    name = "getHotseatNormalItemsMaxCountBy"
                    parameters(Boolean::class, Boolean::class)
                    returnType = Int::class
                }.hook {
                    afterObserved(feature) {
                        LauncherAppStateUtils(hostClassLoader!!).apply {
                            val state = getInstanceNoCreate() ?: return@afterObserved
                            val idp = getInvariantDeviceProfile(state) ?: return@afterObserved
                            val col = idp.asResolver().firstMethod {
                                name = "getNumColumns"; superclass()
                            }.invoke<Int>() ?: return@afterObserved
                            val res = result<Int>() ?: return@afterObserved
                            if (col > res) result = col
                        }
                    }
                }
            }
        }
    }
}
