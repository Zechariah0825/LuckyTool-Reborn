package com.luckyzyx.luckytool.hook.scopes.browser

/** Stable names verified in both supplied 40.10 browser APKs. No numeric resource IDs. */
internal object BrowserMineRules {
    fun isProfile(className: String) = className == "com.heytap.browser.usercenter.my.MyProfileView"
    fun isBookshelf(className: String) =
        className == "com.heytap.browser.usercenter.my.bookshelf.MyBookshelfHistoryCard"
    fun isAccountInfo(className: String) =
        className == "com.platform.sdk.center.widget.HeyTapAccountInfoView"
    fun isAccountCard(className: String) =
        className == "com.platform.sdk.center.sdk.mvvm.view.ui.CustomSignInVipCard"

    fun isCashLabel(text: String?, localizedReward: String?): Boolean {
        val label = text?.trim().orEmpty()
        if (label.isEmpty()) return false
        return label == "领现金" ||
            (!localizedReward.isNullOrBlank() && label == localizedReward.trim())
    }
}
