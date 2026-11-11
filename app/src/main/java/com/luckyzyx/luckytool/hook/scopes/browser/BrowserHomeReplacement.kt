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
    private val boundary = BrowserHomeBoundary()
    private val dispatching = ThreadLocal<BrowserHomeBoundary.Request?>()
    private val loading = ThreadLocal.withInitial { mutableListOf<BrowserHomeBoundary.Request?>() }
    private val fragments = WeakHashMap<Any, WeakReference<Any>>()
    private val explicitWebHome = ThreadLocal.withInitial { 0 }
    private val pendingRecovery = WeakHashMap<Any, Long>()
    private val subpageChanges = WeakHashMap<Any, Boolean>()
    private val homeTag = Any()
    private var reportedInstall = false

    override fun onHook() {
        try {
            val fragmentClass = "com.heytap.browser.home.NormalHomePageFragment".toClassOrNull()
                ?: error("NormalHomePageFragment unavailable")
            // Creation returns an empty wrapper. The real, shared HomeFrame is attached
            // later (ci in 40.10.21.9.6, di in 40.10.25.2), and can change again.
            val create = fragmentClass.declaredMethods.single {
                it.parameterTypes.contentEquals(arrayOf(Context::class.java, LayoutInflater::class.java, ViewGroup::class.java)) &&
                    it.returnType == View::class.java
            }
            val attachName = when (create.name) {
                "xh" -> "ci"
                "yh" -> "di"
                else -> error("Unknown NormalHomePageFragment creation signature: ${create.name}")
            }
            if (mode == RebornBrowserConfig.URL) {
                // Do this before *any* URL lifecycle/replacement hook is installed.
                // g1/g0 and h1/h0 have verified semantics only in these two APKs.
                val manager = (hostApplication ?: systemContext)?.packageManager
                    ?: error("PackageManager unavailable; native homepage retained")
                val info = manager.getPackageInfo(packageName, 0)
                val routes = BrowserHomeNativeRoutes.select(info.versionName, info.longVersionCode, create.name)
                    ?: error("Unverified browser URL routing version ${info.versionName}/${info.longVersionCode}; native homepage retained")
                val tab = "com.heytap.browser.tab.BaseTab".toClassOrNull() ?: error("BaseTab unavailable")
                val page = "com.heytap.browser.page.TabPageFragment".toClassOrNull() ?: error("TabPageFragment unavailable")
                check(routes.matches(tab, page)) { "Browser URL route signature mismatch; native homepage retained" }
                val backRoutes = BrowserHomeBackRoutes.select(info.versionName, info.longVersionCode, create.name)
                    ?: error("Browser Home Back boundary version unverified")
                val params = "com.heytap.browser.platform.load.LoadParams".toClassOrNull() ?: error("LoadParams unavailable")
                val controller = "com.heytap.browser.tab.NavigationController".toClassOrNull() ?: error("NavigationController unavailable")
                check(backRoutes.matches(tab, page, params, controller)) { "Browser Home boundary signature mismatch" }
                hookHomeBoundary(backRoutes, tab, params, controller)
                hookNativeTabRoutes(routes)
                hookExplicitHomeClick()
                hookSubpageSelection()
                hookExplicitWebHomeClicks()
                YLog.info("Reborn: verified URL routing ${info.versionName}: ${routes.returnHome}/${routes.pageChanged}")
            }
            fragmentClass.resolve().firstMethod { name = create.name }.hook {
                after { bindFragment(instance) }
            }
            fragmentClass.resolve().firstMethod { name = attachName; parameters() }.hook {
                after { bindFragment(instance) }
            }
            // onActiveEnter: catches activation after wrapper creation and tab restore.
            fragmentClass.resolve().firstMethod {
                name = if (create.name == "xh") "rh" else "sh"; parameters()
            }.hook { after { bindFragment(instance) } }
            fragmentClass.resolve().firstMethod {
                name = if (create.name == "xh") "sh" else "th"; parameters()
            }.hook { before { navigation.invalidate(instance) } }
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
            val previous = fragments[home]?.get()
            if (previous != null && previous !== fragment) navigation.invalidate(previous)
            fragments[home] = WeakReference(fragment)
            val major = readFieldOfType(home, "com.heytap.browser.main.home.normal.major.MajorHomePage") ?: return
            val container = major.javaClass.getMethod("getView").invoke(major) as? FrameLayout ?: return
            installHome(major, home, container)
            container.requestLayout()
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
                replacement(root)?.takeIf { it.visibility == View.VISIBLE }?.measure(
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
                replacement(root)?.takeIf { it.visibility == View.VISIBLE }?.layout(0, 0, root.width, root.height)
            }
        }
        containerClass.resolve().firstMethod { name = "onInterceptTouchEvent"; parameterCount = 1 }.hook {
            before { if (replacement(instance as FrameLayout)?.visibility == View.VISIBLE) result = false }
        }
    }

    private fun replacement(root: FrameLayout): View? =
        (0 until root.childCount).map(root::getChildAt).firstOrNull { it.tag === homeTag }

    private fun hookExplicitWebHomeClicks() {
        // h1 is also used by TabManager's Back/special-URL routes. Only these
        // verified UI Home callbacks grant a new redirect while h1 executes.
        val listeners = listOf(
            "com.heytap.browser.webdetails.ui.d2" to "h",
            "com.heytap.browser.webdetails.natives.pages.BrowserBaiduToolBarWebListener" to "h",
            "com.heytap.browser.webdetails.natives.pages.NewsBaiduToolBarWebListener" to "h",
            "com.heytap.browser.app.search.web.baidu.SearchBaiduToolBarWebListener" to "h",
            "com.heytap.browser.ai.page.multi.MultiSearchToolBarClickListeners" to "e",
            "com.heytap.browser.ai.page.multi.v2.listener.MultiSearchToolBarClickListenerV2" to "e",
        )
        listeners.forEach { (name, method) ->
            try {
                val listener = name.toClassOrNull() ?: return@forEach
                listener.resolve().firstMethod {
                    this.name = method
                    parameterCount = if (method == "h") 2 else 0
                    returnType = Void.TYPE
                }.hook {
                    before { explicitWebHome.set(explicitWebHome.get() + 1) }
                    after { explicitWebHome.set((explicitWebHome.get() - 1).coerceAtLeast(0)) }
                }
            } catch (error: Exception) {
                YLog.error("Reborn: explicit web Home mapping unavailable: $name", error)
            }
        }
    }

    /**
     * Native L/M (reference M/N) consumes the WebView's own history first.
     * At our first web page only, return the same false as native index zero.
     * The native caller then performs G/H -> onHardBackNotHandle, retaining its
     * own exit prompt/background policy. No synthetic Back, reload or stack edit.
     */
    private fun hookHomeBoundary(
        routes: BrowserHomeBackRoutes, tab: Class<*>, params: Class<*>, controller: Class<*>,
    ) {
        val page = "com.heytap.browser.page.TabPageFragment".toClassOrNull() ?: error("TabPageFragment unavailable")
        fun nativeController(owner: Any): Any? = readFieldOfType(owner, controller.name)
        fun index(owner: Any): Int? = nativeController(owner)?.let {
            controller.getDeclaredMethod("E").invoke(it) as? Int
        }
        tab.resolve().firstMethod {
            name = routes.permissionLoad; parameters(params, Boolean::class.javaPrimitiveType!!); returnType = Void.TYPE
        }.hook {
            before {
                dispatching.get()?.takeIf { it.tab.get() === instance }?.let { request ->
                    args[0]?.let { boundary.track(it, request) }
                }
            }
        }
        tab.resolve().firstMethod {
            name = routes.dispatchLoad; parameters(params, Boolean::class.javaPrimitiveType!!); returnType = Void.TYPE
        }.hook {
            before {
                val scopes = loading.get()
                scopes.add(null)
                val request = args[0]?.let(boundary::request)
                var accepted: BrowserHomeBoundary.Request? = null
                if (request != null) {
                    val home = request.home.get()
                    val valid = request.tab.get() === instance && home != null &&
                        navigation.ownsDispatch(home, request.ticket) &&
                        tab.getMethod("isActive").invoke(instance) == true &&
                        tab.getMethod("getCurrent").invoke(instance) === home && isSelectedHome(home)
                    if (valid) accepted = request else {
                        // A canceled/late permission result belongs to this request,
                        // not whichever tab or page became active in the meantime.
                        result = null
                        YLog.info("Reborn: expired custom Home dispatch ignored")
                    }
                }
                scopes[scopes.lastIndex] = accepted
            }
            after { loading.get().let { if (it.isNotEmpty()) it.removeAt(it.lastIndex) } }
        }
        tab.resolve().firstMethod {
            name = routes.showPage; parameters(page, Boolean::class.javaPrimitiveType!!); returnType = Void.TYPE
        }.hook {
            before {
                val request = loading.get().lastOrNull() ?: return@before
                val home = request.home.get() ?: return@before
                val target = args[0] ?: return@before
                if (request.tab.get() !== instance || tab.getMethod("getCurrent").invoke(instance) !== home) return@before
                val navigationController = nativeController(instance) ?: return@before
                val targetIndex = controller.getDeclaredMethod("B", page).invoke(navigationController, target) as Int
                if (boundary.capture(request, instance, home, target, targetIndex)) {
                    YLog.info("Reborn: custom Home web boundary captured")
                }
            }
        }
        tab.resolve().firstMethod {
            name = routes.backwardPage
            parameters(Boolean::class, Boolean::class)
            returnType = Boolean::class.javaPrimitiveType!!
        }.hook {
            before {
                val home = tab.getMethod("getHomePage").invoke(instance) ?: return@before
                val current = tab.getMethod("getCurrent").invoke(instance) ?: return@before
                if (boundary.isRoot(home, current, index(instance) ?: -1,
                        tab.getMethod("isActive").invoke(instance) == true)) {
                    result = false
                }
            }
        }
        YLog.info("Reborn: custom Home Back boundary configured (${routes.backwardPage})")
    }

    private fun isSelectedHome(fragment: Any): Boolean {
        val home = readFieldOfType(fragment, "com.heytap.browser.main.home.a") ?: return false
        if (fragments[home]?.get() !== fragment) return false
        val major = readFieldOfType(home, "com.heytap.browser.main.home.normal.major.MajorHomePage") ?: return false
        return readFieldOfType(home, "com.heytap.browser.main.home.normal.a") === major
    }

    private fun hookNativeTabRoutes(routes: BrowserHomeNativeRoutes) {
        try {
            val tab = "com.heytap.browser.tab.BaseTab".toClassOrNull() ?: error("BaseTab unavailable")
            val page = "com.heytap.browser.page.TabPageFragment".toClassOrNull() ?: error("TabPageFragment unavailable")
            // Web/search toolbar Home uses g1/h1, not the HomeFrame listener g.b.
            tab.resolve().firstMethod { name = routes.returnHome; parameters(Boolean::class); returnType = Void.TYPE }.hook {
                before {
                    if (explicitWebHome.get() == 0) return@before
                    val fragment = tab.getMethod("getHomePage").invoke(instance) ?: return@before
                    if (fragment.javaClass.name == "com.heytap.browser.home.NormalHomePageFragment" &&
                        tab.getMethod("isActive").invoke(instance) == true) navigation.requestHome(fragment)
                }
                after {
                    val fragment = tab.getMethod("getHomePage").invoke(instance) ?: return@after
                    if (fragment.javaClass.name == "com.heytap.browser.home.NormalHomePageFragment") bindFragment(fragment)
                }
            }
            // onCurrentPageChanged is evidence of leaving/returning, unlike a void
            // loadUrl call which may still be waiting for native permission handling.
            tab.resolve().firstMethod { name = routes.pageChanged; parameters(page, page); returnType = Void.TYPE }.hook {
                after {
                    val previous = args[0]
                    val current = args[1]
                    if (previous != null && previous.javaClass.name == "com.heytap.browser.home.NormalHomePageFragment" && previous !== current) {
                        navigation.leftHome(previous)
                    }
                    if (current != null && current.javaClass.name == "com.heytap.browser.home.NormalHomePageFragment" && previous !== current) {
                        navigation.returnedHome(current)
                        bindFragment(current)
                    }
                }
            }
        } catch (error: Exception) {
            YLog.error("Reborn: native Home tab routing signature unavailable", error)
            throw error
        }
    }

    /** Native subpages switch inside this deferred action, not in fragment creation.
     * Only a change to MajorHomePage authorizes another redirect. Returning from a
     * webpage leaves the selected home subpage unchanged, so Back cannot loop.
     */
    private fun hookSubpageSelection() {
        try {
            val action = "com.heytap.browser.main.home.normal.NormalHome\$setCurrentPage\$2".toClassOrNull()
                ?: error("NormalHome selection action unavailable")
            action.resolve().firstMethod { name = "invoke"; parameters(); returnType = Void.TYPE }.hook {
                before {
                    val home = readFieldOfType(instance, "com.heytap.browser.main.home.normal.NormalHome")
                        ?: return@before
                    val selected = readFieldOfType(instance, "com.heytap.browser.main.home.normal.a")
                    subpageChanges[instance] = selected != null &&
                        selected !== readFieldOfType(home, "com.heytap.browser.main.home.normal.a")
                }
                after {
                    if (subpageChanges.remove(instance) != true) return@after
                    try {
                        val home = readFieldOfType(instance, "com.heytap.browser.main.home.normal.NormalHome")
                            ?: return@after
                        val fragment = fragments[home]?.get() ?: return@after
                        val selected = readFieldOfType(home, "com.heytap.browser.main.home.normal.a")
                        val major = readFieldOfType(home, "com.heytap.browser.main.home.normal.major.MajorHomePage")
                        if (selected !== major || major == null) {
                            navigation.invalidate(fragment)
                            return@after
                        }
                        val owner = ownerTab(fragment) ?: return@after
                        if (owner.javaClass.getMethod("isActive").invoke(owner) != true ||
                            owner.javaClass.getMethod("getCurrent").invoke(owner) !== fragment) return@after
                        navigation.rearm(fragment)
                        val view = major.javaClass.getMethod("getView").invoke(major) as? View ?: return@after
                        queueUrlHome(fragment, home, major, view)
                    } catch (error: Exception) {
                        YLog.error("Reborn: native Home selection navigation unavailable", error)
                    }
                }
            }
            YLog.info("Reborn: native Home subpage selection mapping configured")
        } catch (error: Exception) {
            YLog.error("Reborn: native Home subpage selection signature unavailable", error)
        }
    }

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
                        if (owner.javaClass.getMethod("isActive").invoke(owner) == true &&
                            owner.javaClass.getMethod("getCurrent").invoke(owner) === fragment) {
                            navigation.requestHome(fragment)
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
        val content = BrowserHomeContent()
        val weakContainer = WeakReference(container)
        val weakHome = WeakReference(home)
        val weakMajor = WeakReference(major)
        val refresh = {
            val root = weakContainer.get()
            if (root != null && replacement.parent === root) {
                val activeHome = weakHome.get()
                val host = activeHome?.let { fragments[it]?.get() }
                val recovering = mode == RebornBrowserConfig.URL && host != null && navigation.showRecovery(host)
                // Never uncover the native news anchor, including canceled
                // permission, a failed dispatch, or an unusual return path.
                content.show(root, replacement, false)
                (replacement as? UrlHomePanel)?.showRecovery(recovering)
                val page = weakMajor.get()
                if (!recovering && mode == RebornBrowserConfig.URL && host != null && activeHome != null && page != null) {
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

    private inner class UrlHomePanel(context: Context, retry: () -> Unit) : LinearLayout(context) {
        private var recoveryShown: Boolean? = null
        private val status = TextView(context).apply {
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(foreground(context))
        }
        private val retryButton = Button(context).apply {
            text = "重试"
            setOnClickListener { retry() }
        }
        init {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(context, 24), 0, dp(context, 24), 0)
            setBackgroundColor(background(context))
            addView(status, LayoutParams(-1, -2))
            addView(retryButton, LayoutParams(-2, -2))
            showRecovery(false)
        }
        fun showRecovery(recovering: Boolean) {
            if (recoveryShown == recovering) return
            recoveryShown = recovering
            status.text = if (recovering) "自定义主页暂未打开，请重试" else "正在打开主页…"
            retryButton.visibility = if (recovering) View.VISIBLE else View.GONE
        }
    }

    private fun buildUrlHome(context: Context, home: Any): View {
        if (RebornBrowserConfig.validHttpUrl(configuredUrl) == null) {
            return message(context, "主页网址无效，请在 LuckyTool 中重新设置")
        }
        val weakHome = WeakReference(home)
        // Successful navigation is automatic. Only a failed/canceled native load
        // exposes Retry, and it never uncovers the original news content.
        return UrlHomePanel(context) {
            val host = weakHome.get()?.let { fragments[it]?.get() }
            if (host != null && isSelectedHome(host)) {
                navigation.requestHome(host)
                bindFragment(host)
            }
        }
    }

    private fun queueUrlHome(fragment: Any, home: Any, major: Any, view: View) {
        val url = RebornBrowserConfig.validHttpUrl(configuredUrl) ?: return
        val ticket = navigation.ticket(fragment)
        view.postOnAnimation {
            try {
                if (fragments[home]?.get() !== fragment) return@postOnAnimation
                if (readFieldOfType(home, "com.heytap.browser.main.home.normal.a") !== major) return@postOnAnimation
                val owner = ownerTab(fragment) ?: return@postOnAnimation
                // Check ownership and epoch again when executing, not when queued.
                if (owner.javaClass.getMethod("isActive").invoke(owner) != true ||
                    owner.javaClass.getMethod("getCurrent").invoke(owner) !== fragment) return@postOnAnimation
                var attempted = false
                val dispatched = navigation.openOnce(fragment, ticket) {
                    attempted = true
                    openNativeTab(fragment, url, ticket)
                }
                if (attempted && !dispatched && navigation.failedDispatch(fragment, ticket)) {
                    view.requestLayout()
                } else {
                    recoverUnchangedHome(fragment, home, major, view, ticket)
                }
            } catch (error: Exception) {
                if (navigation.failedDispatch(fragment, ticket)) view.requestLayout()
                YLog.error("Reborn: native homepage activation skipped; recovery controls requested", error)
            }
        }
    }

    private fun recoverUnchangedHome(fragment: Any, home: Any, major: Any, view: View, ticket: Long) {
        if (!navigation.needsRecovery(fragment, ticket) || pendingRecovery[fragment] == ticket) return
        pendingRecovery[fragment] = ticket
        view.postDelayed({
            if (pendingRecovery[fragment] == ticket) pendingRecovery.remove(fragment)
            try {
                if (fragments[home]?.get() !== fragment ||
                    readFieldOfType(home, "com.heytap.browser.main.home.normal.a") !== major) return@postDelayed
                val owner = ownerTab(fragment) ?: return@postDelayed
                if (owner.javaClass.getMethod("isActive").invoke(owner) != true ||
                    owner.javaClass.getMethod("getCurrent").invoke(owner) !== fragment) return@postDelayed
                if (navigation.recoverUndispatched(fragment, ticket)) {
                    // Do not resubmit a permission-blocked native request, clear
                    // history, or strand the user on an empty replacement.
                    view.requestLayout()
                    YLog.info("Reborn: homepage request did not leave Home; recovery controls requested")
                }
            } catch (error: Exception) {
                YLog.error("Reborn: homepage request recovery unavailable", error)
            }
        }, 2000L)
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

    private fun openNativeTab(fragment: Any, url: String, ticket: Long? = null): Boolean {
        val safe = RebornBrowserConfig.validHttpUrl(url) ?: return false
        return try {
            val owner = ownerTab(fragment) ?: return false
            if (owner.javaClass.getMethod("isActive").invoke(owner) != true) return false
            if (owner.javaClass.getMethod("getCurrent").invoke(owner) !== fragment) return false
            val previous = dispatching.get()
            try {
                dispatching.set(ticket?.let { BrowserHomeBoundary.Request(owner, fragment, it) })
                owner.javaClass.getMethod("loadUrl", String::class.java).invoke(owner, safe)
            } finally {
                dispatching.set(previous)
            }
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
