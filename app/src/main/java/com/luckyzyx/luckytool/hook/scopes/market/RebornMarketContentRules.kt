package com.luckyzyx.luckytool.hook.scopes.market

/** Only the complete, explicitly named home recommendation card is removed. */
object RebornMarketContentRules {
    private val homeFragments = setOf(
        "com.heytap.cdo.client.cards.page.main.home.HomeChildCardFragment",
        "com.heytap.cdo.client.cards.page.main.home.HomeChosenCardFragment",
    )

    fun isHomeFragment(classNames: Sequence<String>): Boolean =
        classNames.any { it in homeFragments }

    fun <T> filterCards(items: List<T>, isHome: Boolean, title: (T) -> String?): List<T>? {
        if (!isHome) return null
        val kept = items.filterNot { title(it)?.trim() == "精品随心选" }
        return if (kept.size == items.size) null else ArrayList(kept)
    }

    // Read the card's own title, never names/titles of contained apps or descendants.
    fun cardTitle(card: Any?): String? = try {
        card?.javaClass?.getMethod("getTitle")?.invoke(card) as? String
    } catch (_: ReflectiveOperationException) { null }
}
