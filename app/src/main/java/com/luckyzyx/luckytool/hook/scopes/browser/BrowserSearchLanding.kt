package com.luckyzyx.luckytool.hook.scopes.browser

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** 40.10.21.9.6: SearchInputPart is separate from NativeSugPart (typed suggestions). */
class BrowserSearchLanding : YukiBaseHooker() {
    private val observed = WeakHashMap<View, Boolean>()
    private var reportedFailure = false

    override fun onHook() {
        val manager = "m02.a".toClassOrNull() ?: return
        val history = BrowserSearchLandingPolicy.HISTORY_ITEM.toClassOrNull() ?: return
        // Verify obfuscated history class still owns the named native history holder.
        if (history.declaredFields.none {
                it.type.name == "com.heytap.browser.search.suggest.card.viewholder.SearchHistoryCard"
            }) return
        manager.resolve().firstMethod { name = "e"; parameters(ViewGroup::class) }.hook {
            before {
                try {
                    val root = firstArg().get<ViewGroup>() ?: return@before
                    val layout = BrowserSearchLandingViews.resolve(root) ?: return@before
                    val cards = manager.getDeclaredField("a").apply { isAccessible = true }
                        .get(instance) as? SparseArray<*> ?: return@before
                    val entries = (0 until cards.size()).map { index ->
                        val card = cards.valueAt(index)
                        val owner = card.javaClass.getMethod("f").invoke(card)
                        BrowserSearchLandingPolicy.Card(cards.keyAt(index), card.javaClass.name, owner.javaClass.name)
                    }
                    val removed = BrowserSearchLandingPolicy.removableKeys(entries) ?: return@before
                    removed.forEach(cards::remove)
                    // Filter BEFORE native e initializes holders/LiveData subscriptions.
                    // Keep the native incognito-notice card alongside history.
                    // All subsequent refresh/focus/theme/destroy loops use this same registry.
                    BrowserSearchLandingViews.clean(layout)
                    observe(root)
                } catch (error: Throwable) {
                    if (!reportedFailure) {
                        reportedFailure = true
                        YLog.error("Reborn: search landing mapping unavailable; keeping native page", error)
                    }
                }
            }
        }
    }

    private fun observe(root: ViewGroup) {
        if (observed.put(root, true) != null) return
        val weak = WeakReference(root)
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            weak.get()?.let { view -> BrowserSearchLandingViews.resolve(view)?.let(BrowserSearchLandingViews::clean) }
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                // A detached view can be reused when input is cleared or the page is reopened.
                v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                v.viewTreeObserver.addOnGlobalLayoutListener(listener)
                BrowserSearchLandingViews.resolve(v)?.let(BrowserSearchLandingViews::clean)
            }
            override fun onViewDetachedFromWindow(v: View) {
                if (v.viewTreeObserver.isAlive) v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        })
    }
}
