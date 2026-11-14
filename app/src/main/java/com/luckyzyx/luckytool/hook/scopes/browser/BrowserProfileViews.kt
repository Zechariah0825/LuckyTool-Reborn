package com.luckyzyx.luckytool.hook.scopes.browser

import android.content.Context
import android.view.View
import android.view.ViewGroup

/** Native account-role cleanup. Button captions and localized resources are never selectors. */
internal object BrowserProfileViews {
    fun isInProfile(view: View): Boolean = ancestors(view).any {
        BrowserMineRules.isProfile(it.javaClass.name)
    }

    private fun owner(value: Any, name: String): Class<*>? =
        generateSequence<Class<*>>(value.javaClass) { it.superclass }.firstOrNull { it.name == name }

    private fun read(value: Any, declaring: String, field: String): Any? = runCatching {
        owner(value, declaring)?.getDeclaredField(field)?.let { it.isAccessible = true; it.get(value) }
    }.getOrNull()

    fun rewardCard(view: View): View? {
        if (!isInProfile(view)) return null
        return ancestors(view).firstOrNull { candidate ->
            generateSequence<Class<*>>(candidate.javaClass) { it.superclass }
                .any { BrowserMineRules.isAccountCard(it.name) } &&
                owner(candidate, BrowserMineRules.ACCOUNT_CARD_BASE) != null
        }
    }

    fun isRewardInfo(info: View): Boolean =
        (info.javaClass.name == BrowserMineRules.ACCOUNT_INFO_BASE ||
            owner(info, BrowserMineRules.ACCOUNT_INFO_CUSTOM) != null) &&
            owner(info, BrowserMineRules.ACCOUNT_INFO_BASE) != null && rewardCard(info)?.let {
                read(it, BrowserMineRules.ACCOUNT_CARD_BASE, "mAccountInfoView") === info
            } == true

    fun isLoggedIn(info: View): Boolean? = rewardCard(info)?.let {
        read(it, BrowserMineRules.ACCOUNT_CARD_BASE, "mIsLogin") as? Boolean
    }

    /** setButton(Context, ButtonInfo, isLogin, thirdText): retain the native login branch. */
    fun filterButtonBinding(info: View, args: Array<Any?>) {
        if (!isRewardInfo(info) || args.size != 4 || args[2] !is Boolean) return
        args[1] = null
        args[3] = null
        clearThirdSource(rewardCard(info) ?: return)
    }

    /** SDK true means a remote ButtonInfo caption, not login. False can mean either. */
    fun filterSignText(info: View, args: Array<Any?>) {
        if (!isRewardInfo(info) || args.size != 2) return
        if (args[1] == true || isLoggedIn(info) == true) args[0] = null
    }

    fun clearThirdSource(card: View) {
        if (rewardCard(card) !== card) return
        runCatching {
            owner(card, BrowserMineRules.ACCOUNT_CARD_BASE)?.getDeclaredField("mThirdBtnText")?.let {
                it.isAccessible = true
                it.set(card, null)
            }
        }
    }

    fun cleanAccountInfo(info: View) {
        if (!isRewardInfo(info)) return
        val card = rewardCard(info) ?: return
        val loggedIn = isLoggedIn(info) ?: return // unknown SDK state must not hide login
        clearThirdSource(card)
        val controls = listOf("mImageBtnSignIn", "mBtnSignIn").mapNotNull {
            read(info, BrowserMineRules.ACCOUNT_INFO_BASE, it) as? View
        }.distinct().filter { button -> button !== info && ancestors(button).any { it === info } }
        if (loggedIn) {
            // In this native state both alternatives are remote/third-party actions.
            controls.forEach { if (it.visibility != View.GONE) it.visibility = View.GONE }
        } else {
            // An account response can bind before attachment. The native remote-tag
            // identifies that case without inspecting its caption. Rebind just the
            // button with no remote/third payload, restoring SDK's real login action.
            val image = read(info, BrowserMineRules.ACCOUNT_INFO_BASE, "mImageBtnSignIn") as? View
            if (image in controls && image?.tag == true) {
                runCatching {
                    val base = owner(info, BrowserMineRules.ACCOUNT_INFO_BASE) ?: return@runCatching
                    val model = Class.forName(
                        "com.platform.sdk.center.sdk.mvvm.model.data.AcInfo\$ButtonInfo", false, info.javaClass.classLoader)
                    base.getDeclaredMethod("setButton", Context::class.java, model,
                        Boolean::class.javaPrimitiveType, String::class.java).let {
                        it.isAccessible = true
                        it.invoke(info, info.context, null, false, null)
                    }
                }
            }
        }
    }

    fun cleanProfile(profile: View) {
        if (!BrowserMineRules.isProfile(profile.javaClass.name)) return
        fun visit(view: View, depth: Int) {
            if (depth > 35) return
            when {
                BrowserMineRules.isBookshelf(view.javaClass.name) -> {
                    if (view.visibility != View.GONE) view.visibility = View.GONE
                    return
                }
                isRewardInfo(view) -> cleanAccountInfo(view)
            }
            if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index), depth + 1)
        }
        visit(profile, 0)
    }

    private fun ancestors(view: View) = generateSequence(view as View?) { it.parent as? View }
}
