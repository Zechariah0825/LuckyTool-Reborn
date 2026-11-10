package com.luckyzyx.luckytool.hook.scopes.launcher

import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.hookAll
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveBottomAppIconOfRecentTaskList : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_bottom_app_icon_of_recent_task_list", true)
        feature.setup {
            //Source DockView
            "com.oplus.quickstep.dock.DockView".toClass().resolve().apply {
                firstMethodOrNull { name = "setVisibilityAlpha" }?.hook {
                    afterObserved(feature) {
                        instance<View>().isVisible = false
                    }
                } ?: run {
                    constructor { }.hookAll {
                        afterObserved(feature) {
                            instance<View>().isVisible = false
                        }
                    }
                }
                firstMethodOrNull { name = "hideDockView" }?.hook {
                    beforeObserved(feature) {
                        firstArg().set(true)
                    }
                }
            }

            //Source DockViewController C14+
            "com.oplus.quickstep.dock.DockViewController".toClassOrNull()?.resolve()?.apply {
                firstMethod { name = "onRecentsViewOrientationChange" }.hook {
                    beforeObserved(feature) {
                        firstArg().set(false)
                    }
                }
                firstMethod { name = "updateOnTaskDisplayModeChange" }.hook {
                    beforeObserved(feature) {
                        firstArg().set(true)
                    }
                }
                firstMethod { name = "updateOnLauncherMultiWindowChange" }.hook {
                    beforeObserved(feature) {
                        firstArg().set(true)
                    }
                }
            }
        }
    }
}
