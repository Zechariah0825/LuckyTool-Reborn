package com.luckyzyx.luckytool.hook.scopes.market

/** Protocol from Market 26.10.2_CN. Only apply at the SearchHome presenter. */
internal object MarketSearchRules {
    private val retainedCodes = setOf(100203, 100204, 40117, 154)

    /** null means an unreadable schema: keep the original batch, never partially filter it. */
    fun filterLanding(cards: List<*>): List<Any>? {
        val result = ArrayList<Any>()
        for (card in cards) {
            if (card == null) return null
            val code = try {
                card.javaClass.getMethod("getCode").invoke(card) as? Int ?: return null
            } catch (_: ReflectiveOperationException) { return null }
            if (code in retainedCodes) result.add(card)
        }
        return result
    }

    /** Call the native hint-only API, which also updates the search button state.
     * Never call setSearchText, setText, or replace the user's Editable. */
    fun clearHint(actionBar: Any) {
        actionBar.javaClass.getMethod("setSearchHint", String::class.java).invoke(actionBar, "")
    }
}
