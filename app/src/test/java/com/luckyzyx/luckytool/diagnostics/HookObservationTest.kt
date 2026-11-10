package com.luckyzyx.luckytool.diagnostics

import org.junit.Assert.*
import org.junit.Test

class HookObservationTest {
    private val key = "remove_holiday_page_information_flow"
    private val build = HookObservation.fingerprint("build")
    private fun entry(value: Any = true) = FeatureObservation(key, HookObservation.config(value))
    private fun process(features: List<FeatureObservation> = listOf(entry()), version: String = build) =
        ProcessObservation("app.test", "app.test", "a".repeat(32), version, features)
    private fun status(vararg reports: ProcessObservation, value: Any = true, target: String = key) =
        HookObservation.feature(target, value, build, reports.toList())

    @Test fun disabledWinsEvenOverOldBuildAndFailure() {
        val s = status(process(listOf(entry().copy(failures = 1)), "old"), value = false)
        assertEquals(ObservationState.DISABLED, s.state)
        assertTrue(s.detail.contains("进程可能需重启"))
        assertFalse(s.detail.contains("已卸载"))
    }
    @Test fun noReportsMeansUnknownNotFailure() {
        val s = status()
        assertEquals(ObservationState.UNKNOWN, s.state)
        assertTrue(s.detail.contains("不能据此判定未生效"))
    }
    @Test fun loadedUninstrumentedSettingExplicitlySaysUnknown() {
        val s = status(process(emptyList()), target = "not_in_catalog")
        assertEquals(ObservationState.PROCESS_LOADED, s.state)
        assertTrue(s.detail.contains("尚未接入检测"))
    }
    @Test fun loadedInstrumentedSettingWithoutEntryDoesNotClaimRegistration() {
        val s = status(process(emptyList()))
        assertEquals(ObservationState.PROCESS_LOADED, s.state)
        assertTrue(s.detail.contains("未加载或尚无回报"))
    }
    @Test fun setupAloneIsOnlyProcessLoaded() {
        assertEquals(ObservationState.PROCESS_LOADED, status(process()).state)
    }
    @Test fun registeredRequiresActualCount() {
        assertEquals(ObservationState.REGISTERED, status(process(listOf(entry().copy(registered = 1)))).state)
    }
    @Test fun triggeredWinsOverRegistered() {
        assertEquals(ObservationState.TRIGGERED, status(process(listOf(entry().copy(registered = 4, triggered = 1)))).state)
    }
    @Test fun failureWinsOverTriggeredAndRegistered() {
        val s = status(process(listOf(entry().copy(registered = 1, triggered = 2, failures = 1, failure = "LookupError"))))
        assertEquals(ObservationState.FAILED, s.state)
        assertTrue(s.detail.contains("LookupError"))
    }
    @Test fun configMismatchWinsOverFailure() {
        assertEquals(ObservationState.CONFIG_CHANGED, status(process(listOf(entry(false).copy(failures = 1)))).state)
    }
    @Test fun oldBuildWinsOverConfigMismatch() {
        assertEquals(ObservationState.BUILD_CHANGED, status(process(listOf(entry(false)), "old")).state)
    }
    @Test fun mixedConfigsCannotBeHiddenByOneMatchingProcess() {
        assertEquals(ObservationState.CONFIG_CHANGED, status(process(), process(listOf(entry(false)))).state)
    }
    @Test fun relevantOldProcessCannotBeHiddenByNewProcess() {
        assertEquals(ObservationState.BUILD_CHANGED, status(process(), process(version = "old")).state)
    }
    @Test fun unrelatedOldProcessDoesNotInvalidateCurrentFeature() {
        assertEquals(ObservationState.TRIGGERED, status(process(listOf(entry().copy(triggered = 1))), process(emptyList(), "old")).state)
    }
    @Test fun allOldReportsEvenWithoutFeatureNeedRestart() {
        assertEquals(ObservationState.BUILD_CHANGED, status(process(emptyList(), "old")).state)
    }
    @Test fun aggregateCountersUseLongRatherThanOverflow() {
        val p = process(listOf(entry().copy(triggered = Int.MAX_VALUE)))
        assertTrue(status(p, p.copy(processName = "app.test:worker")).detail.contains("4294967294"))
        val f = process(listOf(entry().copy(failures = Int.MAX_VALUE, failure = "Error")))
        assertTrue(status(f, f.copy(processName = "app.test:worker")).detail.contains("4294967294"))
        val r = process(listOf(entry().copy(registered = Int.MAX_VALUE)))
        assertTrue(status(r, r.copy(processName = "app.test:worker")).detail.contains("4294967294"))
    }
    @Test fun ordinaryCallbacksNeverClaimZeroMutations() {
        val s = status(process(listOf(entry().copy(triggered = 1))))
        assertTrue(s.detail.contains("回调进入 1 次"))
        assertFalse(s.detail.contains("执行修改"))
    }
    @Test fun explicitChangeTrackingReportsActualChanges() {
        val s = status(process(listOf(entry().copy(triggered = 3, changed = 2, tracksChanges = true))))
        assertTrue(s.detail.contains("执行修改 2 次"))
        assertTrue(s.detail.contains("界面效果仍需确认"))
    }
    @Test fun mixedTrackingDoesNotClaimCompleteChangeCounts() {
        val p = process(listOf(entry().copy(triggered = 1, tracksChanges = true)))
        assertFalse(status(p, process(listOf(entry().copy(triggered = 1)))).detail.contains("执行修改"))
    }
    @Test fun freshnessIncludesBoundaryAndRejectsFutureAndExpired() {
        val p = process()
        val reports = listOf(ReceivedObservation(p, 0), ReceivedObservation(p, 1), ReceivedObservation(p, 15_002))
        assertEquals(listOf(p), HookObservation.fresh(reports, 15_001))
        assertEquals(listOf(p), HookObservation.fresh(listOf(ReceivedObservation(p, 0)), 15_000))
    }
    @Test fun expiredReportsReturnUnknown() {
        val fresh = HookObservation.fresh(listOf(ReceivedObservation(process(), 0)), 15_001)
        assertEquals(ObservationState.UNKNOWN, HookObservation.feature(key, true, build, fresh).state)
    }
    @Test fun configHashesAreTypedAndDoNotContainSecrets() {
        assertNotEquals(HookObservation.config(true), HookObservation.config("true"))
        assertNotEquals(HookObservation.config(1), HookObservation.config("1"))
        assertEquals(HookObservation.config(true), HookObservation.config(true))
        assertTrue(HookObservation.config("secret-url").matches(Regex("[a-f0-9]{64}")))
        assertFalse(HookObservation.config("secret-url").contains("secret"))
    }
    @Test(expected = IllegalStateException::class) fun unsupportedConfigTypeRejected() { HookObservation.config(1.5) }

