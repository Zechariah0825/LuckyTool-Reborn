package com.luckyzyx.luckytool.hook.scopes.camera

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import java.util.concurrent.ExecutorService

@Obfuscate
class EnableCameraDebugUIOption(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("enable_camera_debug_ui_option", true)
        feature.setup {
            //Source ConfigSetUtils
            dexKitBridge.findMethod {
                matcher {
                    declaredClass {
                        addFieldForType(classOf<ExecutorService>())
                        addMethod {
                            paramCount(0)
                            returnType(classOf<Boolean>())
                        }
                    }
                    paramCount(0)
                    returnType(classOf<Boolean>())
                    usingStrings("iq_config_set", "hal_config_set")
                }
            }.apply {
                checkDataList("EnableCameraDebugUIOption ConfigSet")
                single().className.toClass().resolve().apply {
                    firstMethod {
                        name = single().methodName
                        emptyParameters()
                        returnType = Boolean::class
                    }.hook {
                        interceptObserved(feature, true)
                    }
                }
            }

            //Source NetworkAuthenticationUtils
            dexKitBridge.findMethod {
                matcher {
                    declaredClass {
                        addFieldForType(classOf<Context>())
                        addFieldForType(classOf<LongArray>())
                        addMethod {
                            paramTypes(classOf<Long>())
                            returnType(classOf<Boolean>())
                        }
                        addMethod {
                            paramTypes(classOf<String>())
                            returnType(classOf<Boolean>())
                        }
                    }
                    paramTypes(classOf<Long>())
                    returnType(classOf<Boolean>())
                    usingNumbers(3600000)
                    usingStrings("NetworkAuthenticationUtils")
                }
            }.apply {
                checkDataList("EnableCameraDebugUIOption NetworkAuthentication")
                single().className.toClass().resolve().apply {
                    firstMethod {
                        name = single().methodName
                        parameters(Long::class)
                        returnType = Boolean::class
                    }.hook {
                        interceptObserved(feature, false)
                    }
                }
            }

            //Source CameraDebugActivity
            "com.oplus.camera.setting.CameraDebugActivity".toClass().resolve().apply {
                firstMethod { name = "onCreate" }.hook {
                    beforeObserved(feature) {
                        val activity = instance<Activity>()
                        val sp = activity.getSharedPreferences("${activity.packageName}_preferences", Context.MODE_PRIVATE)
                        sp.edit(commit = true) { putBoolean("key_has_checked_auth_connection", true) }
                    }
                }
            }
        }
    }
}
