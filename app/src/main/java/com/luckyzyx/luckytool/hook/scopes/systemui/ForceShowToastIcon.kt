package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import android.content.Context
import android.widget.ImageView
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.PackageUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceShowToastIcon : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("force_show_toast_icon", true)
        feature.setup {
            //Source OplusSystemUIToast
            "com.oplus.systemui.toast.OplusSystemUIToast".toClass().resolve().apply {
                firstConstructor { parameterCount = 7 }.hook {
                    afterObserved(feature) {
                        val context = firstField { type = Context::class }.of(instance)
                            .get<Context>() ?: return@afterObserved
                        val packName = arg(3).get<String>() ?: ""
                        val mIconView = firstField { type = ImageView::class }.of(instance)
                            .get<ImageView>() ?: return@afterObserved
                        val icon = PackageUtils(context.packageManager).getApplicationIcon(packName)
                            ?: return@afterObserved
                        mIconView.setImageDrawable(icon)
                        mIconView.isVisible = true
                    }
                }
            }
        }
    }
}
