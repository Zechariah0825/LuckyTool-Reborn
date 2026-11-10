package com.luckyzyx.luckytool.hook.scopes.cloudservice

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableForcedBackupAppList : YukiBaseHooker() {
    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("disable_forced_backup_app_list", true)
        feature.setup {
            val backupRestoreOptUiStyle =
                "com.heytap.cloud.backuprestore.bswitch.BackupRestoreOptUiStyle"

            val uiStyleEnum = backupRestoreOptUiStyle.toClassOrNull() ?: run {
                feature.failed(ClassNotFoundException())
                YLog.debug("DisableForcedBackupAppList clazz is null!")
                return
            }
            if (!uiStyleEnum.isEnum) {
                feature.failed(IllegalStateException())
                YLog.debug("DisableForcedBackupAppList enum is error!")
                return
            }
            val switchStyle = uiStyleEnum.enumConstants?.find { it.toString() == "STYLE_SWITCH" }
                ?: run {
                    feature.failed(NoSuchFieldException())
                    return
                }

            //Source BackupRestoreOpt
            "com.heytap.cloud.backuprestore.bswitch.BackupRestoreOpt".toClass().resolve().apply {
                firstMethodOrNull { name = "getForceSelect" }?.hook {
                    interceptObserved(feature, false)
                }
            }

            //Source BackupRestoreOptUiData
            "com.heytap.cloud.backuprestore.bswitch.bean.BackupRestoreOptUiData".toClass().resolve()
                .apply {
                    firstMethod { name = "getOptStyle" }.hook {
                        before {
                            val optId = feature.setup {
                                firstField { name = "optId" }.of(instance).get<String>()
                            }
                            if (optId == "backup_switch_key_third_app") {
                                feature.callback {
                                    val style = switchStyle.asResolver().firstMethod { name = "getStyle" }
                                        .invoke()
                                    result = style
                                }
                            }
                        }
                    }
                }
        }
    }
}
