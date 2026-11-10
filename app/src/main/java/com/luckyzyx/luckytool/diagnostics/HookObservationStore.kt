package com.luckyzyx.luckytool.diagnostics

/** Bounded per-process memory. Feature callbacks do no disk/Binder/network work. */
class HookObservationStore {
    private val records = linkedMapOf<String, FeatureObservation>()
    @Synchronized fun begin(key: String, value: Any, tracksChanges: Boolean = false) {
        if (key !in records && records.size >= HookObservation.MAX_FEATURES) return
        records.putIfAbsent(key, FeatureObservation(key, HookObservation.config(value), tracksChanges = tracksChanges))
    }
    /** Only call with a real receipt. Yuki 1.5.0 DSL return is not such a receipt. */
    @Synchronized fun registered(key: String) = update(key) { it.copy(registered = increment(it.registered)) }
    @Synchronized fun triggered(key: String, changed: Boolean = false) = update(key) {
        it.copy(triggered = increment(it.triggered), changed = if (changed) increment(it.changed) else it.changed)
    }
    @Synchronized fun changed(key: String) = update(key) { it.copy(changed = increment(it.changed).coerceAtMost(it.triggered)) }
    @Synchronized fun failed(key: String, error: Throwable) = update(key) {
        it.copy(failures = increment(it.failures), failure = error.javaClass.simpleName
            .replace(Regex("[^A-Za-z0-9_.$-]"), "_").take(100).ifBlank { "HookFailure" })
    }
    @Synchronized fun snapshot(): List<FeatureObservation> = records.values.toList()
    private fun update(key: String, change: (FeatureObservation) -> FeatureObservation) { records[key]?.let { records[key] = change(it) } }
    private fun increment(value: Int) = if (value == Int.MAX_VALUE) value else value + 1
}
