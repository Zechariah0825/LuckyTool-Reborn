package com.luckyzyx.luckytool.hook.scopes.camera

import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import android.text.Spanned
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveWatermarkWordLimit(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_watermark_word_limit", true)
        feature.setup {
            //Source CameraSubSettingFragment -> camera_namelength_outofrange -> filter
            //Source CameraSloganSettingFragment -> camera_namelength_outofrange -> filter
            dexKitBridge.findMethod {
                matcher {
                    name("filter")
                    paramTypes(
                        classOf<CharSequence>(), classOf<Int>(), classOf<Int>(),
                        classOf<Spanned>(), classOf<Int>(), classOf<Int>()
                    )
                    returnType(classOf<CharSequence>())
                    usingStrings("")
                    addInvoke {
                        paramCount(2..3)
                        returnType(Void.TYPE)
                    }
                }
            }.apply {
                val onlyOne = SDK >= A13
                checkDataList("RemoveWatermarkWordLimit", onlyOne)
                if (onlyOne.not() && size == 2) {
                    forEach {
                        it.className.toClass().resolve().apply {
                            firstMethod {
                                name = "filter"
                                parameters(
                                    CharSequence::class, Int::class, Int::class,
                                    Spanned::class, Int::class, Int::class
                                )
                                returnType = CharSequence::class
                            }.hook {
                                beforeObserved(feature) {
                                    result = firstArg().get<CharSequence>() ?: return@beforeObserved
                                }
                            }
                        }
                    }
                } else {
                    single().className.toClass().resolve().apply {
                        firstMethod {
                            name = "filter"
                            parameters(
                                CharSequence::class, Int::class, Int::class,
                                Spanned::class, Int::class, Int::class
                            )
                            returnType = CharSequence::class
                        }.hook {
                            beforeObserved(feature) {
                                result = firstArg().get<CharSequence>() ?: return@beforeObserved
                            }
                        }
                    }
                }
            }
        }
    }
}
