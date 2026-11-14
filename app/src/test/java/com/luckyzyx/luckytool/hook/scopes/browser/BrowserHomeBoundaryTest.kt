package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserHomeBoundaryTest {
    private class EqualObject {
        override fun equals(other: Any?) = other is EqualObject
        override fun hashCode() = 1
    }

    @Test fun `matching url or equal params cannot impersonate module request`() {
        val boundary = BrowserHomeBoundary()
        val params = EqualObject()
        val request = BrowserHomeBoundary.Request(Any(), Any(), 7)
        boundary.track(params, request)
        assertSame(request, boundary.request(params))
        assertNull(boundary.request(EqualObject()))
    }

    @Test fun `only captured first web page acts as logical Home`() {
        val boundary = BrowserHomeBoundary()
        val tab = Any(); val home = Any(); val page = Any()
        assertTrue(boundary.capture(BrowserHomeBoundary.Request(tab, home, 0), tab, home, page, 1))
        assertTrue(boundary.isRoot(home, page, 1, true))
        assertFalse(boundary.isRoot(home, page, 2, true))
        assertFalse(boundary.isRoot(home, page, 0, true))
        assertFalse(boundary.isRoot(home, Any(), 1, true))
        assertFalse(boundary.isRoot(Any(), page, 1, true))
        assertFalse(boundary.isRoot(home, page, 1, false))
    }

    @Test fun `wrong owner deeper history and Home anchor cannot be captured`() {
        val boundary = BrowserHomeBoundary()
        val tab = Any(); val home = Any(); val page = Any()
        val request = BrowserHomeBoundary.Request(tab, home, 0)
        assertFalse(boundary.capture(request, Any(), home, page, 1))
        assertFalse(boundary.capture(request, tab, Any(), page, 1))
        assertFalse(boundary.capture(request, tab, home, home, 1))
        assertFalse(boundary.capture(request, tab, home, page, 2))
        assertFalse(boundary.isRoot(home, page, 1, true))
    }

    @Test fun `explicit Home can replace its root without changing another tab`() {
        val boundary = BrowserHomeBoundary()
        val tab = Any(); val home = Any(); val page = Any(); val replacement = Any()
        val otherTab = Any(); val otherHome = Any(); val otherPage = Any()
        boundary.capture(BrowserHomeBoundary.Request(tab, home, 0), tab, home, page, 1)
        boundary.capture(BrowserHomeBoundary.Request(otherTab, otherHome, 0), otherTab, otherHome, otherPage, 1)
        boundary.capture(BrowserHomeBoundary.Request(tab, home, 1), tab, home, replacement, 1)
        assertFalse(boundary.isRoot(home, page, 1, true))
        assertTrue(boundary.isRoot(home, replacement, 1, true))
        assertTrue(boundary.isRoot(otherHome, otherPage, 1, true))
    }

    @Test fun `Back has no mutation or automatic redispatch and returning from deeper history still works`() {
        val boundary = BrowserHomeBoundary()
        val tab = Any(); val home = Any(); val page = Any()
        boundary.capture(BrowserHomeBoundary.Request(tab, home, 0), tab, home, page, 1)
        repeat(3) { assertTrue(boundary.isRoot(home, page, 1, true)) }
        assertFalse(boundary.isRoot(home, Any(), 2, true))
        assertTrue(boundary.isRoot(home, page, 1, true))
    }

    @Test fun `root and home comparisons are identity based`() {
        val boundary = BrowserHomeBoundary()
        val tab = Any(); val home = EqualObject(); val page = EqualObject()
        boundary.capture(BrowserHomeBoundary.Request(tab, home, 0), tab, home, page, 1)
        assertFalse(boundary.isRoot(EqualObject(), page, 1, true))
        assertFalse(boundary.isRoot(home, EqualObject(), 1, true))
    }
    @Test fun `successful return forgets reused page until a new owned request captures it`() {
        val boundary = BrowserHomeBoundary(); val tab = Any(); val home = Any(); val page = Any()
        val other = Any(); val otherPage = Any()
        boundary.capture(BrowserHomeBoundary.Request(tab, home, 0), tab, home, page, 1)
        boundary.capture(BrowserHomeBoundary.Request(tab, other, 0), tab, other, otherPage, 1)
        boundary.forget(home)
        assertFalse(boundary.isRoot(home, page, 1, true))
        assertTrue(boundary.isRoot(other, otherPage, 1, true))
        boundary.capture(BrowserHomeBoundary.Request(tab, home, 1), tab, home, page, 1)
        assertTrue(boundary.isRoot(home, page, 1, true))
    }

}
