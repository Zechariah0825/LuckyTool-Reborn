package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import android.app.ActivityManager.RunningTaskInfo
import android.content.Intent
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.startMirageWindow
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RunFloatingWindowTasksInForeground : YukiBaseHooker() {

    private const val TAG = "MirageHangUp"

    override fun onHook() {
        val feature = ObservedFeature("run_floating_window_tasks_in_foreground", true)
        feature.setup {
            var flag = -1
            var status: Boolean

            //Source ZoomStateManager
            "com.oplus.zoom.zoomstate.ZoomStateManager".toClass().resolve().apply {
                firstMethod {
                    name = "requestChangeZoomTask"
                    parameters(Int::class, Boolean::class)
                }.hook {
                    beforeObserved(feature) {
                        flag = firstArg().get<Int>() ?: 0
                        status = lastArg().get<Boolean>() ?: false

                        //浮窗全屏 flag 4
                        //浮窗贴边 flag 5
                        //浮窗退出 flag 6

                        if (flag == 5 && status) {
                            val mTaskInfo = firstField { name = "mTaskInfo" }.of(instance)
                                .get<RunningTaskInfo>() ?: return@beforeObserved

                            val baseIntent = mTaskInfo.asResolver().firstField {
                                type = Intent::class;superclass()
                            }.get<Intent>() ?: return@beforeObserved

                            val uid = mTaskInfo.asResolver().firstField { name = "uid";superclass() }
                                .get<Int>() ?: return@beforeObserved
                            if (uid > 0) baseIntent.putExtra("TASKINFO_UID", uid)

                            val displayId = startMirageWindow(baseIntent)
                            LogUtils.d(TAG, "requestChangeZoomTask", "systemui hang up uid=$uid displayId=$displayId")
                            result = null
                        }
                    }
                }
            }

            //Source FloatHandleController
            "com.oplus.zoom.ui.floathandle.FloatHandleController".toClass().resolve().apply {
                firstMethod { name = "onTaskMovedToFront" }.hook {
                    beforeObserved(feature) {
                        if (flag != 5) return@beforeObserved
                        LogUtils.d(TAG, "onTaskMovedToFront", "systemui block onTaskMovedToFront")
                        result = null
                    }
                }
            }
        }
    }
}
