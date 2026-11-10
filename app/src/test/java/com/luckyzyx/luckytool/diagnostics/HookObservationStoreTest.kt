package com.luckyzyx.luckytool.diagnostics

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class HookObservationStoreTest {
    @Test fun capacityIsBoundedAndExistingRecordsStillUpdate() {
        val store = HookObservationStore()
        repeat(HookObservation.MAX_FEATURES + 20) { store.begin("key_$it", true) }
        store.triggered("key_0")
        assertEquals(HookObservation.MAX_FEATURES, store.snapshot().size)
        assertEquals(1, store.snapshot().first().triggered)
        assertFalse(store.snapshot().any { it.key == "key_${HookObservation.MAX_FEATURES}" })
    }
    @Test fun beginPreservesLoadedConfigAndCounters() {
        val store = HookObservationStore()
        store.begin("key", true)
        store.triggered("key")
        store.begin("key", false, tracksChanges = true)
        val f = store.snapshot().single()
        assertEquals(HookObservation.config(true), f.config)
        assertEquals(1, f.triggered)
        assertFalse(f.tracksChanges)
    }
    @Test fun updatesToUnknownKeysDoNotEnrollThem() {
        val store = HookObservationStore()
        store.triggered("missing"); store.changed("missing"); store.registered("missing")
        store.failed("missing", Error("secret"))
        assertTrue(store.snapshot().isEmpty())
    }
    @Test fun errorsExposeTypeOnlyAndConfigIsHashed() {
        val store = HookObservationStore()
        store.begin("key", "https://private.example/token")
        store.failed("key", IllegalArgumentException("secret message", Error("secret cause")))
        val f = store.snapshot().single()
        assertEquals("IllegalArgumentException", f.failure)
        assertFalse(f.toString().contains("secret"))
        assertFalse(f.toString().contains("private.example"))
        assertEquals(1, f.failures)
    }
    @Test fun anonymousThrowableHasSafeFallbackName() {
        val store = HookObservationStore(); store.begin("key", true)
        store.failed("key", object : Throwable("private") {})
        assertEquals("HookFailure", store.snapshot().single().failure)
    }
    @Test fun changeCannotExceedTriggerCount() {
        val store = HookObservationStore(); store.begin("key", true, tracksChanges = true)
        store.changed("key")
        assertEquals(0, store.snapshot().single().changed)
        store.triggered("key", changed = true); repeat(3) { store.changed("key") }
        assertEquals(1, store.snapshot().single().changed)
    }
    @Test fun snapshotIsDetachedFromLaterWrites() {
        val store = HookObservationStore(); store.begin("key", true)
        val old = store.snapshot(); store.triggered("key")
        assertEquals(0, old.single().triggered)
        assertEquals(1, store.snapshot().single().triggered)
    }
    @Test fun allCountersSaturateWithoutOverflow() {
        val store = HookObservationStore(); store.begin("key", true)
        // Seed a boundary state without billions of test iterations.
        val field = HookObservationStore::class.java.getDeclaredField("records").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val records = field.get(store) as MutableMap<String, FeatureObservation>
        records["key"] = records.getValue("key").copy(registered = Int.MAX_VALUE, triggered = Int.MAX_VALUE,
            changed = Int.MAX_VALUE, failures = Int.MAX_VALUE)
        store.registered("key"); store.triggered("key", true); store.changed("key"); store.failed("key", Error())
        val f = store.snapshot().single()
        assertEquals(Int.MAX_VALUE, f.registered); assertEquals(Int.MAX_VALUE, f.triggered)
        assertEquals(Int.MAX_VALUE, f.changed); assertEquals(Int.MAX_VALUE, f.failures)
    }
    @Test fun concurrentUpdatesAreNotLost() {
        val store = HookObservationStore(); store.begin("key", true)
        val pool = Executors.newFixedThreadPool(8); val start = CountDownLatch(1)
        try {
            val jobs = (0 until 8).map { pool.submit {
                start.await()
                repeat(250) { store.triggered("key"); store.failed("key", IllegalStateException()); store.snapshot() }
            } }
            start.countDown(); jobs.forEach { it.get(10, TimeUnit.SECONDS) }
            assertEquals(2000, store.snapshot().single().triggered)
            assertEquals(2000, store.snapshot().single().failures)
        } finally { pool.shutdownNow() }
    }
}
