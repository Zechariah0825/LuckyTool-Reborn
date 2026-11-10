package com.luckyzyx.luckytool.hook.scopes.browser

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.LinearLayout
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
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
            if (hideMine) {
                val rewardLabel = runCatching {
                    val id = root.resources.getIdentifier("receive_reward", "string", "com.heytap.browser")
                    if (id != 0) root.resources.getString(id) else null
                }.getOrNull()
                labels.filter { it.visibility == View.VISIBLE }.forEach { tv ->
                    if (BrowserMineRules.isCashLabel(tv.text?.toString(), rewardLabel)) hideCashButton(tv)
                }
            }
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

    private fun hideCashButton(text: TextView) {
        if (!isInProfile(text)) return
        val card = ancestors(text).firstOrNull {
            BrowserMineRules.isAccountCard(it.javaClass.name)
        } ?: return
        try {
            val info = card.javaClass.getMethod("getAccountInfoView").invoke(card) as? View ?: return
            fun button(name: String): View? {
                val id = info.resources.getIdentifier(name, "id", "com.heytap.browser")
                return if (id != 0) info.findViewById(id) else null
            }
            val rewardButton = button("button_login_sign") ?: button("nearx_btn_sign_in") ?: return
            if (rewardButton.visibility != View.VISIBLE || !isDescendant(text, rewardButton)) return
            // AcBaseView.hideSignInBtn only hides the sign/reward control. It keeps
            // the avatar/account/VIP card. Match actual visible reward text so an
            // unauthenticated "登录" control is never hidden by a cached reward title.
            card.javaClass.getMethod("hideSignInBtn").invoke(card)
        } catch (error: Exception) {
            YLog.error("Reborn: cannot hide profile reward button", error)
        }
    }

    private fun ancestors(view: View): Sequence<View> = generateSequence(view as View?) {
        it.parent as? View
    }
    private fun isDescendant(child: View, ancestor: View): Boolean =
        ancestors(child).any { it === ancestor }
    private fun dp(v: View, value: Int) = (v.resources.displayMetrics.density * value + .5f).toInt()
}
