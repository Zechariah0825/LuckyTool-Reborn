package com.luckyzyx.luckytool.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.components.HookDiagnosticUi
import com.luckyzyx.luckytool.ui.compose.components.PrefScopeBuilder
import com.luckyzyx.luckytool.utils.PrefState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HookDiagnosticUiTest {
    private val ui = HookDiagnosticUi("build", emptyList())
    private fun builder(): PrefScopeBuilder {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val raw = context.getSharedPreferences("hook_diagnostic_ui_test", Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        return PrefScopeBuilder(PrefState.ofProvider { raw }).apply { this.context = context; diagnostics = ui }
    }
    @Test fun originalSummaryPreservedWithOneSeparateDiagnosticLine() {
        val original = "原说明第一行\n原说明第二行"
        val summary = ui.summary("key", true, original)
        assertTrue(summary.startsWith("$original\n检测："))
        assertEquals(1, Regex("检测：").findAll(summary).count())
    }
    @Test fun nullOrBlankSummaryHasNoLeadingEmptyLine() {
        for (original in listOf(null, "", "   ")) assertTrue(ui.summary("key", true, original).startsWith("检测："))
    }
    @Test fun disabledSummaryPreservesRestartWarning() {
        assertTrue(ui.summary("key", false, "说明").contains("进程可能需重启"))
    }
    @Test fun moduleSummaryDoesNotClaimSuccessWithoutReport() {
        assertTrue(ui.moduleSummary.contains("未回报不等于未生效"))
    }
    @Test fun mixedBuildModuleSummaryCountsOnlyCompatibleReports() {
        val p = ProcessObservation("app", "app", "session", "build", emptyList())
        val text = HookDiagnosticUi("build", listOf(p, p.copy(build = "old", processName = "app:worker"))).moduleSummary
        assertTrue(text.contains("2 个进程回报")); assertTrue(text.contains("1 个匹配当前模块"))
        assertTrue(text.contains("均不代表最终界面效果"))
    }
    @Test fun diagnosticHeadingAndStatusDoNotPolluteSearchIndex() {
        val b = builder(); b.beginBuild()
        b.switch("switch_key", "开关标题", "原始摘要")
        b.list("list_key", "列表标题", arrayOf("选项"), arrayOf("1"), summary = "当前 %s")
        b.computeSegments()
        assertEquals(3, b.entries.size)
        assertEquals(listOf("switch_key", "list_key"), b.entries.mapNotNull { it.indexKey })
        assertEquals(listOf("开关标题", "列表标题"), b.entries.filter { it.indexKey != null }.map { it.indexTitle })
        assertEquals(listOf("原始摘要", "当前 %s"), b.entries.filter { it.indexKey != null }.map { it.indexSummary })
        assertEquals(2, b.snapshotIndex().size)
        assertTrue(b.entries.first().indexKey == null)
    }
    @Test fun repeatedRebuildAddsExactlyOneHeadingAndOneRow() {
        val b = builder()
        repeat(5) {
            b.beginBuild(); b.switch("key", "标题", "摘要"); b.computeSegments()
            assertEquals(2, b.entries.size)
            assertEquals(1, b.entries.count { it.indexKey == null })
            assertEquals(1, b.snapshotIndex().size)
        }
    }
    @Test fun disablingDiagnosticsRemovesHeadingOnNextBuild() {
        val b = builder(); b.beginBuild(); b.switch("key", "标题")
        assertEquals(2, b.entries.size)
        b.diagnostics = null; b.beginBuild(); b.switch("key", "标题")
        assertEquals(1, b.entries.size)
        assertEquals(1, b.snapshotIndex().size)
    }
    @Test fun sharedProcessAliasesCountAsOneProcess() {
        val p = ProcessObservation("app", "shared.process", "session", "build", emptyList())
        val text = HookDiagnosticUi("build", listOf(p, p.copy(packageName = "other.app"))).moduleSummary
        assertTrue(text.contains("1 个进程回报")); assertTrue(text.contains("1 个匹配当前模块"))
    }
    @Test fun sharedProcessAliasWithMixedBuildCannotCountAsCurrent() {
        val p = ProcessObservation("app", "shared.process", "session", "build", emptyList())
        val text = HookDiagnosticUi("build", listOf(p, p.copy(packageName = "other.app", build = "old"))).moduleSummary
        assertTrue(text.contains("1 个进程回报")); assertTrue(text.contains("0 个匹配当前模块"))
    }

}
