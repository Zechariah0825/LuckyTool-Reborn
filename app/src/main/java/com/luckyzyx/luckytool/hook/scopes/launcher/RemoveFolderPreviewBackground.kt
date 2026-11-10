package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.hookAll
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveFolderPreviewBackground : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_folder_preview_background", true)
        feature.setup {
            val osCode = getOSVersionCode
            if (osCode >= 34) loadHooker(FolderPreviewBackground)
            else loadHooker(FolderPreviewBackgroundV14)
        }
    }

    @Obfuscate
    object FolderPreviewBackground : YukiBaseHooker() {
        override fun onHook() {
            val feature = ObservedFeature("remove_folder_preview_background", true)
            feature.setup {
                //Source OplusPreviewBackground
                "com.android.launcher3.folder.OplusPreviewBackground".toClass().resolve().apply {
                    firstMethod { name = "setBackground" }.hook {
                        beforeObserved(feature) {
                            firstField { name = "mBgDrawable" }.of(instance).set(null)
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    object FolderPreviewBackgroundV14 : YukiBaseHooker() {
        override fun onHook() {
            val feature = ObservedFeature("remove_folder_preview_background", true)
            feature.setup {
                //Source OplusPreviewBackground folder_icon_bg big_folder_bg
                "com.android.launcher3.folder.OplusPreviewBackground".toClass().resolve().apply {
                    method { name = "setup" }.hookAll {
                        afterObserved(feature) {
                            firstField { name = "mBgDrawable" }.of(instance).set(null)
                        }
                    }
                    firstMethod { name = "drawBackground" }.hook {
                        interceptObserved(feature)
                    }
                }
                if (SDK < A13) return
                //Source OplusFolderAnimationManager
                "com.android.launcher3.folder.OplusFolderAnimationManager".toClass().resolve().apply {
                    firstMethod { name = "getFolderBackgroundAnimator" }.hook {
                        interceptObserved(feature)
                    }
                }
            }
        }
    }
}
