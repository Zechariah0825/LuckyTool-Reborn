package com.luckyzyx.luckytool.hook.scopes.market

import android.view.View
import android.view.ViewTreeObserver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

/**
 * OPPO Market captured build:  SubTabCardDto / SubTabCardGlobalDto expose
 * getSubTabDtoList(), entries expose getTitle(), model SubTabDto exposes getName().
 * Filter the DTO before the tabs/pager are built. Preserve every other tab and order.
 */
@Obfuscate
class MarketHomeTabs : YukiBaseHooker() {
    override fun onHook() {
        listOf(
            "com.heytap.cdo.card.domain.dto.SubTabCardDto",
            "com.heytap.cdo.card.global.dto.SubTabCardGlobalDto",
            "com.heytap.card.main.api.model.TabDto",
        ).forEach { className ->
            val klass = className.toClassOrNull() ?: return@forEach
            try {
                klass.resolve().firstMethod { name = "getSubTabDtoList" }.hook {
                    after {
                        val original = result as? List<*> ?: return@after
                        stripFeatured(original)?.let { result = it }
                    }
                }
                // Strip before the DTO is stored as well, so pager and tab labels
                // cannot later diverge by reading the unfiltered backing field.
                klass.resolve().firstMethod { name = "setSubTabDtoList" }.hook {
                    before {
                        val original = firstArg().get<List<*>>() ?: return@before
                        stripFeatured(original)?.let { firstArg().set(it) }
                    }
                }
            } catch (error: Throwable) {
                YLog.error("Reborn: Market tab DTO changed: $className", error)
            }
        }
    }

    private fun stripFeatured(original: List<*>): List<*>? {
        val updated = RebornMarketTabRules.removeFeatured(original, ::tabName) ?: return null
        updated.firstOrNull { tabName(it) == "应用时刻" }?.let { markDefaultSelected(it) }
        // Preserve mutable ArrayList expected by legacy TabDto setters.
        return ArrayList(updated)
    }

    private fun tabName(item: Any?): String? {
        if (item == null) return null
        return try {
            val method = item.javaClass.methods.firstOrNull {
                it.parameterCount == 0 && it.name in setOf("getTitle", "getName")
            } ?: return null
            method.invoke(item)?.toString()?.trim()
        } catch (_: Throwable) { null }
    }

    private fun markDefaultSelected(item: Any) {
        try {
            item.javaClass.methods.firstOrNull {
                it.name == "setDefaultSelected" && it.parameterCount == 1 &&
                    it.parameterTypes[0] == Int::class.javaPrimitiveType
            }?.invoke(item, 1)
        } catch (_: Throwable) { /* View-level fallback still runs */ }
    }
}

/** Ensures the first launch is the first remaining tab, without overriding user tab changes.
 * Invoked after subtab construction; never removes a visual tab unless its text matches.
 */
@Obfuscate
class MarketHomeTabSelection : YukiBaseHooker() {
    // The tab view is attached before the server-provided titles are populated.
    // Keep watching layout until the requested tab exists, rather than posting once.
    private val completed = java.util.WeakHashMap<View, Boolean>()
    private val watching = java.util.WeakHashMap<View, ViewTreeObserver.OnGlobalLayoutListener>()

    override fun onHook() {
        val cls = "com.heytap.cdo.client.cards.page.main.home.tab.view.ScrollIdleTabLayout"
            .toClassOrNull() ?: return
        try {
            cls.resolve().firstMethod { name = "onAttachedToWindow" }.hook {
                after {
                    val view = instance as? View ?: return@after
                    if (completed[view] == true || watching.containsKey(view)) return@after
                    var attempts = 0
                    val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            attempts++
                            if (selectAppMoment(view) || attempts >= 30) {
                                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                watching.remove(view)
                                completed[view] = true
                            }
                        }
                    }
                    watching[view] = listener
                    view.viewTreeObserver.addOnGlobalLayoutListener(listener)
                    view.post {
                        if (completed[view] == true) return@post
                        if (selectAppMoment(view)) {
                            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                            watching.remove(view)
                            completed[view] = true
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            YLog.error("Reborn: Market home tab layout changed", error)
        }
    }

    private fun selectAppMoment(view: View): Boolean {
        try {
            val cls = view.javaClass
            val getCount = cls.getMethod("getTabCount")
            val getAt = cls.getMethod("getTabAt", Int::class.javaPrimitiveType)
            val count = getCount.invoke(view) as? Int ?: return false
            if (count < 2) return false
            var chosen: Any? = null
            for (i in 0 until count) {
                val tab = getAt.invoke(view, i) ?: continue
                val label = tab.javaClass.methods.firstOrNull {
                    it.name == "getText" && it.parameterCount == 0
                }?.invoke(tab)?.toString()?.trim()
                if (label == "应用时刻") chosen = tab
            }
            if (chosen != null) {
                // selectTab is a native TabLayout API and updates the attached ViewPager.
                chosen.javaClass.methods.firstOrNull {
                    it.name == "select" && it.parameterCount == 0
                }?.invoke(chosen)
                return true
            }
        } catch (_: Throwable) {
            // Early lifecycle may not have tab methods yet; retry on the next layout.
        }
        return false
    }
}
