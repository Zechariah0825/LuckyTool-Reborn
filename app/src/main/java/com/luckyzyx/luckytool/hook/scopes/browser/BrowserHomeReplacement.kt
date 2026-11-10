package com.luckyzyx.luckytool.hook.scopes.browser

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

/**
 * Replaces only MajorContainer (the 首页 content), never the encompassing HomeFrame
 * that also contains the bottom navigation and 我的/视频 pages. Native URL loading
 * uses ownerTab.loadUrl(String); no additional WebView or new Activity is created.
 */
@Obfuscate
class BrowserHomeReplacement(
    private val mode: String,
    private val configuredUrl: String,
    private val shortcuts: String,
) : YukiBaseHooker() {
    private val navigation = BrowserHomeRouteGate()
    private val fragments = WeakHashMap<Any, WeakReference<Any>>()
    private val homeTag = Any()
    private var reportedInstall = false

    override fun onHook() {
        if (mode == RebornBrowserConfig.URL) hookExplicitHomeClick()
        try {
            val fragmentClass = "com.heytap.browser.home.NormalHomePageFragment".toClassOrNull()
                ?: error("NormalHomePageFragment unavailable")
            // Creation returns an empty wrapper. The real, shared HomeFrame is attached
            // later (ci in 40.10.21.9.6, di in 40.10.25.2), and can change again.
            val create = fragmentClass.declaredMethods.single {
                it.parameterTypes.contentEquals(arrayOf(Context::class.java, LayoutInflater::class.java, ViewGroup::class.java)) &&
                    it.returnType == View::class.java
            }
            fragmentClass.resolve().firstMethod { name = create.name }.hook {
                after { bindFragment(instance) }
            }
            val attachName = when (create.name) {
                "xh" -> "ci"
                "yh" -> "di"
                else -> error("Unknown NormalHomePageFragment creation signature: ${create.name}")
            }
            fragmentClass.resolve().firstMethod { name = attachName; parameters() }.hook {
                after { bindFragment(instance) }
            }
            // onActiveEnter: catches activation after wrapper creation and tab restore.
            fragmentClass.resolve().firstMethod {
                name = if (create.name == "xh") "rh" else "sh"; parameters()
            }.hook { after { bindFragment(instance) } }
            fragmentClass.resolve().firstMethod { name = "onHomeChanged"; parameterCount = 2 }.hook {
                after { bindFragment(instance) }
            }
            hookReplacementLayout()
            YLog.info("Reborn: homepage lifecycle hook mappings configured (${create.name}/$attachName)")
            val majorClass = "com.heytap.browser.main.home.normal.major.MajorHomePage".toClassOrNull()
                ?: error("MajorHomePage unavailable")
            majorClass.resolve().firstMethod { name = "getView"; parameters() }.hook {
                after {
                    try {
                        val major = instance
                        val home = readFieldOfType(major, "com.heytap.browser.main.home.normal.NormalHome") ?: return@after
                        val fragment = fragments[home]?.get() ?: return@after
                        val container = result as? FrameLayout ?: return@after
                        installHome(major, home, container)
                    } catch (error: Exception) {
                        YLog.error("Reborn: MajorContainer replacement skipped", error)
                    }
                }
            }
        } catch (error: Exception) {
            YLog.error("Reborn: native homepage signatures changed", error)
        }
    }

    private fun bindFragment(fragment: Any) {
        try {
            val home = readFieldOfType(fragment, "com.heytap.browser.main.home.a") ?: return
            if (home.javaClass.name != "com.heytap.browser.main.home.normal.NormalHome") return
            // HomeFrame is shared across native tabs. Do not let creation of a
            // background tab overwrite the foreground fragment's owner mapping.
            val owner = ownerTab(fragment) ?: return
            if (owner.javaClass.getMethod("isActive").invoke(owner) != true) return
            fragments[home] = WeakReference(fragment)
            val major = readFieldOfType(home, "com.heytap.browser.main.home.normal.major.MajorHomePage") ?: return
            val container = major.javaClass.getMethod("getView").invoke(major) as? FrameLayout ?: return
            installHome(major, home, container)
            if (mode == RebornBrowserConfig.URL) queueUrlHome(fragment, home, major, container)
        } catch (error: Exception) {
            YLog.error("Reborn: homepage attachment unavailable; original retained", error)
        }
    }

    private fun hookReplacementLayout() {
        val containerClass = "com.heytap.browser.main.home.normal.major.MajorContainer".toClassOrNull()
            ?: error("MajorContainer unavailable")
        // MajorContainer delegates measurement/layout to MajorLayoutHelper, whose
        // scroll offsets are intended for news. Explicitly anchor our own child.
        containerClass.resolve().firstMethod { name = "onMeasure"; parameters(Int::class, Int::class) }.hook {
            after {
                val root = instance as FrameLayout
                replacement(root)?.measure(
                    View.MeasureSpec.makeMeasureSpec(root.measuredWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(root.measuredHeight, View.MeasureSpec.EXACTLY),
                )
            }
        }
        containerClass.resolve().firstMethod {
            name = "onLayout"; parameters(Boolean::class, Int::class, Int::class, Int::class, Int::class)
        }.hook {
            after {
                val root = instance as FrameLayout
                replacement(root)?.layout(0, 0, root.width, root.height)
            }
        }
        containerClass.resolve().firstMethod { name = "onInterceptTouchEvent"; parameterCount = 1 }.hook {
            before { if (replacement(instance as FrameLayout) != null) result = false }
        }
    }

    private fun replacement(root: FrameLayout): View? =
        (0 until root.childCount).map(root::getChildAt).firstOrNull { it.tag === homeTag }

    private fun hookExplicitHomeClick() {
        try {
            // Both supplied versions: BrowserHomeToolbarController.onToolBarHomeButtonClicked.
            val listener = "com.heytap.browser.home.g".toClassOrNull() ?: return
            val toolbar = "com.heytap.browser.root.ToolBar".toClassOrNull() ?: return
            val button = "com.heytap.browser.widget.ToolBarRippleButton".toClassOrNull() ?: return
            listener.resolve().firstMethod {
                name = "b"
                parameters(toolbar, button)
                returnType = Void.TYPE
            }.hook {
                before {
                    try {
                        val home = readFieldOfType(instance, "com.heytap.browser.main.home.normal.NormalHome") ?: return@before
                        val fragment = fragments[home]?.get() ?: return@before
                        val owner = ownerTab(fragment) ?: return@before
                        if (owner.javaClass.getMethod("getCurrent").invoke(owner) === fragment) {
                            navigation.rearm(fragment)
                        }
                    } catch (error: Exception) {
                        YLog.error("Reborn: explicit Home click rearm skipped", error)
                    }
                }
                after {
                    try {
                        val home = readFieldOfType(instance, "com.heytap.browser.main.home.normal.NormalHome") ?: return@after
                        val fragment = fragments[home]?.get() ?: return@after
                        val major = readFieldOfType(home, "com.heytap.browser.main.home.normal.major.MajorHomePage") ?: return@after
                        val view = major.javaClass.getMethod("getView").invoke(major) as? View ?: return@after
                        // The queued callback rechecks both selected subpage and owning tab.
                        // If the native switch is asynchronous, its layout callback retries.
                        queueUrlHome(fragment, home, major, view)
                    } catch (error: Exception) {
                        YLog.error("Reborn: explicit Home click navigation skipped", error)
                    }
                }
            }
        } catch (error: Exception) {
            YLog.error("Reborn: explicit Home click signature unavailable", error)
        }
    }

    private fun installHome(major: Any, home: Any, container: FrameLayout) {
        if ((0 until container.childCount).any { container.getChildAt(it).tag === homeTag }) return
        val replacement = buildHome(container.context, home).apply { tag = homeTag }
        // Build first, retain every original child and all lifecycle references.
        container.addView(replacement, FrameLayout.LayoutParams(-1, -1))
        if (!reportedInstall) {
            reportedInstall = true
            YLog.info("Reborn: custom homepage installed in MajorContainer (mode=$mode)")
        }
        val weakContainer = WeakReference(container)
        val weakHome = WeakReference(home)
        val weakMajor = WeakReference(major)
        val refresh = {
            val root = weakContainer.get()
            if (root != null && replacement.parent === root) {
                for (index in 0 until root.childCount) {
                    val child = root.getChildAt(index)
                    if (child.tag !== homeTag && child.visibility == View.VISIBLE) child.visibility = View.INVISIBLE
                }
                if (root.childCount > 0 && root.getChildAt(root.childCount - 1) !== replacement) replacement.bringToFront()
                val activeHome = weakHome.get()
                val host = activeHome?.let { fragments[it]?.get() }
                val page = weakMajor.get()
                if (mode == RebornBrowserConfig.URL && host != null && activeHome != null && page != null) {
                    val current = runCatching {
                        readFieldOfType(activeHome, "com.heytap.browser.main.home.normal.a")
                    }.getOrNull()
                    if (current === page) queueUrlHome(host, activeHome, page, root)
                }
            }
        }
        val listener = ViewTreeObserver.OnGlobalLayoutListener { refresh() }
        var observed: ViewTreeObserver? = null
        fun register(view: View) {
            val observer = view.viewTreeObserver
            if (observed === observer) return
            observed?.takeIf { it.isAlive }?.removeOnGlobalLayoutListener(listener)
            observer.addOnGlobalLayoutListener(listener)
            observed = observer
        }
        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                register(view)
                view.post { refresh() }
            }
            override fun onViewDetachedFromWindow(view: View) {
                observed?.takeIf { it.isAlive }?.removeOnGlobalLayoutListener(listener)
                observed = null
            }
        })
        if (container.isAttachedToWindow) register(container)
        refresh()
        container.post { refresh() }
    }

    private fun readFieldOfType(owner: Any, type: String): Any? {
        var cls: Class<*>? = owner.javaClass
        while (cls != null) {
            val field = cls.declaredFields.singleOrNull { it.type.name == type }
            if (field != null) {
                field.isAccessible = true
                return field.get(owner)
            }
            cls = cls.superclass
        }
        return null
    }

    private fun buildHome(context: Context, home: Any): View = when (mode) {
        RebornBrowserConfig.URL -> buildUrlHome(context, home)
        RebornBrowserConfig.SHORTCUTS -> buildShortcutHome(context, home)
        else -> FrameLayout(context).apply { setBackgroundColor(background(context)) }
    }

    private fun buildUrlHome(context: Context, home: Any): View {
        val url = RebornBrowserConfig.validHttpUrl(configuredUrl)
            ?: return message(context, "主页网址无效，请在 LuckyTool 中重新设置")
        // Returning from the native webpage leaves a safe explicit action, not an
        // automatic redirect loop. The first activation is opened automatically.
        return FrameLayout(context).apply {
            setBackgroundColor(background(context))
            addView(Button(context).apply {
                text = "打开自定义主页"
                isAllCaps = false
                setOnClickListener { fragments[home]?.get()?.let { openNativeTab(it, url) } }
            }, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        }
    }

    private fun queueUrlHome(fragment: Any, home: Any, major: Any, view: View) {
        val url = RebornBrowserConfig.validHttpUrl(configuredUrl) ?: return
        view.post {
            try {
                if (fragments[home]?.get() !== fragment) return@post
                if (readFieldOfType(home, "com.heytap.browser.main.home.normal.a") !== major) return@post
                val owner = ownerTab(fragment) ?: return@post
                // Do not hijack another tab/page when an old lifecycle callback runs.
                if (owner.javaClass.getMethod("getCurrent").invoke(owner) !== fragment) return@post
                navigation.openOnce(fragment) { openNativeTab(fragment, url) }
            } catch (error: Exception) {
                YLog.error("Reborn: native homepage activation skipped", error)
            }
        }
    }

    private fun buildShortcutHome(context: Context, home: Any): View {
        val fg = foreground(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(dp(context, 20), dp(context, 52), dp(context, 20), dp(context, 24))
            setBackgroundColor(background(context))
        }
        container.addView(TextView(context).apply {
            text = "我的主页"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(fg)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(context, 64)))
        val grid = GridLayout(context).apply { columnCount = 3; alignmentMode = GridLayout.ALIGN_BOUNDS }
        val links = RebornBrowserConfig.parseShortcuts(shortcuts)
        if (links.isEmpty()) {
            container.addView(TextView(context).apply {
                text = "在 LuckyTool → 浏览器中添加快捷链接\n每行填写：名称 | https://网址"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(fg)
            }, LinearLayout.LayoutParams(-1, dp(context, 120)))
        } else {
            links.forEach { link ->
                grid.addView(Button(context).apply {
                    text = link.title
                    isAllCaps = false
                    setOnClickListener { fragments[home]?.get()?.let { openNativeTab(it, link.url) } }
                }, GridLayout.LayoutParams().apply {
                    width = (context.resources.displayMetrics.widthPixels - dp(context, 56)) / 3
                    height = dp(context, 68)
                    setMargins(dp(context, 2), dp(context, 5), dp(context, 2), dp(context, 5))
                })
            }
            container.addView(grid)
        }
        return ScrollView(context).apply {
            isFillViewport = true
            addView(container, FrameLayout.LayoutParams(-1, -2))
        }
    }

    private fun message(context: Context, msg: String) = TextView(context).apply {
        text = msg
        textSize = 17f
        gravity = Gravity.CENTER
        setTextColor(foreground(context))
        setBackgroundColor(background(context))
    }

    private fun ownerTab(fragment: Any): Any? =
        readFieldOfType(fragment, "com.heytap.browser.api.tab.x")

    private fun openNativeTab(fragment: Any, url: String): Boolean {
        val safe = RebornBrowserConfig.validHttpUrl(url) ?: return false
        return try {
            val owner = ownerTab(fragment) ?: return false
            if (owner.javaClass.getMethod("isActive").invoke(owner) != true) return false
            if (owner.javaClass.getMethod("getCurrent").invoke(owner) !== fragment) return false
            owner.javaClass.getMethod("loadUrl", String::class.java).invoke(owner, safe)
            true
        } catch (error: Exception) {
            YLog.error("Reborn: cannot load URL in native owner tab", error)
            false
        }
    }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density + .5f).toInt()
    private fun background(context: Context): Int =
        if (dark(context)) Color.BLACK else Color.WHITE
    private fun foreground(context: Context): Int =
        if (dark(context)) Color.WHITE else Color.BLACK
    private fun dark(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
