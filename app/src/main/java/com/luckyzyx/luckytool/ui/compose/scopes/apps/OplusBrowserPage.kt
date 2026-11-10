package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.hook.scopes.browser.RebornBrowserConfig as C
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.showToast

/**
 * 浏览器页：广告、窗口数量限制与搜索栏推广的开关，以及简洁模式入口。
 * 简洁模式入口只在装有 com.heytap.browser 时出现，点击跳浏览器自身的首页设置页。
 */
object OplusBrowserPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_browser",
        prefsName = ModulePrefs,
        packName = "com.heytap.browser",
        scopes = arrayOf("com.heytap.browser"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (c.checkPackName("com.heytap.browser")) {
            custom(title = c.getString(R.string.browser_concise_mode)) {
                PrefRowInset {
                    PrefRow(
                        title = stringResource(R.string.browser_concise_mode),
                        onClick = {
                            try {
                                Intent().apply {
                                    setClassName(
                                        "com.heytap.browser",
                                        "com.heytap.browser.settings.component.BrowserPreferenceActivity"
                                    )
                                    putExtra(
                                        "key.fragment.name",
                                        "com.heytap.browser.settings.homepage.HomepagePreferenceFragment"
                                    )
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                    c.startActivity(this)
                                }
                            } catch (_: Exception) {
                                c.showToast("Error: Please check your browser version!")
                            }
                        },
                    )
                }
            }
        }
        category("Reborn · 主页")
        list(
            key = C.HOME_MODE,
            title = "主页显示模式",
            entries = arrayOf("原始主页（不替换）", "自定义网址", "纯净空白页", "我的快捷链接"),
            entryValues = arrayOf(C.ORIGINAL, C.URL, C.BLANK, C.SHORTCUTS),
            default = C.ORIGINAL,
            notify = true,
        )
        editText(
            key = C.HOME_URL,
            title = "自定义主页网址",
            hint = "https://www.google.com",
            summary = "仅在「自定义网址」模式下使用（http/https）",
            validate = { value ->
                if (value.isBlank() || C.validHttpUrl(value) != null) null
                else "请输入完整的 http/https 网址"
            },
            notify = true,
        )
        editText(
            key = C.HOME_SHORTCUTS,
            title = "编辑主页快捷链接",
            summary = "每行一个：名称 | https://网址",
            hint = "Google | https://www.google.com",
            dialogMessage = "可增加、编辑、删除或调整顺序；每行一条，调整文本行顺序即可排序。最多 24 条。",
            maxLines = 12,
            validate = { C.entriesError(it, search = false) },
            notify = true,
        )
        category("Reborn · 导航及我的")
        switch(
            key = C.HIDE_BOTTOM_TABS,
            title = "移除底部「视频」和「小说」",
            summary = "保留首页、搜索+、我的；重新启动浏览器生效",
            notify = true,
        )
        switch(
            key = C.HIDE_MINE_PROMOS,
            title = "移除「领现金」及「我的小说」区域",
            summary = "仅处理「我的」中的小说卡片和领现金按钮，保留登录、账户及其他工具",
            notify = true,
        )
        category("Reborn · 搜索引擎")
        switch(
            key = C.SEARCH_EXTEND,
            title = "向浏览器原生列表添加 Bing、Google 和自定义引擎",
            summary = "目标 40.10.21.9.6 / 40.10.25.2；实验性，更改后重启浏览器",
            default = false,
            notify = true,
        )
        val customEntries = state.getString(C.SEARCH_CUSTOM_ENGINES, "").orEmpty()
        val customEngines = C.parseCustomSearchEngines(customEntries)
        val savedProvider = state.getString(C.SEARCH_PROVIDER, C.ORIGINAL).orEmpty()
        val savedEngine = C.searchEngineById(savedProvider, "", "", customEntries)
        list(
            key = C.SEARCH_PROVIDER,
            title = "默认搜索引擎",
            entries = (listOf("跟随浏览器设置（含新增引擎）", "Bing", "Google") + customEngines.map { it.label }).toTypedArray(),
            entryValues = (listOf(C.ORIGINAL, C.BING, C.GOOGLE) + customEngines.map {
                // Keep an old saved hash ID visibly selected until the user changes it.
                if (savedProvider.startsWith("custom_") && savedEngine?.id == it.id) savedProvider else it.id
            }).toTypedArray(),
            default = C.ORIGINAL,
            summary = "此处指定的引擎优先；跟随模式使用浏览器原生列表中选定的引擎",
            notify = true,
        )
        editText(
            key = C.SEARCH_CUSTOM_ENGINES,
            title = "添加、编辑和删除自定义搜索引擎",
            summary = "每行一个：名称 | https://搜索地址?q=%s",
            hint = "DuckDuckGo | https://duckduckgo.com/?q=%s",
            dialogMessage = "每行添加一条，删除整行即可移除；名称和 URL 都能编辑。保存后会出现在上面的默认搜索引擎列表中。模板必须包含一个 %s。",
            maxLines = 14,
            validate = { C.entriesError(it, search = true) },
            onChange = { value ->
                val current = state.getString(C.SEARCH_PROVIDER, C.ORIGINAL).orEmpty()
                if (current.startsWith("custom_") && C.searchEngineById(current, "", "", value) == null) {
                    state.set(C.SEARCH_PROVIDER, C.ORIGINAL)
                }
            },
            notify = true,
        )
        category(c.getString(R.string.ads))
        switch(
            key = "remove_ads_from_download_dialog",
            title = c.getString(R.string.remove_ads_from_download_dialog),
        )
        switch(
            key = "remove_ads_at_download_page_bottom",
            title = c.getString(R.string.remove_ads_at_download_page_bottom),
        )
        switch(
            key = "remove_browser_window_limit_number",
            title = c.getString(R.string.remove_browser_window_limit_number),
        )
        switch(
            key = "remove_browser_search_bar_app_promotion",
            title = c.getString(R.string.remove_browser_search_bar_app_promotion),
        )
    }
}
