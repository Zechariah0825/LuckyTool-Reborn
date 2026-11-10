package com.luckyzyx.luckytool.diagnostics

import java.security.MessageDigest

data class FeatureObservation(
    val key: String, val config: String, val registered: Int = 0, val triggered: Int = 0,
    val changed: Int = 0, val failures: Int = 0, val failure: String = "", val tracksChanges: Boolean = false,
)
data class ProcessObservation(
    val packageName: String, val processName: String, val session: String, val build: String,
    val features: List<FeatureObservation>, val hostBuild: String = "",
)
data class ReceivedObservation(val observation: ProcessObservation, val receivedAt: Long)
enum class ObservationState { DISABLED, UNKNOWN, PROCESS_LOADED, CONFIG_CHANGED, BUILD_CHANGED, HOST_CHANGED, REGISTERED, TRIGGERED, FAILED }
data class FeatureStatus(val state: ObservationState, val detail: String)

/** Evidence levels, never a verdict about final user-visible behavior. */
object HookObservation {
    const val MAX_FEATURES = 128
    const val FRESH_MS = 15_000L
    val instrumentedKeys: Set<String> get() = HookDiagnosticCoverage.keys
    fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun config(value: Any): String = fingerprint(when (value) {
        is Boolean -> "boolean:$value"
        is String -> "string:$value"
        is Int -> "int:$value"
        else -> error("Unsupported observation setting type")
    })
    fun fresh(reports: List<ReceivedObservation>, now: Long): List<ProcessObservation> = reports
        .filter { now >= it.receivedAt && now - it.receivedAt <= FRESH_MS }.map { it.observation }
    fun feature(key: String, value: Any, expectedBuild: String, reports: List<ProcessObservation>,
                expectedHostBuilds: Map<String, String> = emptyMap()): FeatureStatus {
        if (value == false) return FeatureStatus(ObservationState.DISABLED, "配置已关闭；已运行进程可能需重启")
        if (reports.isEmpty()) return FeatureStatus(ObservationState.UNKNOWN, "未知：未收到目标进程回报，不能据此判定未生效")
        if (reports.any { report -> expectedHostBuilds[report.packageName]?.let { it != report.hostBuild } == true })
            return FeatureStatus(ObservationState.HOST_CHANGED, "目标应用安装版本已变化或无法匹配，请重启作用域后检测")
        val compatible = reports.filter { it.build == expectedBuild }
        if (compatible.isEmpty() || reports.any { r -> r.build != expectedBuild && r.features.any { it.key == key } })
            return FeatureStatus(ObservationState.BUILD_CHANGED, "目标进程仍为其他模块版本，请重启作用域后检测")
        // Shared-UID package namespaces report the same in-process store. Check all
        // versions/configurations first, then merge cumulative snapshots by session.
        val entriesBySession = compatible.groupBy { it.processName to it.session }.values.map { processReports ->
            processReports.flatMap { it.features.filter { f -> f.key == key } }
        }.filter { it.isNotEmpty() }
        val entries = entriesBySession.flatten()
        if (entries.isEmpty()) return FeatureStatus(ObservationState.PROCESS_LOADED,
            if (key in instrumentedKeys) "模块已加载；此项未加载或尚无回报，可能需重启作用域"
            else "模块已加载；此项尚未接入检测，效果未知")
        if (entries.any { it.config != config(value) }) return FeatureStatus(ObservationState.CONFIG_CHANGED,
            "目标进程配置与当前不同，请重启作用域后检测")
        val matching = entriesBySession.map { snapshots ->
            snapshots.first().copy(
                registered = snapshots.maxOf { it.registered }, triggered = snapshots.maxOf { it.triggered },
                changed = snapshots.maxOf { it.changed }, failures = snapshots.maxOf { it.failures },
                failure = snapshots.maxBy { it.failures }.failure, tracksChanges = snapshots.all { it.tracksChanges },
            )
        }
        val failures = matching.sumOf { it.failures.toLong() }
        if (failures > 0) return FeatureStatus(ObservationState.FAILED,
            "捕获失败记录 $failures 条（${matching.first { it.failures > 0 }.failure}）；其他路径可能仍工作")
        val hits = matching.sumOf { it.triggered.toLong() }
        val changed = matching.sumOf { it.changed.toLong() }
        if (hits > 0) return FeatureStatus(ObservationState.TRIGGERED,
            if (matching.all { it.tracksChanges }) "本次进程会话已触发 $hits 次，执行修改 $changed 次；界面效果仍需确认"
            else "本次进程会话回调进入 $hits 次；尚未检测最终修改或界面效果")
        val registered = matching.sumOf { it.registered.toLong() }
        if (registered > 0) return FeatureStatus(ObservationState.REGISTERED,
            "已注册 $registered 个 Hook，尚未触发；请使用目标功能后返回查看")
        return FeatureStatus(ObservationState.PROCESS_LOADED, "模块已加载；此项尚无注册或触发证据")
    }
}
