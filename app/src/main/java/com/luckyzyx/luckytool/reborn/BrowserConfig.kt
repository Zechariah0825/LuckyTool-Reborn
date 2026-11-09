/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.luckyzyx.luckytool.reborn

import org.json.JSONArray
import org.json.JSONObject

/** Shared, validated serialization format for module preferences and injected hooks. */
object BrowserConfig {
    const val COMPACT_TABS = "reborn_browser_compact_tabs"
    const val HIDE_CASH = "reborn_browser_hide_cash"
    const val HIDE_BOOKSHELF = "reborn_browser_hide_bookshelf"
    const val HOME_MODE = "reborn_browser_home_mode"
    const val HOME_URL = "reborn_browser_home_url"
    const val QUICK_LINKS = "reborn_browser_quick_links"
    const val EXTEND_SEARCH = "reborn_browser_search_extend"
    const val ENGINES = "reborn_browser_search_engines"
    const val DEFAULT_ENGINE = "reborn_browser_default_engine"
    const val MARKET_MOMENTS = "reborn_market_moments_home"

    fun links(json: String): List<CustomizationPolicy.Link> {
        val array = JSONArray(json)
        require(array.length() <= CustomizationPolicy.MAX_LINKS) { "最多保存 64 个快捷链接" }
        val ids = HashSet<String>()
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            CustomizationPolicy.Link(obj.getString("id"), obj.getString("title"), obj.getString("url"))
                .also { require(ids.add(it.id)) { "快捷链接标识重复" } }
        }
    }

    fun encodeLinks(items: List<CustomizationPolicy.Link>): String = JSONArray().apply {
        items.forEach { link ->
            put(JSONObject().put("id", link.id).put("title", link.title).put("url", link.url))
        }
    }.toString()

    fun customEngines(json: String): List<CustomizationPolicy.Engine> {
        val array = JSONArray(json)
        require(array.length() <= CustomizationPolicy.MAX_ENGINES) { "最多保存 24 个自定义搜索引擎" }
        val ids = hashSetOf("bing", "google")
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            CustomizationPolicy.Engine(obj.getString("id"), obj.getString("name"), obj.getString("template"))
                .also { require(ids.add(it.id)) { "搜索引擎标识重复" } }
        }
    }

    fun allEngines(json: String): List<CustomizationPolicy.Engine> = listOf(
        CustomizationPolicy.Engine("bing", "Bing", CustomizationPolicy.BING_TEMPLATE),
        CustomizationPolicy.Engine("google", "Google", CustomizationPolicy.GOOGLE_TEMPLATE),
    ) + customEngines(json)

    fun encodeEngines(items: List<CustomizationPolicy.Engine>): String = JSONArray().apply {
        items.forEach { engine ->
            put(JSONObject().put("id", engine.id).put("name", engine.name).put("template", engine.template))
        }
    }.toString()
}
