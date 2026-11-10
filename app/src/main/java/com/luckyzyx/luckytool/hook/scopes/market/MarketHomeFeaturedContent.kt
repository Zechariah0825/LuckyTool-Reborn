package com.luckyzyx.luckytool.hook.scopes.market

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate
import java.util.Collections
import java.util.WeakHashMap

/**
 * Market 26.10.2_CN: cq2.ԫ(Fragment, dq2, CardApiRecycleViewAdapter) binds the
 * adapter BEFORE registering its loader callbacks. Both HomeChildCardFragment
 * (应用时刻) and HomeChosenCardFragment use this path. The presenter supplies full
 * CardDto lists through addDataAndNotifyChanged; replacement uses setDatas.
 * Filter there, before the native adapter expands groups into individual rows.
 */
@Obfuscate
class MarketHomeFeaturedContent : YukiBaseHooker() {
    private val homeAdapters = Collections.synchronizedMap(WeakHashMap<Any, Boolean>())
    private val hookedMethods = HashSet<java.lang.reflect.Method>()

    override fun onHook() {
        val binder = "a.a.a.cq2".toClassOrNull() ?: return
        try {
            binder.resolve().firstMethod {
                name = "\u052b"
                parameters(
                    "androidx.fragment.app.Fragment", "a.a.a.dq2",
                    "com.heytap.card.api.listener.CardApiRecycleViewAdapter",
                )
            }.hook {
                before {
                    val fragment = args[0] ?: return@before
                    val adapter = args[2] ?: return@before
                    val isHome = RebornMarketContentRules.isHomeFragment(
                        generateSequence(fragment.javaClass as Class<*>?) { it.superclass }
                            .map { it.name })
                    if (!isHome) {
                        homeAdapters.remove(adapter)
                        return@before
                    }
                    homeAdapters[adapter] = true
                    installAdapterHooks(adapter.javaClass)
                }
            }
        } catch (error: Throwable) {
            YLog.error("Reborn: Market home card binding changed", error)
        }
    }

    private fun installAdapterHooks(type: Class<*>) {
        listOf("addDataAndNotifyChanged", "setDatas").forEach { methodName ->
            try {
                // The API base methods are abstract; hook the actual implementation,
                // including the proxy used by the supplied APK, not the base stub.
                val method = type.getMethod(methodName, List::class.java)
                synchronized(hookedMethods) {
                    if (method in hookedMethods) return@forEach
                    method.declaringClass.resolve().firstMethod {
                        name = methodName
                        parameters(List::class)
                    }.hook {
                        before {
                            val original = args[0] as? List<*> ?: return@before
                            val filtered = RebornMarketContentRules.filterCards(
                                original, homeAdapters[instance] == true,
                                RebornMarketContentRules::cardTitle,
                            ) ?: return@before
                            args[0] = filtered
                        }
                    }
                    hookedMethods.add(method)
                }
            } catch (error: Throwable) {
                YLog.error("Reborn: Market home card adapter changed: $methodName", error)
            }
        }
    }
}
