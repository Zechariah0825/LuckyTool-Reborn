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
import java.util.WeakHashMap

/** Visual fallback for asynchronously rendered browser pages. Enabled only on request.
 * The data-layer bottom-tab hook is preferable; this handles re-rendered legacy layouts.
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
                    val listener = ViewTreeObserver.OnGlobalLayoutListener { clean(root) }
                    root.viewTreeObserver.addOnGlobalLayoutListener(listener)
                    observed[root] = listener
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
                labels.filter { it.visibility == View.VISIBLE }.forEach { tv ->
                    when (tv.text?.toString()?.trim()) {
                        "领现金" -> hideCashButton(tv, root)
                        "我的小说" -> hideNovelCard(tv, root)
                    }
                }
            }
        } finally {
            inLayout = false
        }
    }

    private fun walk(view: View, depth: Int, output: MutableList<TextView>) {
        if (depth > 35 || view.visibility != View.VISIBLE) return
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

    private fun hideCashButton(text: TextView, root: View) {
        val maxWidth = (root.width * .5f).toInt()
        val candidate = ancestors(text).firstOrNull {
            it !== root && it.width in 1..maxWidth && it.height in 1..dp(text, 92) &&
                (it.isClickable || it.background != null)
        }
        (candidate ?: text).visibility = View.GONE
    }

    private fun hideNovelCard(text: TextView, root: View) {
        // Remove the entire horizontally spanning card, not the heading alone.
        val candidate = ancestors(text).firstOrNull {
            it !== root && it.width > root.width * .72f &&
                it.height in dp(text, 96)..dp(text, 460) && it.background != null
        }
        (candidate ?: text).visibility = View.GONE
    }

    private fun ancestors(view: View): Sequence<View> = generateSequence(view as View?) {
        it.parent as? View
    }
    private fun isDescendant(child: View, ancestor: View): Boolean =
        ancestors(child).any { it === ancestor }
    private fun dp(v: View, value: Int) = (v.resources.displayMetrics.density * value + .5f).toInt()
}
