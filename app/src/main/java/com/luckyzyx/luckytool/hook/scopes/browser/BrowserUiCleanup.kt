package com.luckyzyx.luckytool.hook.scopes.browser

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.LinearLayout
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import com.highcapable.yukihookapi.hook.log.YLog

/** DEX-verified MyProfile subtree cleanup plus a bottom-navigation visual fallback.
 * Keep watching layout because the browser asynchronously restores its card visibility.
 * Never infer a removable profile card from text geometry in unrelated pages.
 */
@Obfuscate
class BrowserUiCleanup(private val hideMine: Boolean, private val hideBottom: Boolean) : YukiBaseHooker() {
    private val observed = WeakHashMap<View, ViewTreeObserver.OnGlobalLayoutListener>()
    private var inLayout = false

    override fun onHook() {
        if (hideMine) hookNativeProfile()
        Activity::class.resolve().firstMethod {
            name = "onWindowFocusChanged"
            parameters(Boolean::class)
        }.hook {
            after {
                val activity = instance as? Activity ?: return@after
                if (activity.packageName != "com.heytap.browser") return@after
                val root = activity.window?.decorView ?: return@after
                if (!observed.containsKey(root)) {
                    // The WeakHashMap value must not strongly retain its own key.
                    // Remove observers on detach so repeated windows do not leak Activities.
                    val weakRoot = WeakReference(root)
                    val listener = ViewTreeObserver.OnGlobalLayoutListener {
                        weakRoot.get()?.let(::clean)
                    }
                    root.viewTreeObserver.addOnGlobalLayoutListener(listener)
                    observed[root] = listener
                    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) = Unit
                        override fun onViewDetachedFromWindow(v: View) {
                            if (v.viewTreeObserver.isAlive) {
                                v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                            }
                            observed.remove(v)
                            v.removeOnAttachStateChangeListener(this)
                        }
                    })
                }
                root.post { clean(root) }
            }
        }
    }

    private fun clean(root: View) {
        if (inLayout || root.width == 0 || root.height == 0) return
        inLayout = true
        try {
            val labels = ArrayList<TextView>()
            walk(root, 0, labels)
            if (hideBottom) removeBottomNav(root, labels)

        } catch (error: Exception) {
            YLog.error("Reborn: browser view cleanup skipped", error)
        } finally {
            inLayout = false
        }
    }

    private fun walk(view: View, depth: Int, output: MutableList<TextView>) {
        if (depth > 35 || view.visibility != View.VISIBLE) return
        if (hideMine && BrowserMineRules.isBookshelf(view.javaClass.name) && isInProfile(view)) {
            view.visibility = View.GONE
            return
        }
        if (hideMine && BrowserProfileViews.isRewardInfo(view)) {
            BrowserProfileViews.cleanAccountInfo(view)
        }
        if (view is TextView) output.add(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i), depth + 1, output)
    }

    private fun removeBottomNav(root: View, labels: List<TextView>) {
        val find: (String) -> TextView? = { name ->
            labels.firstOrNull { it.text?.toString()?.trim() == name && isAtBottom(it, root) }
        }
        val home = find("首页") ?: return
        val search = find("搜索+") ?: return
        val mine = find("我的") ?: return
        val container = ancestors(home).firstOrNull { ancestor ->
            ancestor is ViewGroup && ancestor.height in 1..(root.height / 4) &&
                ancestor.width >= root.width / 2 &&
                isDescendant(search, ancestor) && isDescendant(mine, ancestor)
        } as? ViewGroup ?: return
        val removed = mutableListOf<View>()
        listOf("视频", "小说").forEach { target ->
            val tv = find(target) ?: return@forEach
            if (!isDescendant(tv, container)) return@forEach
            val child = ancestors(tv).takeWhile { it !== container }.lastOrNull() ?: return@forEach
            if (child.parent === container && child.visibility == View.VISIBLE) {
                child.visibility = View.GONE
                removed.add(child)
            }
        }
        // Some ColorOS navigation bars use explicit weightSum=5. Merely hiding
        // two children leaves 2/5 of the bar empty in that implementation.
        // Rebalance ONLY a positively identified five-item horizontal layout.
        if (removed.size == 2 && container is LinearLayout &&
            container.orientation == LinearLayout.HORIZONTAL && container.childCount == 5
        ) {
            val survivors = (0 until container.childCount)
                .map(container::getChildAt).filter { it.visibility == View.VISIBLE }
            if (survivors.size == 3 && survivors.all { it.layoutParams is LinearLayout.LayoutParams }) {
                container.weightSum = 3f
                survivors.forEach { child ->
                    (child.layoutParams as LinearLayout.LayoutParams).let { lp ->
                        lp.width = 0
                        lp.weight = 1f
                        child.layoutParams = lp
                    }
                }
            }
        }
    }

    private fun isAtBottom(view: View, root: View): Boolean {
        val pos = IntArray(2); val base = IntArray(2)
        view.getLocationOnScreen(pos); root.getLocationOnScreen(base)
        return pos[1] - base[1] > root.height * .73f
    }

    private fun isInProfile(view: View): Boolean =
        ancestors(view).any { BrowserMineRules.isProfile(it.javaClass.name) }

    /** Both supplied APKs have these named SDK setters and profile attachment.
     * Hooking the actual views also catches asynchronous account refreshes when
     * the browser Activity overrides onWindowFocusChanged without calling super.
     */
    private fun hookNativeProfile() {
        val profileClass = "com.heytap.browser.usercenter.my.MyProfileView".toClassOrNull()
        try {
            profileClass?.resolve()?.firstMethod {
                name = "onAttachedToWindow"
                parameters()
            }?.hook {
                after {
                    val profile = instance as? View ?: return@after
                    BrowserProfileViews.cleanProfile(profile)
                    observeProfile(profile)
                }
            }
        } catch (error: Throwable) {
            YLog.error("Reborn: profile attachment hook mismatch", error)
        }
        hookRewardBinding()
    }

    private fun hookRewardBinding() {
        val base = BrowserMineRules.ACCOUNT_INFO_BASE.toClassOrNull() ?: return
        val model = "com.platform.sdk.center.sdk.mvvm.model.data.AcInfo".toClassOrNull() ?: return
        val buttonModel = "com.platform.sdk.center.sdk.mvvm.model.data.AcInfo\$ButtonInfo".toClassOrNull() ?: return
        fun bind(name: String, types: Array<Class<*>>, beforeFilter: (View, Array<Any?>) -> Unit = { _, _ -> }) {
            try {
                base.resolve().firstMethod { this.name = name; parameters(*types); returnType = Void.TYPE }.hook {
                    before { (instance as? View)?.let { beforeFilter(it, args) } }
                    after { (instance as? View)?.let(BrowserProfileViews::cleanAccountInfo) }
                }
            } catch (error: Throwable) {
                YLog.error("Reborn: profile reward binding mismatch: $name", error)
            }
        }
        bind("setButton", arrayOf(android.content.Context::class.java, buttonModel, Boolean::class.java, String::class.java),
            BrowserProfileViews::filterButtonBinding)
        bind("setSignBtnText", arrayOf(String::class.java, Boolean::class.java), BrowserProfileViews::filterSignText)
        bind("setShowSignButton", arrayOf(Boolean::class.java)) { info, args ->
            if (BrowserProfileViews.isRewardInfo(info) && BrowserProfileViews.isLoggedIn(info) == true) args[0] = false
        }
        bind("setThirdSignBtn", arrayOf(Int::class.java, Int::class.java, Int::class.java, String::class.java)) { info, args ->
            if (BrowserProfileViews.isRewardInfo(info)) args[3] = null
        }
        bind("setAccountInfo", arrayOf(android.content.Context::class.java, model, Boolean::class.java, String::class.java))
        bind("setUnLoginAccountInfo", arrayOf(android.content.Context::class.java, model, String::class.java))

        // Clear the browser's alternate source, not only the currently displayed
        // remote title. Otherwise the fallback reward reappears after a refresh.
        val card = BrowserMineRules.ACCOUNT_CARD_BASE.toClassOrNull() ?: return
        listOf("setSignInBtn", "setSignInBtnText", "applyPendingThirdSignBtnConfig").forEach { name ->
            try {
                card.resolve().firstMethod {
                    this.name = name
                    when (name) {
                        "setSignInBtn" -> parameters(Int::class, Int::class, Int::class, String::class)
                        "setSignInBtnText" -> parameters(String::class)
                        else -> parameters()
                    }
                    returnType = Void.TYPE
                }.hook {
                    before {
                        val view = instance as? View ?: return@before
                        if (BrowserProfileViews.rewardCard(view) !== view) return@before
                        BrowserProfileViews.clearThirdSource(view)
                        if (name == "setSignInBtn") args[3] = null
                        if (name == "setSignInBtnText") args[0] = null
                    }
                }
            } catch (error: Throwable) {
                YLog.error("Reborn: profile reward source mismatch: $name", error)
            }
        }
    }

    private fun observeProfile(profile: View) {
        if (observed.containsKey(profile)) return
        val weak = WeakReference(profile)
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            weak.get()?.let(BrowserProfileViews::cleanProfile)
        }
        observed[profile] = listener
        profile.viewTreeObserver.addOnGlobalLayoutListener(listener)
        profile.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                if (v.viewTreeObserver.isAlive) v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                observed.remove(v)
                v.removeOnAttachStateChangeListener(this)
            }
        })
    }

    private fun ancestors(view: View): Sequence<View> = generateSequence(view as View?) {
        it.parent as? View
    }
    private fun isDescendant(child: View, ancestor: View): Boolean =
        ancestors(child).any { it === ancestor }
    private fun dp(v: View, value: Int) = (v.resources.displayMetrics.density * value + .5f).toInt()
}
