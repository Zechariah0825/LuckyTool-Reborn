package com.luckyzyx.luckytool.hook.scopes.weather

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hookAll
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object Enable15DayWeatherExpandList : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("enable_15_day_weather_expand_list", true)
        feature.setup {
            //Source FutureDayWeatherItem
            "com.oplus.weather.main.view.itemview.FutureDayWeatherItem".toClass().resolve().apply {
                firstFieldOrNull { name = "isAllow15DayExpand";type = Boolean::class } ?: return@apply
                constructor {
                    parameters { it.contains(classOf<Boolean>()) }
                }.hookAll {
                    beforeObserved(feature) {
                        arg(args.indexOfFirst { it is Boolean }).set(true)
                    }
                }
            }

            //Source UIConfigManager
            "com.oplus.weather.uiconfig.UIConfigManager".toClass().resolve().apply {
                method {
                    name { it.startsWith("get") && it.contains("Day15ExpandConfig") }
                }.hookAll {
                    interceptObserved(feature, true)
                }
            }
        }
    }
}
