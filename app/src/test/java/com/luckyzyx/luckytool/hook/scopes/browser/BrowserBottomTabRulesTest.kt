package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserBottomTabRulesTest {
    @Test fun `actual default integer list removes video and novels`() {
        assertEquals(listOf(1, 9, 6), BrowserBottomTabRules.filter(listOf(1, 2, 9, 8, 6)) { it })
    }
    @Test fun `identity filtering preserves reordered surviving tabs`() {
        assertEquals(listOf(6, 9, 1), BrowserBottomTabRules.filter(listOf(8, 6, 2, 9, 1)) { it })
    }
    @Test fun `compact list and unrecognized model are untouched`() {
        assertNull(BrowserBottomTabRules.filter(listOf(1, 9, 6)) { it })
        assertNull(BrowserBottomTabRules.filter(listOf(1, 2, 8, 6)) { it })
        assertNull(BrowserBottomTabRules.filter(listOf(1, 2, 9, 8, 6)) { if (it == 8) null else it })
    }
}
