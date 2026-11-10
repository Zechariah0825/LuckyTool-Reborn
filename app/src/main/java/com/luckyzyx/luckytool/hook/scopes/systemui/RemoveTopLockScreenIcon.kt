package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import com.luckyzyx.luckytool.diagnostics.afterObserved
import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveTopLockScreenIcon : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_top_lock_screen_icon", true)
        feature.setup {
            //Source LockIcon (C12 C13唯一实现)
            "com.android.systemui.statusbar.phone.LockIcon".toClassOrNull()?.resolve()?.apply {
                firstMethod { name = "updateIconVisibility" }.hook {
                    beforeObserved(feature) {
                        firstArg().set(false)
                    }
                }
            }

            //Source LockIconView (C14 C15) OplusLockIconView (C16 C17)
            //注意: VariousClass 默认加载器在本环境不指向宿主, 必须显式传入 hostClassLoader
            val lockIconView = VariousClass(
                "com.android.keyguard.LockIconView",
                "com.android.keyguard.OplusLockIconView"
            ).toClassOrNull() ?: return

            lockIconView.resolve().apply {
                //构造完成即隐藏图标本体与背景圆, 覆盖初始可见的场景
                firstConstructor {}.hook {
                    afterObserved(feature) {
                        instance<View>().isVisible = false
                    }
                }
                //C14-C17所有控制器显隐均通过 mView.setVisibility, 统一强制 INVISIBLE
                //(仅限本类实例, 父类方法全局生效故需过滤)
                firstMethod {
                    name = "setVisibility"
                    parameters(Int::class)
                    superclass()
                }.hook {
                    before {
                        if (instance<View>().javaClass != lockIconView) return@before
                        feature.callback { firstArg().set(4) }
                    }
                }
            }
        }
    }
}
