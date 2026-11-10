package com.luckyzyx.luckytool.diagnostics

import org.junit.Assert.*
import org.junit.Test

class ObservedFeatureTest {
    private val store = HookObservationStore()
    private val feature = ObservedFeature("key", true, store)
    private fun record() = store.snapshot().single()
    @Test fun enrollmentRecordsActualValueWithoutRegistration() {
        val other = HookObservationStore(); ObservedFeature("disabled", false, other)
        assertEquals(HookObservation.config(false), other.snapshot().single().config)
        assertEquals(0, record().registered); assertEquals(0, record().triggered)
    }
    @Test fun setupPreservesValueIdentityAndNull() {
        val token = Any()
        assertSame(token, feature.setup { token })
        assertNull(feature.setup { null })
        assertEquals(0, record().triggered)
    }
    @Test fun callbackPreservesValueIdentityAndNull() {
        val token = Any()
        assertSame(token, feature.callback { token })
        assertNull(feature.callback { null })
        assertEquals(2, record().triggered)
        assertEquals(0, record().registered)
    }
    @Test fun setupRethrowsIdenticalExceptionAndRecordsOnce() {
        val original = IllegalArgumentException("private")
        try { feature.setup { throw original }; fail("must throw") } catch (actual: IllegalArgumentException) { assertSame(original, actual) }
        assertEquals(1, record().failures); assertEquals(0, record().triggered)
    }
    @Test fun callbackRethrowsIdenticalExceptionAndRecordsEntry() {
        val original = AssertionError("private")
        try { feature.callback { throw original }; fail("must throw") } catch (actual: AssertionError) { assertSame(original, actual) }
        assertEquals(1, record().failures); assertEquals(1, record().triggered)
    }
    @Test fun inlineSetupAllowsNonLocalReturn() {
        fun returnEarly(): String {
            feature.setup { return "early" }
            return "unexpected"
        }
        assertEquals("early", returnEarly()); assertEquals(0, record().failures)
    }
    @Test fun earlyCallbackReturnStillCountsEntry() {
        assertEquals("skipped", feature.callback { return@callback "skipped" })
        assertEquals(1, record().triggered); assertEquals(0, record().changed)
    }
    @Test fun explicitCaughtFailureIsNotLostOrDoubleCounted() {
        feature.callback {
            try { throw IllegalStateException() } catch (error: IllegalStateException) { feature.failed(error) }
        }
        assertEquals(1, record().failures); assertEquals(1, record().triggered)
    }
    @Test fun defaultsNeverPresentUnmeasuredZeroChangesAsMeasured() {
        feature.callback { Unit }
        assertFalse(record().tracksChanges)
        val p = ProcessObservation("app", "app", "a".repeat(32), "build", store.snapshot())
        val s = HookObservation.feature("key", true, "build", listOf(p))
        assertEquals(ObservationState.TRIGGERED, s.state)
        assertFalse(s.detail.contains("执行修改"))
    }
}
