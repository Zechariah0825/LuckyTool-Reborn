package com.luckyzyx.luckytool.hook.scopes.market

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

/** Search screen only. Does not hook global TextView APIs or homepage search widgets. */
@Obfuscate
class MarketBlankSearchHint : YukiBaseHooker() {
    override fun onHook() {
        val bar = "com.heytap.market.search.core.actionbar.SearchActionBar".toClassOrNull() ?: return
        val callback = "com.heytap.market.search.core.actionbar.SearchActionBar\$h".toClassOrNull() ?: return
        val helper = "a.a.a.jde".toClassOrNull() ?: return
        val recommend = "a.a.a.nhe".toClassOrNull() ?: return

        // The native button-state code consults this tag when both text and hint
        // are empty. Return the native no-recommendation value, not a hidden word.
        helper.resolve().firstMethod {
            name = "\u037f"
            parameters(bar)
        }.hook { before { result = null } }
        bar.resolve().firstMethod {
            name = "setSearchHint"
            parameters(String::class)
        }.hook { before { args[0] = "" } }

        // Rich setters install delayed global-layout callbacks. Stop those before
        // installation, and use the simple native setter to refresh button state.
        for (signature in listOf(
            arrayOf(String::class.java, String::class.java, callback),
            arrayOf(String::class.java, String::class.java, String::class.java, String::class.java, callback),
        )) {
            bar.resolve().firstMethod {
                name = "setSearchHint"
                parameters(*signature)
            }.hook {
                before {
                    MarketSearchRules.clearHint(instance)
                    result = null
                }
            }
        }
        // Stop recommendation fallback/rebinding at the action-bar-specific source.
        helper.resolve().firstMethod {
            name = "\u052b"
            parameters(bar, recommend, String::class)
        }.hook {
            before {
                args[0]?.let(MarketSearchRules::clearHint)
                result = null
            }
        }
        bar.resolve().firstMethod {
            name = "\u0792"
            parameters()
        }.hook {
            after { MarketSearchRules.clearHint(instance) }
        }
    }
}
