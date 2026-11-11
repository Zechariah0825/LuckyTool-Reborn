package com.luckyzyx.luckytool.hook.scopes.browser

/** Current APK's empty-input card registry, not the typed-suggestion adapter. */
internal object BrowserSearchLandingPolicy {
    const val PREF = "reborn_browser_search_history_only"
    const val PART = "com.heytap.browser.search.suggest.local.part.impl.SearchInputPart"
    const val HISTORY_ITEM = "com.heytap.browser.search.suggest.card.item.g"
    const val INCOGNITO_ITEM = "com.heytap.browser.search.suggest.card.item.h"
    const val INCOGNITO_VIEW = "com.heytap.browser.search.suggest.view.IncognitoView"
    const val INCOGNITO_HEAD = "com.heytap.browser.search.suggest.view.IncognitoViewHead"
    const val ROOT = "com.heytap.browser.search.suggest.view.SearchMainScrollView"
    const val HEADER = "com.heytap.browser.search.suggest.view.SearchScrollHeadView"
    data class Card(val key: Int, val className: String, val ownerClass: String)

    /** Fail open if the history card/owner contract has changed. Never synthesize history. */
    fun removableKeys(cards: List<Card>): List<Int>? {
        if (cards.isEmpty() || cards.any { it.ownerClass != PART }) return null
        if (cards.count { it.key == 4 && it.className == HISTORY_ITEM } != 1) return null
        return cards.filterNot { (it.key == 4 && it.className == HISTORY_ITEM) ||
            (it.key == 8 && it.className == INCOGNITO_ITEM) }.map { it.key }
    }
}
