package com.luckyzyx.luckytool.hook.scopes.browser

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.RelativeLayout
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Scoped to module-created engines; never changes keys, native selection or search URLs. */
class BrowserSearchIcons(private val engines: List<RebornBrowserConfig.SearchEngine>) : YukiBaseHooker() {
    private val definitions = engines.associateBy { "reborn_${it.id}" }

    override fun onHook() {
        val type = "com.heytap.browser.ai.page.multi.ui.channel.PagerTabItemView".toClassOrNull() ?: return
        val engineType = "com.heytap.browser.api.search.engine.c".toClassOrNull() ?: return
        try {
            // Verified native binder: d(engine) binds label, f(engine) binds theme icon URL.
            type.resolve().firstMethod { name = "d"; parameters(engineType) }.hook { before {
                val engine = firstArg().get<Any>() ?: return@before
                val view = instance as? RelativeLayout ?: return@before
                unbind(view) // Also invalidate callbacks when recycled to a native engine.
                if (definition(engine) == null) return@before
                runCatching {
                    type.getMethod("setShouldDisplayTabIcon", Boolean::class.javaPrimitiveType).invoke(view, true)
                    val field = type.getDeclaredField("c").apply { isAccessible = true }
                    if (field.get(view) == null) type.getMethod("a", RelativeLayout::class.java).invoke(view, view)
                }.onFailure { YLog.error("Reborn: search icon container unavailable", it) }
            } }
            type.resolve().firstMethod { name = "f"; parameters(engineType) }.hook { after {
                val engine = firstArg().get<Any>() ?: return@after
                val view = instance as? View ?: return@after
                val definition = definition(engine) ?: run { unbind(view); return@after }
                bind(view, definition)
            } }
        } catch (error: Throwable) { YLog.error("Reborn: search icon binder mapping unavailable", error) }
    }

    private fun definition(engine: Any): RebornBrowserConfig.SearchEngine? = runCatching {
        definitions[engine.javaClass.getMethod("getKey").invoke(engine) as? String]
    }.getOrNull()

    companion object {
        private val loader = BrowserSearchIconLoader()
        private val bindings = BrowserSearchIconBindings<View>()
        private val listeners = WeakHashMap<View, View.OnAttachStateChangeListener>()

        fun seedNative(native: Any, engine: RebornBrowserConfig.SearchEngine) {
            val weak = WeakReference(native)
            val setter = runCatching { native.javaClass.getMethod("E", Bitmap::class.java) }.getOrNull() ?: return
            val initial = loader.request(BrowserSearchIconPolicy.origin(engine.urlTemplate), engine.label) { icon ->
                weak.get()?.let { target -> runCatching { setter.invoke(target, icon.bitmap) } }
            }
            runCatching { setter.invoke(native, initial.bitmap) }
        }

        private fun unbind(view: View) {
            bindings.remove(view)
            listeners.remove(view)?.let(view::removeOnAttachStateChangeListener)
        }

        private fun bind(view: View, engine: RebornBrowserConfig.SearchEngine) {
            unbind(view)
            val binding = bindings.bind(view, BrowserSearchIconPolicy.origin(engine.urlTemplate), engine.label, "reborn_${engine.id}")
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    if (bindings.current(v, binding)) request(v, binding)
                }
                override fun onViewDetachedFromWindow(v: View) {
                    // No UI callbacks run while detached. Reattach requests cached data.
                }
            }
            listeners[view] = listener
            view.addOnAttachStateChangeListener(listener)
            request(view, binding)
        }

        private fun request(view: View, binding: BrowserSearchIconBindings.Token) {
            val weak = WeakReference(view)
            val initial = loader.request(binding.origin, binding.label) { icon ->
                weak.get()?.let { target ->
                    if (bindings.mayApply(target, binding, target.isAttachedToWindow, boundKey(target))) apply(target, binding, icon)
                }
            }
            apply(view, binding, initial) // Immediate fallback is safe before first attachment.
        }

        private fun boundKey(view: View): String? = runCatching {
            val model = view.javaClass.getDeclaredField("e").apply { isAccessible = true }.get(view)
            model?.javaClass?.getMethod("getKey")?.invoke(model) as? String
        }.getOrNull()

        private fun apply(view: View, binding: BrowserSearchIconBindings.Token, icon: BrowserSearchIconLoader.Icon) {
            if (!bindings.current(view, binding)) return
            runCatching {
                val model = view.javaClass.getDeclaredField("e").apply { isAccessible = true }.get(view) ?: return
                val key = model.javaClass.getMethod("getKey").invoke(model) as? String
                if (key != binding.key) return
                val image = view.javaClass.getDeclaredField("c").apply { isAccessible = true }.get(view) as? View ?: return
                // Only a bounded in-memory PNG data URI reaches the native loader.
                // It handles controller cancellation, theme and layout normally.
                image.javaClass.getMethod("setImageURI", Uri::class.java, Any::class.java)
                    .invoke(image, Uri.parse(icon.uri), null)
                image.visibility = View.VISIBLE
            }
        }
    }
}
