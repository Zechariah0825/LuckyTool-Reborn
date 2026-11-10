package com.luckyzyx.luckytool.hook.scopes.games

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveToolRecommendationCard : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_tool_recommendation_card", true)
        feature.setup {
            //Source ToolsRecommendCardLayout
            ("business.module.toolsrecommend.ToolsRecommendCardLayout".toClassOrNull() ?: run {
                feature.failed(ClassNotFoundException("business.module.toolsrecommend.ToolsRecommendCardLayout"))
                return
            }).resolve().apply {
                    firstMethod {
                        parameters(List::class)
                        returnType = Void.TYPE
                    }.hook {
                        beforeObserved(feature) {
                            firstArg().set(ArrayList<Any>())
                        }
                    }
                }

            //Source GameToolTileAdapter V9.0.0+
    //        "business.toolpanel.adapter.GameToolTileAdapter".toClassOrNull()?.apply {
    //            method { name = "onCreateViewHolder" }.hook {
    //                after {
    //                    val parent = firstArg().get<ViewGroup>() ?: return@after
    //                    val id = lastArg().get<Int>() ?: 0
    //                    if (id == 10005) result<ViewHolder>()?.itemView?.isVisible = false
    //                }
    //            }
    //        }
        }
    }
}
