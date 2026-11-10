package com.luckyzyx.luckytool.hook.scopes.market

enum class MarketPageKind { CHOSEN, APP_MOMENT, OTHER }
data class MarketPageSignature(val title: String, val kind: MarketPageKind)
data class MarketHomePairs<T, P>(val tabs: List<T>, val pages: List<P>, val selectedIndex: Int, val homeIndex: Int)

/** Runs only AFTER the native factory has associated each title with its real page. */
object RebornMarketTabRules {
    fun pageKind(classNames: Sequence<String>, actionParam: String? = null): MarketPageKind {
        val names = classNames.toSet()
        return when {
            "com.heytap.cdo.client.cards.page.main.home.HomeChosenCardFragment" in names -> MarketPageKind.CHOSEN
            "com.heytap.cdo.client.cn.appmoment.AppMomentCardsFragment" in names -> MarketPageKind.APP_MOMENT
            "com.heytap.cdo.client.cards.page.main.home.HomeChildCardFragment" in names &&
                appMomentRoute(actionParam) -> MarketPageKind.APP_MOMENT
            else -> MarketPageKind.OTHER
        }
    }

    private fun appMomentRoute(actionParam: String?): Boolean = try {
        // Native hi7 factory stores the original actionParam on the pager item.
        // With no fragment service, /appmoment/main uses HomeChildCardFragment
        // and defaults its request path to /card/store/v4/column/main.
        val uri = java.net.URI(actionParam ?: "")
        uri.scheme == "oap" && uri.host == "mk" && uri.path == "/appmoment/main"
    } catch (_: java.net.URISyntaxException) { false }

    fun <T, P> filterPairs(
        tabs: List<T>, pages: List<P>, signatures: List<MarketPageSignature>,
        selectedTitle: String?, title: (T) -> String?,
    ): MarketHomePairs<T, P>? {
        if (tabs.size != pages.size || tabs.size != signatures.size) return null
        val titles = tabs.map { title(it)?.trim() ?: return null }
        if (titles.indices.any { titles[it] != signatures[it].title.trim() }) return null
        val moments = titles.indices.filter { titles[it] == "应用时刻" }
        val featured = titles.indices.filter { titles[it] == "精选" }
        // Refuse mismatched/relabelled native pages, absent routes and ambiguous lists.
        if (moments.size != 1 || featured.size != 1) return null
        if (signatures[moments.single()].kind != MarketPageKind.APP_MOMENT ||
            signatures[featured.single()].kind != MarketPageKind.CHOSEN) return null
        val kept = titles.indices.filter { it != featured.single() }
        val keptTitles = kept.map { titles[it] }
        val selected = keptTitles.indexOf(selectedTitle?.trim()).takeIf { it >= 0 }
            ?: keptTitles.indexOf("应用时刻")
        return MarketHomePairs(kept.map { tabs[it] }, kept.map { pages[it] }, selected, keptTitles.indexOf("应用时刻"))
    }
}
