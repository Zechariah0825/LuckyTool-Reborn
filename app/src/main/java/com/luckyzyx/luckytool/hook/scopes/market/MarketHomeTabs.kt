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
                        stripFeatured(original)?.let { filtered ->
                            result = filtered
                            // Deserializers may write fields without the setter. Keep
                            // the backing DTO consistent with its filtered getter.
                            try {
                                instance?.let { owner ->
                                    owner.javaClass.methods.firstOrNull {
                                        it.name == "setSubTabDtoList" && it.parameterCount == 1 &&
                                            it.parameterTypes[0].isInstance(filtered)
                                    }?.invoke(owner, filtered)
                                }
                            } catch (error: Throwable) {
                                YLog.error("Reborn: cannot synchronize Market tab data", error)
                            }
                        }
                    }
                }
            } catch (error: Throwable) {
                YLog.error("Reborn: Market tab getter changed: $className", error)
            }
            try {
                // Strip before the DTO is stored as well, so pager and tab labels
                // cannot later diverge by reading the unfiltered backing field.
                klass.resolve().firstMethod { name = "setSubTabDtoList" }.hook {
                    before {
                        val original = firstArg().get<List<*>>() ?: return@before
                        stripFeatured(original)?.let { firstArg().set(it) }
                    }
                }
            } catch (error: Throwable) {
                YLog.error("Reborn: Market tab setter changed: $className", error)
            }
            // These card DTOs keep the startup pager position separately from the
            // list. Resolve it AFTER filtering; otherwise removal shifts the index.
            if (className != "com.heytap.card.main.api.model.TabDto") {
                try {
                    klass.resolve().firstMethod { name = "getFirstPagePos" }.hook {
                        after {
                            val owner = instance ?: return@after
                            val tabs = try {
                                owner.javaClass.getMethod("getSubTabDtoList").invoke(owner) as? List<*>
                            } catch (_: Throwable) { null } ?: return@after
                            val index = RebornMarketTabRules.appMomentIndex(tabs, ::tabName)
                            if (index >= 0) result = index
                        }
                    }
                } catch (error: Throwable) {
                    YLog.error("Reborn: Market startup position changed: $className", error)
                }
            }
        }
    }

    private fun stripFeatured(original: List<*>): List<*>? {
        val updated = RebornMarketTabRules.removeFeatured(original, ::tabName) ?: return null
        // Exactly one tab may be the default. Leaving a previous flag set can
        // make the native pager choose a different tab depending on iteration order.
        updated.filterNotNull().forEach { markDefaultSelected(it, tabName(it) == "应用时刻") }
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

    private fun markDefaultSelected(item: Any, selected: Boolean) {
        try {
            item.javaClass.methods.firstOrNull {
                it.name == "setDefaultSelected" && it.parameterCount == 1 &&
                    it.parameterTypes[0] == Int::class.javaPrimitiveType
            }?.invoke(item, if (selected) 1 else 0)
        } catch (_: Throwable) { /* View-level fallback still runs */ }
    }
}

/** Selects App Moment once per view, without overriding subsequent user tab changes.
 * Invoked after subtab construction; never removes a visual tab unless its text matches.
 */
@Obfuscate
class MarketHomeTabSelection : YukiBaseHooker() {
    // The tab view is attached before the server-provided titles are populated.
    // Keep watching layout until the requested tab exists, rather than posting once.
    private val completed = java.util.WeakHashMap<View, Boolean>()
    private val watching = java.util.WeakHashMap<View, Boolean>()

    override fun onHook() {
        val cls = "com.heytap.cdo.client.cards.page.main.home.tab.view.ScrollIdleTabLayout"
            .toClassOrNull() ?: return
        // The supplied 26.10.2 APK inherits this method from COUITabLayout.
        // Hook its actual declaring class and gate callbacks to the target subtype.
        val owner = generateSequence(cls as Class<*>?) { it.superclass }.firstOrNull { type ->
            type.declaredMethods.any { it.name == "onAttachedToWindow" && it.parameterCount == 0 }
        } ?: return
        try {
            owner.resolve().firstMethod { name = "onAttachedToWindow" }.hook {
                after {
                    val view = instance as? View ?: return@after
                    if (cls.isInstance(view)) watch(view)
                }
            }
        } catch (error: Throwable) {
            YLog.error("Reborn: Market home tab layout changed", error)
        }
    }

    private fun watch(view: View) {
        if (completed[view] == true || watching[view] == true) return
        watching[view] = true
        val observer = view.viewTreeObserver
        var stopped = false
        lateinit var layout: ViewTreeObserver.OnGlobalLayoutListener
        lateinit var attachment: View.OnAttachStateChangeListener
        lateinit var timeout: Runnable
        fun stop() {
            if (stopped) return
            stopped = true
            if (observer.isAlive) observer.removeOnGlobalLayoutListener(layout)
            view.removeOnAttachStateChangeListener(attachment)
            view.removeCallbacks(timeout)
            watching.remove(view)
        }
        fun attempt() {
            if (!stopped && selectAppMoment(view)) {
                completed[view] = true
                stop()
            }
        }
        layout = ViewTreeObserver.OnGlobalLayoutListener { attempt() }
        attachment = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) = stop()
        }
        timeout = Runnable { stop() }
        observer.addOnGlobalLayoutListener(layout)
        view.addOnAttachStateChangeListener(attachment)
        // A frame-count cap expired before slow server responses. Allow a bounded
        // time window, and always unregister when the view detaches.
        view.postDelayed(timeout, 15_000L)
        view.post { attempt() }
    }

    private fun selectAppMoment(view: View): Boolean {
        try {
            val cls = view.javaClass
            val getCount = cls.getMethod("getTabCount")
            val getAt = cls.getMethod("getTabAt", Int::class.javaPrimitiveType)
            val count = getCount.invoke(view) as? Int ?: return false
            if (count < 1) return false
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
                val select = chosen.javaClass.methods.firstOrNull {
                    it.name == "select" && it.parameterCount == 0
                } ?: return false
                select.invoke(chosen)
                return true
            }
        } catch (_: Throwable) {
            // Early lifecycle may not have tab methods yet; retry on the next layout.
        }
        return false
    }
}
