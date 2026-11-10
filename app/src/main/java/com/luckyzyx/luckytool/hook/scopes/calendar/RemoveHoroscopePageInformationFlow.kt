package com.luckyzyx.luckytool.hook.scopes.calendar

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.isVisible
import com.highcapable.betterandroid.ui.extension.view.removeSelf
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveHoroscopePageInformationFlow : YukiBaseHooker() {
    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("remove_horoscope_page_information_flow", true)
        feature.setup {
            //Source HoroscopeFragment -> H5InterfaceHelper getHoroscopeUrl
            "com.android.calendar.module.subscription.horoscope.HoroscopeFragment".toClass().resolve()
                .apply {
                    firstMethod { name = "onViewCreated"; parameterCount = 2 }.hook {
                        afterObserved(feature) {
                            val viewGroup = firstArg().get<ViewGroup>() ?: return@afterObserved
                            val res = viewGroup.resources
                            viewGroup.findViewById<View>(
                                res.getIdentifier(
                                    "web_title_layout", "id",
                                    this@RemoveHoroscopePageInformationFlow.packageName
                                )
                            )?.removeSelf()
                            viewGroup.findViewById<WebView>(
                                res.getIdentifier(
                                    "webView", "id",
                                    this@RemoveHoroscopePageInformationFlow.packageName
                                )
                            )?.removeSelf()
                            viewGroup.findViewById<View>(
                                res.getIdentifier(
                                    "fb_to_top", "id",
                                    this@RemoveHoroscopePageInformationFlow.packageName
                                )
                            )?.removeSelf()
                            viewGroup.findViewById<View>(
                                res.getIdentifier(
                                    "tv_source", "id",
                                    this@RemoveHoroscopePageInformationFlow.packageName
                                )
                            )?.isVisible = true
                        }
                    }
                }
        }
    }
}
