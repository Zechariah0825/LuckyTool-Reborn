package com.luckyzyx.luckytool.hook.scopes.market

/** Verified DEX layouts, not a guess based on field order or the first integer. */
internal data class MarketHomeSchema(val homeIndex: String, val startupIndex: String) {
    companion object {
        private const val CHOSEN = "com.heytap.cdo.client.cards.page.main.home.HomeChosenCardFragment"
        fun resolve(fields: Map<String, String>): MarketHomeSchema? {
            if (fields["\u08a8"] != "com.heytap.cdo.client.cards.page.main.home.tab.view.ScrollIdleTabLayout" ||
                fields["\u08aa"] != "com.nearme.module.ui.view.CdoViewPager") return null
            val candidates = listOf(
                // 26.10.2_CN: the home index occupies 08af.
                MarketHomeSchema("\u08af", "\u08b3"),
                // 26.11.3_CN shifts the HomeChosenCardFragment reference to 08af.
                MarketHomeSchema("\u08b0", "\u08b4"),
            ).filter { fields[it.homeIndex] == "int" && fields[it.startupIndex] == "int" }
            val selected = candidates.singleOrNull() ?: return null
            if (selected.homeIndex == "\u08b0" && fields["\u08af"] != CHOSEN) return null
            return selected
        }
    }
}
