package com.luckyzyx.luckytool.hook.scopes.browser

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/** Operates only on the two SDK sign-button fields, never the account card itself. */
internal object BrowserProfileViews {
    fun isInProfile(view: View): Boolean = ancestors(view).any {
        BrowserMineRules.isProfile(it.javaClass.name)
    }

    fun cleanAccountInfo(info: View) {
        if (!BrowserMineRules.isAccountInfo(info.javaClass.name) || !isInProfile(info)) return
        // CustomSignInVipCard inflates CustomSignInAccountInfoView. Its two
        // button fields are declared by HeyTapAccountInfoView, not the subclass.
        val fieldOwner = generateSequence<Class<*>>(info.javaClass) { it.superclass }
            .firstOrNull { it.name == BrowserMineRules.ACCOUNT_INFO_BASE } ?: return
        val reward = runCatching {
            val id = info.resources.getIdentifier("receive_reward", "string", "com.heytap.browser")
            if (id == 0) null else info.resources.getString(id)
        }.getOrNull()
        val candidates = listOf("mImageBtnSignIn", "mBtnSignIn").mapNotNull { fieldName ->
            runCatching {
                fieldOwner.getDeclaredField(fieldName).let {
                    it.isAccessible = true
                    it.get(info) as? View
                }
            }.getOrNull()
        }
        // The shipped APK obfuscates resource entry names. Use the verified SDK
        // fields instead of guessed names, numeric IDs or arbitrary cash text.
        hideCashButtons(info, candidates.distinct(), reward)
    }

    internal fun hideCashButtons(info: View, candidates: List<View>, reward: String?) {
        candidates.filterIsInstance<TextView>().forEach { button ->
            // Inspect both controls, including the hidden first alternative. A cached
            // login or remote title is not evidence that this visible control is cash.
            if (ancestors(button).any { it === info } &&
                BrowserMineRules.isCashLabel(button.text?.toString(), reward)
            ) {
                if (button.visibility != View.GONE) button.visibility = View.GONE
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
                BrowserMineRules.isAccountInfo(view.javaClass.name) -> cleanAccountInfo(view)
            }
            if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index), depth + 1)
        }
        visit(profile, 0)
    }

    private fun ancestors(view: View) = generateSequence(view as View?) { it.parent as? View }
}
