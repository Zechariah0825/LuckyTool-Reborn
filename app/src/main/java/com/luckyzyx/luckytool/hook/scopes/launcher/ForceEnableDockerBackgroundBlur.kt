package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceEnableDockerBackgroundBlur : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("force_enable_docker_background_blur", true)
        feature.setup {
            // 旧版在 setDockerBackground() 内通过 createBlurDrawable() 生成模糊背景并设置到
            // mShortcutsAndWidgets；C17 起该方法被内联进 setDockerBackground()，模糊改由
            // mDockerBackgroundDrawable + callback 绘制，background 被显式置 null，旧的
            // after 重设 background 的写法不再有效。两版共通的强制方式：在
            // setDockerBackground() 调用栈内把模糊分支条件强制为 true，让宿主走原生模糊路径。
            val forcingBlur = ThreadLocal<Boolean>()
            //Source OplusHotseat
            "com.android.launcher3.OplusHotseat".toClass().resolve().apply {
                firstMethod { name = "setDockerBackground" }.hook {
                    beforeObserved(feature) {
                        forcingBlur.set(true)
                    }
                    afterObserved(feature) {
                        forcingBlur.remove()
                    }
                }
            }
            //Source OplusBlurProperties
            "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties".toClass()
                .resolve().apply {
                    firstMethod { name = "isSupportNewBlur" }.hook {
                        before {
                            if (forcingBlur.get() == true) feature.callback { result = true }
                        }
                    }
                }
            //Source ScreenUtils
            "com.android.common.util.ScreenUtils".toClass().resolve().apply {
                firstMethodOrNull { name = "hasLargeDisplayFeatures"; emptyParameters() }?.hook {
                    before {
                        if (forcingBlur.get() == true) feature.callback { result = true }
                    }
                }
            }
        }
    }
}
