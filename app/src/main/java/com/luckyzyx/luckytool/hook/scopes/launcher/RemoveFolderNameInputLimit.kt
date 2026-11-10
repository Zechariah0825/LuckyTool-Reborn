package com.luckyzyx.luckytool.hook.scopes.launcher

import android.text.Editable
import android.text.TextWatcher
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.afterObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveFolderNameInputLimit : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_folder_name_input_limit", true)
        feature.setup {
            //Source OplusFolder
            "com.android.launcher3.folder.OplusFolder".toClass().resolve().apply {
                firstConstructor().hook {
                    afterObserved(feature) {
                        firstField { type = TextWatcher::class }.of(instance).set(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {

                            }

                            override fun beforeTextChanged(
                                s: CharSequence?, start: Int, count: Int, after: Int
                            ) {

                            }

                            override fun onTextChanged(
                                s: CharSequence?, start: Int, before: Int, count: Int
                            ) {

                            }
                        })
                    }
                }
            }
        }
    }
}
