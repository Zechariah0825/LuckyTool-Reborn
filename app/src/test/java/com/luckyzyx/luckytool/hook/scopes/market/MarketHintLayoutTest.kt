package com.luckyzyx.luckytool.hook.scopes.market

import org.junit.Assert.*
import org.junit.Test

@Suppress("UNUSED_PARAMETER")
class MarketHintLayoutTest {
    class Bar
    class Recommendation
    class OldHelper {
        companion object {
            @JvmStatic fun `Ϳ`(bar: Bar): Recommendation? = null
            @JvmStatic fun `ԫ`(bar: Bar, recommendation: Recommendation, hint: String) {}
        }
    }
    class NewHelper {
        companion object {
            @JvmStatic fun `Ԩ`(bar: Bar): Recommendation? = null
            @JvmStatic fun `ԭ`(bar: Bar, recommendation: Recommendation, hint: String) {}
        }
    }
    class WrongReturn {
        companion object {
            @JvmStatic fun `Ԩ`(bar: Bar): String? = null
            @JvmStatic fun `ԭ`(bar: Bar, recommendation: Recommendation, hint: String) {}
        }
    }
    class WrongBinderReturn {
        companion object {
            @JvmStatic fun `Ԩ`(bar: Bar): Recommendation? = null
            @JvmStatic fun `ԭ`(bar: Bar, recommendation: Recommendation, hint: String) = true
        }
    }
    class NonStatic {
        fun `Ԩ`(bar: Bar): Recommendation? = null
        fun `ԭ`(bar: Bar, recommendation: Recommendation, hint: String) {}
    }
    class WrongParameter {
        companion object {
            @JvmStatic fun `Ԩ`(bar: Any): Recommendation? = null
            @JvmStatic fun `ԭ`(bar: Bar, recommendation: Any, hint: String) {}
        }
    }
    class MissingBinder {
        companion object { @JvmStatic fun `Ԩ`(bar: Bar): Recommendation? = null }
    }
    private fun matches(layout: MarketHintLayout, helper: Class<*>) =
        layout.matches(Bar::class.java, helper, Recommendation::class.java)

    @Test fun `old helper layout remains accepted`() {
        assertTrue(matches(MarketHintLayout.verified[0], OldHelper::class.java))
    }
    @Test fun `new helper layout accepted with changed method names`() {
        assertTrue(matches(MarketHintLayout.verified[1], NewHelper::class.java))
    }
    @Test fun `new helper is not mistaken for old obfuscation layout`() {
        assertFalse(matches(MarketHintLayout.verified[0], NewHelper::class.java))
        assertFalse(matches(MarketHintLayout.verified[1], OldHelper::class.java))
    }
    @Test fun `class-name reuse with wrong types is rejected`() {
        for (helper in listOf(WrongReturn::class.java, WrongBinderReturn::class.java,
            NonStatic::class.java, WrongParameter::class.java, MissingBinder::class.java)) {
            assertFalse(helper.name, matches(MarketHintLayout.verified[1], helper))
        }
    }
}
