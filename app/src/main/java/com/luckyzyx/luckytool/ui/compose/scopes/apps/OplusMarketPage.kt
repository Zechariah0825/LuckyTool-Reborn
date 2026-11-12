package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 软件商店页：按启动与主页、搜索、更新下载及我的页面统一组织功能。
 */
object OplusMarketPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_market",
        prefsName = ModulePrefs,
        packName = "com.heytap.market",
        scopes = arrayOf("com.heytap.market"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        category("启动与主页")
        switch(
            key = "reborn_market_remove_featured_tab",
            title = "隐藏「精选」并默认进入「应用时刻」",
            summary = "从栏目数据中移除精选，其他栏目保持原样；重启软件商店生效",
            notify = true,
        )
        switch(
            key = "remove_market_splash_page_app_recommend",
            title = c.getString(R.string.remove_market_splash_page_app_recommend),
        )
        category("搜索页")
        switch(
            key = "reborn_market_clean_search_landing",
            title = "推荐区仅保留「大家正在搜」",
            summary = "移除人气热搜、精品推荐和热门 APP 等推荐板块；不删除搜索历史；重启软件商店生效",
            default = true,
            notify = true,
        )
        switch(
            key = "reborn_market_blank_search_hint",
            title = "保持空白搜索框",
            summary = "移除滚动推荐关键词，不修改已输入的搜索内容；重启软件商店生效",
            default = true,
            notify = true,
        )
        category("更新与下载")
        switch(
            key = "remove_market_update_download_page_app_recommend",
            title = c.getString(R.string.remove_market_update_download_page_app_recommend),
        )
        switch(
            key = "default_expand_update_list",
            title = c.getString(R.string.default_expand_update_list),
        )
        category("我的页面")
        switch(
            key = "remove_market_mine_page_app_recommend",
            title = c.getString(R.string.remove_market_mine_page_app_recommend),
        )
    }
}
