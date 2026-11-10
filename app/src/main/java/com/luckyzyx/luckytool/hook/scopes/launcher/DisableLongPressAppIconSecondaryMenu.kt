package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.beforeObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableLongPressAppIconSecondaryMenu : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("disable_long_press_app_icon_secondary_menu", true)
        feature.setup {
            //Source OplusPopupContainerWithArrow -> PopupDataProvider
            "com.android.launcher3.popup.PopupDataProvider".toClass().resolve().apply {
                firstMethod {
                    name = "getNotificationKeysForItem"
                    returnType = List::class
                }.hook {
                    beforeObserved(feature) {
                        val itemInfo = firstArg().get() ?: return@beforeObserved
                        itemInfo.asResolver().firstField {
                            name = "mAddShortcutCount"; superclass()
                        }.set(0)
                    }
                }
            }
        }
    }
}
