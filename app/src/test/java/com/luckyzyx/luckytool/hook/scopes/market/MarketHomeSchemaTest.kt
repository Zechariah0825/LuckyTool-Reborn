package com.luckyzyx.luckytool.hook.scopes.market

import org.junit.Assert.*
import org.junit.Test

class MarketHomeSchemaTest {
    private val shared = mapOf(
        "\u08a8" to "com.heytap.cdo.client.cards.page.main.home.tab.view.ScrollIdleTabLayout",
        "\u08aa" to "com.nearme.module.ui.view.CdoViewPager",
    )
    private val newFields = shared + mapOf(
        "\u08af" to "com.heytap.cdo.client.cards.page.main.home.HomeChosenCardFragment",
        "\u08b0" to "int", "\u08b4" to "int",
    )
    @Test fun `old layout remains supported`() {
        val schema = MarketHomeSchema.resolve(shared + mapOf("\u08af" to "int", "\u08b3" to "int"))!!
        assertEquals("\u08af", schema.homeIndex)
        assertEquals("\u08b3", schema.startupIndex)
    }
    @Test fun `new fragment field is never treated as an index`() {
        val schema = MarketHomeSchema.resolve(newFields)!!
        assertEquals("\u08b0", schema.homeIndex)
        assertEquals("\u08b4", schema.startupIndex)
        assertNotEquals("\u08af", schema.homeIndex)
    }
    @Test fun `incomplete layouts and changed types fail open`() {
        for (key in newFields.keys) assertNull(MarketHomeSchema.resolve(newFields - key))
        assertNull(MarketHomeSchema.resolve(newFields + ("\u08b0" to "long")))
        assertNull(MarketHomeSchema.resolve(newFields + ("\u08af" to "unknown.Fragment")))
        assertNull(MarketHomeSchema.resolve(emptyMap()))
    }
    @Test fun `ambiguous index mappings fail open`() {
        assertNull(MarketHomeSchema.resolve(newFields + mapOf("\u08af" to "int", "\u08b3" to "int")))
    }
}
