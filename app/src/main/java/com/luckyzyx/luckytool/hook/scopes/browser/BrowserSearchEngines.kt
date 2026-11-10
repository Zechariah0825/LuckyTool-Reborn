package com.luckyzyx.luckytool.hook.scopes.browser

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate
import java.util.WeakHashMap

/**
 * OPPO Browser 40.10.25.2 search model mapping, verified against supplied DEX:
 * api.search.engine.e(label,key,int,searchUrl,sugUrl, ..., boolean)
 * search.impl.engine.k(Context,e), search.impl.engine.c(Context,k).
 * The native engine replaces {searchTerms} with an encoded search phrase.
 */
@Obfuscate
class BrowserSearchEngines(
    private val extendNativeList: Boolean,
    private val selectedProvider: String,
    private val customName: String,
    private val customTemplate: String,
    private val customEntries: String,
) : YukiBaseHooker() {
    private val cache = WeakHashMap<Any, List<Any>>()

    override fun onHook() {
        val model = if (extendNativeList)
            "com.heytap.browser.search.impl.engine.DefaultSearchEngines".toClassOrNull()
        else null
        if (model != null) {
            listOf("M", "Q", "d0", "e0", "g0", "k", "n").forEach { methodName ->
                try {
                    val isBooleanList = methodName == "k" || methodName == "n"
                    if (model.declaredMethods.none {
                        it.name == methodName && List::class.java.isAssignableFrom(it.returnType) &&
                            (if (isBooleanList) it.parameterTypes.contentEquals(arrayOf(Boolean::class.javaPrimitiveType))
                            else it.parameterCount == 0)
                    }) return@forEach
                    model.resolve().firstMethod {
                        name = methodName
                        if (isBooleanList) parameters(Boolean::class) else parameters()
                    }.hook {
                        after {
                            val list = result as? List<*> ?: return@after
                            if (list.isEmpty() || list.any {
                                    it == null || !it.javaClass.name.startsWith("com.heytap.browser.search.impl.engine.c")
                                }) return@after
                            val owner = instance ?: return@after
                            val newEngines = synchronized(cache) {
                                cache[owner] ?: createModels(owner).also { cache[owner] = it }
                            }
                            if (newEngines.isEmpty()) return@after
                            val existing = list.mapNotNull { engineKey(it) }.toSet()
                            val missing = newEngines.filter { engineKey(it) !in existing }
                            if (missing.isNotEmpty()) result = ArrayList<Any>(list.filterNotNull()).apply { addAll(missing) }
                        }
                    }
                } catch (error: Throwable) {
                    YLog.error("Reborn: search list hook mismatch: $methodName", error)
                }
            }
            listOf("r", "u", "x").forEach { methodName ->
                try {
                    if (model.declaredMethods.none {
                        it.name == methodName && it.parameterTypes.contentEquals(arrayOf(String::class.java)) &&
                            it.returnType.name == "com.heytap.browser.api.search.engine.c"
                    }) return@forEach
                    model.resolve().firstMethod {
                        name = methodName
                        parameters(String::class)
                    }.hook {
                        after {
                            val key = try { firstArg().get<String>() } catch (_: Throwable) { null }
                            if (!key.orEmpty().startsWith("reborn_")) return@after
                            val owner = instance ?: return@after
                            val engines = synchronized(cache) {
                                cache[owner] ?: createModels(owner).also { cache[owner] = it }
                            }
                            engines.firstOrNull { engineKey(it) == key }?.let { result = it }
                        }
                    }
                } catch (error: Throwable) {
                    YLog.error("Reborn: search lookup hook mismatch: $methodName", error)
                }
            }
        }
        // An explicitly selected Reborn engine must work even if the native picker
        // has changed. Supplied DEX builds use c.r(query) or c.s(query).
        val route = RebornBrowserConfig.searchEngineById(
            selectedProvider, customName, customTemplate, customEntries
        )
            ?: return
        val engineClass = "com.heytap.browser.search.impl.engine.c".toClassOrNull() ?: return
        try {
            val routeMethod = engineClass.declaredMethods.singleOrNull {
                it.name in setOf("r", "s") && it.returnType == String::class.java &&
                    it.parameterTypes.contentEquals(arrayOf(String::class.java))
            } ?: return
            engineClass.resolve().firstMethod {
                name = routeMethod.name
                parameters(String::class)
            }.hook {
                after {
                    val input = try { firstArg().get<String>() } catch (_: Throwable) { null }
                        ?: return@after
                    if (input.length > 2048) return@after
                    // Do not change unexpected non-URL return values.
                    val nativeUrl = result as? String ?: return@after
                    if (RebornBrowserConfig.validHttpUrl(nativeUrl) != null) {
                        result = RebornBrowserConfig.searchUrl(route, input)
                    }
                }
            }
        } catch (error: Throwable) {
            YLog.error("Reborn: native search URL method changed", error)
        }
    }

    private fun engineKey(model: Any?): String? = try {
        model?.javaClass?.getMethod("getKey")?.invoke(model)?.toString()
    } catch (_: Throwable) { null }

    private fun createModels(owner: Any): List<Any> {
        val context = try {
            owner.javaClass.declaredFields.firstOrNull {
                Context::class.java.isAssignableFrom(it.type)
            }?.let { field -> field.isAccessible = true; field.get(owner) as? Context }
        } catch (error: Exception) {
            YLog.error("Reborn: search model Context unavailable", error)
            null
        } ?: return emptyList()
        val engines = listOfNotNull(
            RebornBrowserConfig.searchEngine(RebornBrowserConfig.BING, "", ""),
            RebornBrowserConfig.searchEngine(RebornBrowserConfig.GOOGLE, "", ""),
            RebornBrowserConfig.searchEngine(RebornBrowserConfig.CUSTOM, customName, customTemplate),
        ) + RebornBrowserConfig.parseCustomSearchEngines(customEntries)
        val loader = owner.javaClass.classLoader ?: return emptyList()
        return engines.mapNotNull { engine ->
            try {
                val dataClass = loader.loadClass("com.heytap.browser.api.search.engine.e")
                val nativeClass = loader.loadClass("com.heytap.browser.search.impl.engine.k")
                val wrapperClass = loader.loadClass("com.heytap.browser.search.impl.engine.c")
                val config = dataClass.getDeclaredConstructor(
                    String::class.java, String::class.java, Int::class.javaPrimitiveType,
                    String::class.java, String::class.java, String::class.java,
                    String::class.java, String::class.java, String::class.java,
                    Boolean::class.javaPrimitiveType,
                ).newInstance(
                    engine.label,
                    "reborn_${engine.id}",
                    0,
                    engine.urlTemplate.replace("%s", "{searchTerms}"),
                    "", "", "", "", "", true,
                )
                val native = nativeClass.getDeclaredConstructor(Context::class.java, dataClass)
                    .newInstance(context, config)
                wrapperClass.getDeclaredConstructor(Context::class.java, nativeClass)
                    .newInstance(context, native)
            } catch (error: Throwable) {
                YLog.error("Reborn: cannot create native search engine ${engine.id}", error)
                null
            }
        }
    }
}
