package com.luckyzyx.luckytool.diagnostics

import com.highcapable.yukihookapi.hook.core.YukiHookCreator
import com.highcapable.yukihookapi.hook.param.HookParam

/** One explicitly attributed setting. Never enroll always-loaded hooks with an invented true. */
class ObservedFeature(val key: String, value: Any, private val store: HookObservationStore = HookDiagnostics.store) {
    init { store.begin(key, value) }
    inline fun <T> setup(block: () -> T): T = try { block() } catch (error: Throwable) { failed(error); throw error }
    fun <T> callback(block: () -> T): T {
        store.triggered(key)
        return try { block() } catch (error: Throwable) { store.failed(key, error); throw error }
    }
    fun failed(error: Throwable) = store.failed(key, error)
}
fun YukiHookCreator.ClassicMemberHooker.beforeObserved(feature: ObservedFeature, block: HookParam.() -> Unit) =
    before { feature.callback { block(this) } }
fun YukiHookCreator.ClassicMemberHooker.afterObserved(feature: ObservedFeature, block: HookParam.() -> Unit) =
    after { feature.callback { block(this) } }
fun YukiHookCreator.ClassicMemberHooker.interceptObserved(feature: ObservedFeature, value: Any? = null) =
    intercept { feature.callback { value } }
fun YukiHookCreator.ClassicMemberHooker.interceptObserved(feature: ObservedFeature, block: HookParam.() -> Any?) =
    intercept { feature.callback { block(this) } }