    private val currentHost = HookObservation.fingerprint("/data/app/new/base.apk")
    private val oldHost = HookObservation.fingerprint("/data/app/old/base.apk")
    private fun hostStatus(reports: List<ProcessObservation>, value: Any = true,
                           hosts: Map<String, String> = mapOf("app.test" to currentHost)) =
        HookObservation.feature(key, value, build, reports, hosts)
    private fun hitHost(host: String = currentHost) =
        process(listOf(entry().copy(triggered = 1))).copy(hostBuild = host)

    @Test fun updatedHostOverridesOldProcessTriggerEvidence() {
        val s = hostStatus(listOf(hitHost(oldHost)))
        assertEquals(ObservationState.HOST_CHANGED, s.state)
        assertTrue(s.detail.contains("重启"))
    }
    @Test fun matchingHostPreservesTriggerEvidence() {
        assertEquals(ObservationState.TRIGGERED, hostStatus(listOf(hitHost())).state)
    }
    @Test fun missingReportedHostWithKnownInstalledHostNeedsRestart() {
        assertEquals(ObservationState.HOST_CHANGED, hostStatus(listOf(hitHost(""))).state)
    }
    @Test fun missingInstalledHostFingerprintDoesNotInventMismatch() {
        assertEquals(ObservationState.TRIGGERED, hostStatus(listOf(hitHost(oldHost)), hosts = emptyMap()).state)
        assertEquals(ObservationState.TRIGGERED, hostStatus(listOf(hitHost("")), hosts = emptyMap()).state)
    }
    @Test fun disabledStillWinsOverHostMismatchAndFailure() {
        val p = process(listOf(entry().copy(failures = 1))).copy(hostBuild = oldHost)
        assertEquals(ObservationState.DISABLED, hostStatus(listOf(p), value = false).state)
    }
    @Test fun oneMatchingProcessCannotHideStaleHostProcess() {
        val old = hitHost(oldHost).copy(processName = "app.test:worker")
        assertEquals(ObservationState.HOST_CHANGED, hostStatus(listOf(hitHost(), old)).state)
        assertEquals(ObservationState.HOST_CHANGED, hostStatus(listOf(old, hitHost())).state)
    }
    @Test fun allMatchingHostProcessesAggregateNormally() {
        val second = hitHost().copy(processName = "app.test:worker")
        val s = hostStatus(listOf(hitHost(), second))
        assertEquals(ObservationState.TRIGGERED, s.state)
        assertTrue(s.detail.contains("2 次"))
    }
    @Test fun hostFingerprintsAreMatchedByPackageNotProcessName() {
        val other = hitHost(oldHost).copy(packageName = "other.app", processName = "other.app:worker")
        val hosts = mapOf("app.test" to currentHost, "other.app" to oldHost)
        assertEquals(ObservationState.TRIGGERED, hostStatus(listOf(hitHost(), other), hosts = hosts).state)
        assertEquals(ObservationState.HOST_CHANGED,
            hostStatus(listOf(hitHost(), other.copy(hostBuild = currentHost)), hosts = hosts).state)
    }
    @Test fun hostMismatchPrecedesModuleAndConfigMismatch() {
        val p = process(listOf(entry(false)), "old-module").copy(hostBuild = oldHost)
        assertEquals(ObservationState.HOST_CHANGED, hostStatus(listOf(p)).state)
    }
    @Test fun installedHostWithoutAnyReportRemainsUnknown() {
        assertEquals(ObservationState.UNKNOWN, hostStatus(emptyList()).state)
    }
    @Test fun sharedProcessAliasesDoNotDoubleCountTriggersOrChanges() {
        val p = process(listOf(entry().copy(triggered = 5, changed = 2, tracksChanges = true)))
        val alias = p.copy(packageName = "shared.app", features = listOf(entry().copy(triggered = 8, changed = 3, tracksChanges = true)))
        for (reports in listOf(listOf(p, alias), listOf(alias, p))) {
            val text = status(*reports.toTypedArray()).detail
            assertTrue(text.contains("触发 8 次")); assertTrue(text.contains("执行修改 3 次"))
        }
    }
    @Test fun sharedProcessAliasesDoNotDoubleCountFailuresOrRegistrations() {
        val f = process(listOf(entry().copy(failures = 2, failure = "LookupError")))
        val failure = status(f, f.copy(packageName = "shared.app")).detail
        assertTrue(failure.contains("失败记录 2 条"))
        val r = process(listOf(entry().copy(registered = 3)))
        assertTrue(status(r, r.copy(packageName = "shared.app")).detail.contains("已注册 3 个"))
    }
    @Test fun sameProcessNameWithDifferentSessionsRemainsSeparateEvidence() {
        val p = process(listOf(entry().copy(triggered = 2)))
        assertTrue(status(p, p.copy(session = "b".repeat(32))).detail.contains("进入 4 次"))
    }
    @Test fun sharedProcessAliasCannotHideConfigMismatch() {
        val p = process(listOf(entry().copy(triggered = 4)))
        val alias = p.copy(packageName = "shared.app", features = listOf(entry(false)))
        assertEquals(ObservationState.CONFIG_CHANGED, status(p, alias).state)
    }
    @Test fun sharedProcessAliasCannotHideHostMismatch() {
        val p = hitHost()
        val alias = p.copy(packageName = "shared.app", hostBuild = oldHost)
        assertEquals(ObservationState.HOST_CHANGED,
            hostStatus(listOf(p, alias), hosts = mapOf("app.test" to currentHost, "shared.app" to currentHost)).state)
    }

}
