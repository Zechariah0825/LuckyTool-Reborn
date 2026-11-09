package com.luckyzyx.luckytool.hook.scopes.market

/** Pure model filter: remove only '精选' when '应用时刻' is present in the SAME list. */
object RebornMarketTabRules {
    fun <T> removeFeatured(items: List<T>, title: (T) -> String?): List<T>? {
        if (items.size < 2) return null
        val names = items.map(title)
        val featured = names.indexOf("精选")
        if (featured < 0 || "应用时刻" !in names) return null
        return items.filterIndexed { index, _ -> index != featured }
    }
}
