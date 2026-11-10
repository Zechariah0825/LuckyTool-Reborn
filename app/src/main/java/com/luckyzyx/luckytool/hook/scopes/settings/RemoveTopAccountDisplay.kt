package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.diagnostics.ObservedFeature
import com.luckyzyx.luckytool.diagnostics.interceptObserved
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveTopAccountDisplay : YukiBaseHooker() {
    override fun onHook() {
        val feature = ObservedFeature("remove_top_account_display", true)
        feature.setup {
            //Source UserPreferenceController
            "com.oplus.settings.feature.homepage.user.UserPreferenceController".toClass().resolve()
                .apply {
                    firstMethod {
                        name = if (SDK >= A13) "checkAvailable"
                        else "getAvailabilityStatus"
                    }.hook {
                        if (SDK >= A13) {
                            interceptObserved(feature, false)
                        } else {
                            interceptObserved(feature, 3)
                        }
                    }
                }
        }
    }
}
