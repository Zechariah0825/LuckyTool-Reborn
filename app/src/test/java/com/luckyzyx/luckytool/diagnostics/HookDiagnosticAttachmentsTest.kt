package com.luckyzyx.luckytool.diagnostics

import org.junit.Assert.*
import org.junit.Test

class HookDiagnosticAttachmentsTest {
    @Test fun repeatedPackageRegistersExactlyOnce() {
        val attachments = HookDiagnosticAttachments()
        var count = 0
        repeat(3) { attachments.attach("app", "shared.process") { count++ } }
        assertEquals(1, count)
    }
    @Test fun sharedProcessRegistersEachPackageWithTheSameSession() {
        val attachments = HookDiagnosticAttachments()
        val sessions = mutableListOf<String>()
        attachments.attach("first.app", "shared.process") { sessions += it }
        attachments.attach("second.app", "shared.process") { sessions += it }
        assertEquals(2, sessions.size)
        assertEquals(sessions[0], sessions[1])
        assertTrue(sessions[0].matches(Regex("[a-f0-9]{32}")))
    }
    @Test fun differentProcessesHaveDifferentSessions() {
        val attachments = HookDiagnosticAttachments()
        val sessions = mutableListOf<String>()
        attachments.attach("app", "app") { sessions += it }
        attachments.attach("app", "app:worker") { sessions += it }
        assertNotEquals(sessions[0], sessions[1])
    }
    @Test fun failedRegistrationCanRetryWithoutChangingSession() {
        val attachments = HookDiagnosticAttachments()
        var failedSession = ""
        val expected = IllegalStateException("setup")
        try {
            attachments.attach("app", "shared.process") { failedSession = it; throw expected }
            fail("Expected setup exception")
        } catch (error: IllegalStateException) { assertSame(expected, error) }
        var successSession = ""
        attachments.attach("app", "shared.process") { successSession = it }
        assertEquals(failedSession, successSession)
        attachments.attach("app", "shared.process") { fail("Must not register again") }
    }
}
