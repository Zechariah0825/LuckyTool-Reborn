package com.luckyzyx.luckytool.hook.scopes.mms

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveMmsBottomInputBoxMenu : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_mms_bottom_input_box_menu", true)
        feature.setup {
            //Source MenuInfoBaseBean
            "com.opos.smart.mms.interfaces.netmsg.menu.MenuInfoBaseBean".toClass().resolve().apply {
                firstMethod {
                    name = "getMenus"
                    returnType = List::class
                }.hook {
                    interceptObserved(feature, ArrayList<Any>())
                }
            }
        }
    }
}
