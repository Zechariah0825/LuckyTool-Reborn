package com.luckyzyx.luckytool.hook.scopes.market

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.hookAll
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import java.lang.reflect.Modifier
import org.lsposed.lsparanoid.Obfuscate

/** Verified helper layouts, not a global scan for similarly shaped methods. */
internal data class MarketHintLayout(
    val helper: String, val recommendation: String, val getter: String, val binder: String,
) {
    fun matches(bar: Class<*>, helperType: Class<*>, recommendationType: Class<*>): Boolean = try {
        val getterMethod = helperType.getDeclaredMethod(getter, bar)
        val binderMethod = helperType.getDeclaredMethod(binder, bar, recommendationType, String::class.java)
        Modifier.isStatic(getterMethod.modifiers) && Modifier.isPublic(getterMethod.modifiers) &&
            getterMethod.returnType == recommendationType &&
            Modifier.isStatic(binderMethod.modifiers) && Modifier.isPublic(binderMethod.modifiers) &&
            binderMethod.returnType == Void.TYPE
    } catch (_: ReflectiveOperationException) { false }

    companion object {
        val verified = listOf(
            MarketHintLayout("a.a.a.jde", "a.a.a.nhe", "\u037f", "\u052b"), // 26.10.2_CN
            MarketHintLayout("a.a.a.tie", "a.a.a.fne", "\u0528", "\u052d"), // 26.11.3_CN
        )
    }
}

/** Search screen only. Does not hook global TextView APIs or homepage search widgets. */
@Obfuscate
class MarketBlankSearchHint : YukiBaseHooker() {
    override fun onHook() {
        val bar = "com.heytap.market.search.core.actionbar.SearchActionBar".toClassOrNull() ?: return
        val callback = "com.heytap.market.search.core.actionbar.SearchActionBar\$h".toClassOrNull() ?: return

        // These named APIs are stable in both verified APKs. An unknown obfuscated
        // helper must not silently disable their independent display cleanup.
        bar.resolve().firstMethod {
            name = "setSearchHint"
            parameters(String::class)
        }.hook { before { args[0] = "" } }
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
        // Both APKs initialize their EditText before the innermost constructor
        // returns. No dependency on the renamed private init method (0792/0798).
        bar.resolve().constructor {}.hookAll {
            after { MarketSearchRules.clearHint(instance) }
        }

        val matches = MarketHintLayout.verified.mapNotNull { layout ->
            val helper = layout.helper.toClassOrNull() ?: return@mapNotNull null
            val recommendation = layout.recommendation.toClassOrNull() ?: return@mapNotNull null
            if (layout.matches(bar, helper, recommendation)) Triple(layout, helper, recommendation) else null
        }
        val (layout, helper, recommendation) = matches.singleOrNull() ?: run {
            YLog.warn("Reborn: Market recommendation helper schema unsupported or ambiguous; hint display only")
            return
        }
        // The native button-state code consults this tag when both text and hint
        // are empty. Return the native no-recommendation value, not a hidden word.
        helper.resolve().firstMethod {
            name = layout.getter
            parameters(bar)
        }.hook { before { result = null } }
        // Stop fallback/rebinding before recommendation tags or delayed rich-hint
        // listeners are installed. Search text and submit callbacks remain native.
        helper.resolve().firstMethod {
            name = layout.binder
            parameters(bar, recommendation, String::class)
        }.hook {
            before {
                args[0]?.let(MarketSearchRules::clearHint)
                result = null
            }
        }
    }
}
