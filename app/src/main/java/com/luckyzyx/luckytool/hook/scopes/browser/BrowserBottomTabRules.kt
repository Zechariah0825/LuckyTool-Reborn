package com.luckyzyx.luckytool.hook.scopes.browser

/** DEX-verified native type IDs: home=1, video=2, search=9, novel=8, mine=6. */
internal object BrowserBottomTabRules {
    fun <T> filter(items: List<T>, type: (T) -> Int?): List<T>? {
        val types = items.map(type)
        if (types.any { it == null } || !types.containsAll(listOf(1, 9, 6))) return null
        if (types.none { it == 2 || it == 8 }) return null
        return items.filterIndexed { index, _ -> types[index] != 2 && types[index] != 8 }
    }
}
