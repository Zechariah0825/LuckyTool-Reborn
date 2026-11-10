package com.luckyzyx.luckytool.hook.scopes.browser

import android.content.Context
import android.view.View
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
    private val barMode: String = RebornBrowserConfig.ORIGINAL,
    private val barEngineIds: Set<String> = emptySet(),
    private val providerRevision: String = "",
) : YukiBaseHooker() {
    private val cache = WeakHashMap<Any, List<Any>>()
    private val nativeCache = WeakHashMap<Any, List<Any>>()
    private val pageModelKeys = WeakHashMap<Any, List<String?>>()
    private var contextFailureLogged = false
    private var defaultFailureLogged = false
    private val syncingDefault = java.util.concurrent.atomic.AtomicBoolean(false)
    @Volatile private var defaultSyncFinished = false

    override fun onHook() {
        val model = "com.heytap.browser.search.impl.engine.DefaultSearchEngines".toClassOrNull()
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
                            if (list.any {
                                    it == null || !it.javaClass.name.startsWith("com.heytap.browser.search.impl.engine.c")
                                }) return@after
                            val owner = instance ?: return@after
                            val newEngines = models(owner)
                            if (newEngines.isEmpty()) return@after
                            val isResultList = methodName == "Q" || methodName == "M"
                            if (isResultList && barMode == RebornBrowserConfig.SEARCH_BAR_CUSTOM) {
                                result = resultEngines(owner, list.filterNotNull())
                                return@after
                            }
                            if (!extendNativeList) return@after
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
                            val engines = models(owner)
                            engines.firstOrNull { engineKey(it) == key }?.let { result = it }
                        }
                    }
                } catch (error: Throwable) {
                    YLog.error("Reborn: search lookup hook mismatch: $methodName", error)
                }
            }
        }
        if (model != null) {
            val current = model.declaredMethods.any { it.name == "J" && it.parameterCount == 0 &&
                it.returnType.name == "com.heytap.browser.api.search.engine.c" }
            try {
                // The native refresh validates persisted keys against this internal list.
                // Augment before validation, so a selected custom engine is not reset on boot.
                model.resolve().firstMethod {
                    name = "o0"; parameters(List::class, Boolean::class, Boolean::class, Boolean::class)
                }.hook { before {
                    val owner = instance ?: return@before
                    val incoming = firstArg().get<List<Any>>() ?: return@before
                    val usesModuleModels = extendNativeList || selectedProvider != RebornBrowserConfig.ORIGINAL ||
                        barMode == RebornBrowserConfig.SEARCH_BAR_CUSTOM
                    if (!usesModuleModels) {
                        val savedKey = owner.javaClass.getDeclaredMethod(if (current) "G" else "d")
                            .apply { isAccessible = true }.invoke(owner) as? String
                        if (!savedKey.orEmpty().startsWith("reborn_")) return@before
                    }
                    val existing = incoming.mapNotNull(::engineKey).toSet()
                    val missing = models(owner).filter { engineKey(it) !in existing }
                    if (missing.isNotEmpty()) firstArg().set(ArrayList(incoming + missing))
                } }
                // No returned key/object is overridden. A changed module request is
                // written once through the native setter; future native choices win.
                model.resolve().firstMethod {
                    name = if (current) "G" else "d"; parameters()
                }.hook { before {
                    instance?.let { syncDefaultOnce(it, current) }
                } }
            } catch (error: Throwable) {
                YLog.error("Reborn: native default sync mapping unavailable", error)
            }
        }
        installResultBar()
    }

    private fun syncDefaultOnce(owner: Any, current: Boolean) {
        // Native refresh holds its manager lock while reading G/d. Never wait
        // for another sync here: its native setter may need that same lock.
        if (defaultSyncFinished || !syncingDefault.compareAndSet(false, true)) return
        try {
            if (defaultSyncFinished) return
            val route = RebornBrowserConfig.searchEngineById(selectedProvider, customName, customTemplate, customEntries)
            val token = BrowserDefaultEnginePolicy.requestToken(route, providerRevision)
            val context = owner.javaClass.getDeclaredField("a").apply { isAccessible = true }.get(owner) as Context
            val prefs = context.getSharedPreferences(BrowserDefaultEnginePolicy.PREFS, Context.MODE_PRIVATE)
            if (token == null) {
                // Following the browser never writes its native default key.
                // Forget the prior request so a later identical request can apply again.
                if (selectedProvider == RebornBrowserConfig.ORIGINAL &&
                    prefs.contains(BrowserDefaultEnginePolicy.APPLIED_REQUEST)) {
                    prefs.edit().remove(BrowserDefaultEnginePolicy.APPLIED_REQUEST).commit()
                }
                defaultSyncFinished = true
                return
            }
            if (!BrowserDefaultEnginePolicy.shouldApply(token, prefs.getString(BrowserDefaultEnginePolicy.APPLIED_REQUEST, null))) {
                defaultSyncFinished = true
                return
            }
            val key = "reborn_${route!!.id}"
            if (models(owner).none { engineKey(it) == key }) return
            owner.javaClass.getDeclaredMethod(if (current) "g" else "j", String::class.java)
                .apply { isAccessible = true }.invoke(owner, key)
            val saved = owner.javaClass.getDeclaredMethod(if (current) "G" else "d")
                .apply { isAccessible = true }.invoke(owner) as? String
            if (saved != key) return
            // Store only one bounded digest, not a history or any search URL/query.
            if (!prefs.edit().putString(BrowserDefaultEnginePolicy.APPLIED_REQUEST, token).commit()) {
                YLog.error("Reborn: could not persist applied search request; check browser storage")
            }
            defaultSyncFinished = true
        } catch (error: Throwable) {
            if (!defaultFailureLogged) {
                defaultFailureLogged = true
                YLog.error("Reborn: cannot apply module default request; leaving native selection available", error)
            }
        } finally { syncingDefault.set(false) }
    }

    private fun models(owner: Any): List<Any> = synchronized(cache) {
        cache[owner] ?: createModels(owner).also { if (it.isNotEmpty()) cache[owner] = it }
    }

    private fun resultEngines(owner: Any, original: List<Any>): List<Any> {
        val available = models(owner)
        if (available.isEmpty()) return original // No partial model rewrite if construction failed.
        val definitions = RebornBrowserConfig.availableSearchEngines(customName, customTemplate, customEntries)
        val wanted = RebornBrowserConfig.searchBarEngines(definitions, barEngineIds, "")
        val byKey = available.associateBy { engineKey(it) }
        val native = synchronized(nativeCache) {
            val merged = (nativeCache[owner].orEmpty() + original.filter {
                !engineKey(it).orEmpty().startsWith("reborn_")
            }).associateBy { engineKey(it) }.values.toList()
            nativeCache[owner] = merged
            merged
        }
        val chosenNative = RebornBrowserConfig.nativeSearchBarChoices.mapNotNull { (id, _) ->
            if (id !in barEngineIds) null else native.firstOrNull {
                RebornBrowserConfig.nativeSearchBarId(engineKey(it), engineLabel(it)) == id
            }
        }
        val chosen = wanted.mapNotNull { byKey["reborn_${it.id}"] } + chosenNative
        val default = nativeDefault(owner)
        // Never silently replace a missing default with the first checked engine.
        if (default == null || engineKey(default).isNullOrBlank()) return original
        return ArrayList(RebornBrowserConfig.withSearchBarDefault(chosen, default, ::engineKey))
    }

    private fun nativeDefault(owner: Any): Any? = try {
        // Both APKs call the static facade b() for the live engine manager.
        // Resolve each time so follow-browser mode tracks changes to its default.
        // These object getters call key lookup, not result-list getters: no recursion.
        val loader = owner.javaClass.classLoader
        val facade = loader.loadClass("com.heytap.browser.api.search.engine.SearchEnginesFacade")
        val manager = facade.getDeclaredMethod("b").invoke(null)
        val getter = manager.javaClass.declaredMethods.singleOrNull {
            it.name in setOf("J", "F") && it.parameterCount == 0 &&
                it.returnType.name == "com.heytap.browser.api.search.engine.c"
        }
        getter?.apply { isAccessible = true }?.invoke(manager)
    } catch (error: Throwable) {
        if (!defaultFailureLogged) {
            defaultFailureLogged = true
            YLog.error("Reborn: native default unavailable; preserving original search list", error)
        }
        null
    }

    private fun installResultBar() {
        val custom = barMode == RebornBrowserConfig.SEARCH_BAR_CUSTOM
        val hide = barMode == RebornBrowserConfig.SEARCH_BAR_HIDDEN
        if (!custom && !hide) return
        val page = "com.heytap.browser.ai.page.multi.MultiSearchPage".toClassOrNull()
        if (custom && page != null) {
            // These getters return the actual shared mutable model used for BOTH
            // page creation and tab binding, including restored/server-refreshed lists.
            // Returning a detached list here would break native clear/addAll callers.
            val getter = if (page.declaredMethods.any { it.name == "Ui" && it.parameterCount == 0 &&
                it.returnType == List::class.java }) "Ui" else "aj"
            try {
                val setter = if (getter == "Ui") "Lk" else "dl"
                listOfNotNull(page, "com.heytap.browser.ai.page.multi.v1.MultiSearchPageV1".toClassOrNull()).forEach { type ->
                    if (type.declaredMethods.any { it.name == setter && it.parameterTypes.contentEquals(arrayOf(List::class.java)) }) {
                        type.resolve().firstMethod { name = setter; parameters(List::class) }.hook { before {
                            val owner = instance ?: return@before
                            val incoming = firstArg().get<List<Any>>() ?: return@before
                            val chosen = resultEngines(owner, incoming)
                            if (chosen.isNotEmpty()) firstArg().set(ArrayList(chosen))
                        } }
                    }
                }
                page.resolve().firstMethod { name = getter; parameters() }.hook { after {
                    val owner = instance ?: return@after
                    val list = result as? MutableList<Any> ?: return@after
                    if (list.isEmpty()) return@after
                    try {
                        val keys = list.map(::engineKey)
                        val previous = synchronized(pageModelKeys) { pageModelKeys[owner] }
                        // Do not reshuffle existing pages when the native settings
                        // activity changes the default. A new native model input
                        // (setter/server/fallback refresh) starts the next generation.
                        if (!RebornBrowserConfig.shouldNormalizeSearchPage(keys, previous)) return@after
                        val wanted = resultEngines(owner, list)
                        val wantedKeys = wanted.map(::engineKey)
                        if (wanted.isNotEmpty() && keys != wantedKeys) {
                            list.clear()
                            list.addAll(wanted)
                        }
                        synchronized(pageModelKeys) { pageModelKeys[owner] = list.map(::engineKey) }
                    } catch (error: Throwable) { YLog.error("Reborn: result engine model unavailable", error) }
                } }
            } catch (error: Throwable) { YLog.error("Reborn: result page hook mismatch", error) }
        }
        if (hide) {
            val tabs = "com.heytap.browser.ai.page.multi.ui.channel.MultiSearchTabLayout".toClassOrNull() ?: return
            try {
                val bind = tabs.declaredMethods.singleOrNull {
                    it.name in setOf("F0", "G0") && it.parameterTypes.contentEquals(arrayOf(
                        List::class.java, Boolean::class.javaPrimitiveType, Context::class.java))
                } ?: return
                tabs.resolve().firstMethod {
                    name = bind.name
                    parameters(List::class, Boolean::class, Context::class)
                }.hook { after { (instance as? View)?.visibility = View.GONE } }
                // Native owners may show the bar again after binding. Guard only
                // this exact view type, leaving every other View untouched.
                View::class.java.resolve().firstMethod {
                    name = "setVisibility"; parameters(Int::class)
                }.hook { before {
                    if (instance?.javaClass == tabs) firstArg().set(View.GONE)
                } }
                tabs.resolve().firstConstructor { parameters(Context::class, android.util.AttributeSet::class) }
                    .hook { after { (instance as? View)?.visibility = View.GONE } }
            } catch (error: Throwable) { YLog.error("Reborn: result bar visibility hook mismatch", error) }
        }
    }

    private fun engineLabel(model: Any?): String? = try {
        model?.javaClass?.getMethod("getLabel")?.invoke(model)?.toString()
    } catch (_: Throwable) { null }

    private fun engineKey(model: Any?): String? = try {
        model?.javaClass?.getMethod("getKey")?.invoke(model)?.toString()
    } catch (_: Throwable) { null }

    private fun createModels(owner: Any): List<Any> {
        val context = try {
            (owner as? Context) ?: (runCatching {
                owner.javaClass.getMethod("getContext").invoke(owner) as? Context
            }.getOrNull()) ?: owner.javaClass.declaredFields.firstOrNull {
                Context::class.java.isAssignableFrom(it.type)
            }?.let { field -> field.isAccessible = true; field.get(owner) as? Context }
        } catch (error: Exception) {
            YLog.error("Reborn: search model Context unavailable", error)
            null
        } ?: run {
            if (!contextFailureLogged) {
                contextFailureLogged = true
                YLog.error("Reborn: search model Context not yet available; preserving native engines")
            }
            return emptyList()
        }
        val engines = RebornBrowserConfig.availableSearchEngines(customName, customTemplate, customEntries)
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
                ).apply { isAccessible = true }.newInstance(
                    engine.label,
                    "reborn_${engine.id}",
                    -1,
                    engine.urlTemplate.replace("%s", "{searchTerms}"),
                    "", "", "", "", "singleSearch", true,
                )
                val native = nativeClass.getDeclaredConstructor(Context::class.java, dataClass)
                    .apply { isAccessible = true }.newInstance(context, config)
                wrapperClass.getDeclaredConstructor(Context::class.java, nativeClass)
                    .apply { isAccessible = true }.newInstance(context, native)
            } catch (error: Throwable) {
                YLog.error("Reborn: cannot create native search engine ${engine.id}", error)
                null
            }
        }
    }
}
