package com.luckyzyx.luckytool.hook.scopes.launcher

import android.content.Intent
import android.view.View
import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object LongPressAppIconOpenAppDetails : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("long_press_app_icon_open_app_details", true)
        feature.setup {
            //Source OplusTaskHeaderView
            "com.android.quickstep.views.OplusTaskViewImpl".toClass().resolve().apply {
                firstMethod {
                    name = "setIcon"
                    parameterCount { it in 1..2 }
                }.hook {
                    after {
                        // Listener preparation is not a user action.
                        feature.setup {
                            val headerView = firstMethod { name = "getHeaderView" }.of(instance).invoke()
                                ?: return@after
                            val iconView = headerView.asResolver().firstMethod { name = "getTaskIcon" }
                                .invoke<View>() ?: return@after
                            val titleView = headerView.asResolver().firstField {
                                name = if (SDK >= A13) "titleTv" else "mTitleView"
                            }.get<TextView>() ?: return@after
                            val task = firstMethod { name = "getTask"; superclass() }
                                .of(instance).invoke() ?: return@after
                            val key = task.asResolver().firstField { name = "key" }.get() ?: return@after
                            val intent = key.asResolver().firstField { type = Intent::class }.get<Intent>()
                                ?: return@after
                            val packName = intent.component?.packageName ?: intent.`package`
                            val userId =
                                task.asResolver().firstMethodOrNull { name = "getUserId" }?.invoke<Int>()
                                    ?: key.asResolver().firstField { name = "userId" }.get<Int>()
                            iconView.setLongClick(packName, userId, feature)
                            titleView.setLongClick(packName, userId, feature)
                        }
                    }
                }
            }

            //Source DockIconView
            "com.oplus.quickstep.dock.DockIconView".toClass().resolve().apply {
                firstMethod {
                    name = "setIcon"
                    parameterCount = 1
                }.hook {
                    after {
                        // Listener preparation is not a user action.
                        feature.setup {
                            val task =
                                firstMethod { name = "getTask" }.of(instance).invoke() ?: return@after
                            val key = task.asResolver().firstField { name = "key" }.get() ?: return@after
                            val intent = key.asResolver().firstField { type = Intent::class }.get<Intent>()
                                ?: return@after
                            val packName = intent.component?.packageName ?: intent.`package`
                            val userId =
                                task.asResolver().firstMethodOrNull { name = "getUserId" }?.invoke<Int>()
                                    ?: key.asResolver().firstField { name = "userId" }.get<Int>()
                            instance<View>().setLongClick(packName, userId, feature)
                        }
                    }
                }
            }
        }
    }

    private fun View.setLongClick(packName: String?, userId: Int? = 0, feature: ObservedFeature) {
        setOnLongClickListener {
            // Count the actual long-press action, not listener installation.
            packName?.let { its ->
                feature.callback { AppUtils(it.context).openAppDetailIntent(its, userId) }
            }
            true
        }
    }
}
