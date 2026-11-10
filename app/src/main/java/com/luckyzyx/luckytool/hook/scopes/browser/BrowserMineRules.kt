package com.luckyzyx.luckytool.hook.scopes.browser

/** Known browser/SDK view names. No numeric resource IDs or arbitrary text-container matching. */
internal object BrowserMineRules {
    const val ACCOUNT_INFO_BASE = "com.platform.sdk.center.widget.HeyTapAccountInfoView"

    fun isProfile(className: String) = className == "com.heytap.browser.usercenter.my.MyProfileView"
    fun isBookshelf(className: String) =
        className == "com.heytap.browser.usercenter.my.bookshelf.MyBookshelfHistoryCard"
    fun isAccountInfo(className: String) =
        className == ACCOUNT_INFO_BASE ||
            className == "com.platform.sdk.center.widget.CustomSignInAccountInfoView"
    fun isAccountCard(className: String) =
        className == "com.platform.sdk.center.sdk.mvvm.view.ui.CustomSignInVipCard"

    fun isCashLabel(text: String?, localizedReward: String?): Boolean {
        val label = text?.trim().orEmpty()
        if (label.isEmpty()) return false
        return label == "领现金" ||
            (!localizedReward.isNullOrBlank() && label == localizedReward.trim())
    }
}
