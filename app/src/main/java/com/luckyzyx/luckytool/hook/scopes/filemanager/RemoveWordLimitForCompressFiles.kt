package com.luckyzyx.luckytool.hook.scopes.filemanager

import android.text.InputFilter
import android.widget.EditText
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveWordLimitForCompressFiles(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        // Loaded only when this exact switch is enabled.
        val feature = ObservedFeature("remove_word_limit_for_compress_files", true)
        feature.setup {
            //Source CompressConfirmDialog
            dexKitBridge.findClass {
                matcher {
                    methods {
                        add { name("onTextChanged") }
                        add { paramTypes(classOf<EditText>(), classOf<InputFilter>()) }
                    }
                    usingStrings("CompressConfirmDialog")
                }
            }.apply {
                checkDataList("CompressConfirmDialog")

                findMethod {
                    matcher {
                        paramCount(0)
                        returnType(classOf<Int>())
                        usingNumbers(50)
                    }
                }.apply {
                    checkDataList("MaxCount")

                    single().className.toClass().resolve().apply {
                        firstMethod {
                            name = single().methodName
                            emptyParameters()
                            returnType = Int::class
                        }.hook {
                            interceptObserved(feature, 9999)
                        }
                    }
                }
            }
        }
    }
}
