package com.luckyzyx.luckytool.ui.compose.components

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.luckyzyx.luckytool.diagnostics.HookDiagnosticClient
import com.luckyzyx.luckytool.diagnostics.HookDiagnostics
import com.luckyzyx.luckytool.diagnostics.HookObservation
import com.luckyzyx.luckytool.diagnostics.ProcessObservation
import kotlinx.coroutines.delay

internal val LocalHookDiagnosticUi = compositionLocalOf<HookDiagnosticUi?> { null }
internal data class HookDiagnosticUi(val build: String, val reports: List<ProcessObservation>,
                                     val hostBuilds: Map<String, String> = emptyMap()) {
    val moduleSummary: String get() = if (reports.isEmpty()) {
        "未知：未收到目标进程回报。请确认 LSPosed 作用域、重启并打开目标应用后返回。未回报不等于未生效。"
    } else {
        val sessions = reports.groupBy { it.processName to it.session }.values
        val current = sessions.count { snapshots -> snapshots.all { it.build == build } }
        "最近 15 秒内收到 ${sessions.size} 个进程回报，其中 $current 个匹配当前模块。" +
            "加载、注册、触发均不代表最终界面效果；未接入检测的项目会明确显示未知。"
    }
    fun summary(key: String, value: Any, original: String?): String = listOfNotNull(
        original?.takeIf { it.isNotBlank() }, "检测：" + HookObservation.feature(key, value, build, reports, hostBuilds).detail,
    ).joinToString("\n")
}
@Composable
internal fun rememberHookDiagnosticUi(packages: List<String>): HookDiagnosticUi {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val targets = remember(packages) { packages.map { if (it == "system") "android" else it }.filter { it.isNotBlank() }.distinct() }
    val build = remember(context) { HookDiagnostics.buildFingerprint(context.applicationInfo.sourceDir) }
    val observations by HookDiagnosticClient.observations.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(targets, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = SystemClock.elapsedRealtime()
                targets.forEach { HookDiagnosticClient.request(context, it) }
                delay(3000)
            }
        }
    }
    // Re-read installed paths on every poll, so an APK replacement while this page
    // remains open invalidates positive evidence from the old host process.
    val hostBuilds = remember(targets, now) { targets.mapNotNull { target ->
        runCatching {
            @Suppress("DEPRECATION")
            val path = context.packageManager.getApplicationInfo(target, 0).sourceDir.orEmpty()
            target to HookObservation.fingerprint(path)
        }.getOrNull()
    }.toMap() }
    return HookDiagnosticUi(build, targets.flatMap { HookObservation.fresh(observations[it].orEmpty(), now) }, hostBuilds)
}
