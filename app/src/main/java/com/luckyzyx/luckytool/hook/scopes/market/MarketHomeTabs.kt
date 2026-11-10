package com.luckyzyx.luckytool.hook.scopes.market

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

/**
 * 26.10.2_CN assigns the original firstPagePos to HomeChosenCardFragment BEFORE
 * constructing the other pages. Never filter its source DTO or change that index:
 * doing so relabels the recommendation feed as 应用时刻. Filter completed pairs only.
 */
@Obfuscate
class MarketHomeTabs : YukiBaseHooker() {
    private data class PendingSelection(val selected: Int, val home: Int)
    private val pendingSelection = java.util.WeakHashMap<Any, PendingSelection>()
    private val installedTitles = java.util.WeakHashMap<Any, List<String>>()

    override fun onHook() {
        val presenter = "com.heytap.cdo.client.cards.page.main.home.tab.HomeSubTabPresenter"
            .toClassOrNull() ?: return
        try {
            presenter.resolve().firstMethod {
                // Native final binding: pagerAdapter.setItems(pages), tabLayout.bind(tabs).
                name = "\u079a"
                parameters(List::class, List::class)
            }.hook {
                before {
                    val owner = instance ?: return@before
                    val tabs = args[0] as? List<*> ?: return@before
                    val pages = args[1] as? List<*> ?: return@before
                    try {
                        val signatures = pages.map { pageSignature(it) ?: return@before }
                        val layout = presenter.getDeclaredField("\u08a8").apply { isAccessible = true }.get(owner)
                        val selectedPosition = layout?.javaClass?.getMethod("getSelectedTabPosition")
                            ?.invoke(layout) as? Int
                        val selectedTitle = selectedPosition?.let { installedTitles[owner]?.getOrNull(it) }
                        val filtered = RebornMarketTabRules.filterPairs(
                            tabs, pages, signatures, selectedTitle, ::tabName,
                        ) ?: return@before
                        // During binding the one-shot native observer reads the default
                        // index to select a page. Temporarily supply the restored current
                        // index, then put the App Moment Back target back in after().
                        // This never touches the source DTO firstPagePos.
                        val current = presenter.getDeclaredField("\u08af").apply { isAccessible = true }
                        val startup = presenter.getDeclaredField("\u08b3").apply { isAccessible = true }
                        if (current.type != Int::class.javaPrimitiveType || startup.type != Int::class.javaPrimitiveType)
                            return@before
                        val titles = filtered.tabs.map { tabName(it)!!.trim() }
                        val filteredTabs = ArrayList(filtered.tabs)
                        val filteredPages = ArrayList(filtered.pages)
                        pendingSelection[owner] = PendingSelection(filtered.selectedIndex, filtered.homeIndex)
                        current.setInt(owner, filtered.selectedIndex)
                        startup.setInt(owner, filtered.selectedIndex)
                        args[0] = filteredTabs
                        args[1] = filteredPages
                        installedTitles[owner] = titles
                    } catch (error: Throwable) {
                        pendingSelection.remove(owner)
                        YLog.error("Reborn: Market paired home pages changed; keeping native pages", error)
                    }
                }
                after {
                    val owner = instance ?: return@after
                    val selection = pendingSelection.remove(owner) ?: return@after
                    try {
                        presenter.getDeclaredField("\u08af").apply { isAccessible = true }
                            .setInt(owner, selection.home)
                        val pager = presenter.getDeclaredField("\u08aa").apply { isAccessible = true }.get(owner)
                        pager.javaClass.getMethod("setCurrentItem", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
                            .invoke(pager, selection.selected, false)
                        // Same-index selection does not dispatch onPageSelected.
                        // Synchronize native Back registration with the restored home.
                        val actual = pager.javaClass.getMethod("getCurrentItem").invoke(pager) as? Int
                        if (actual != null && actual in (installedTitles[owner]?.indices ?: IntRange.EMPTY)) {
                            presenter.getMethod("onPageSelected", Int::class.javaPrimitiveType).invoke(owner, actual)
                        }
                    } catch (error: Throwable) {
                        YLog.error("Reborn: Market home page selection changed", error)
                    }
                }
            }
        } catch (error: Throwable) {
            YLog.error("Reborn: Market home page binding changed", error)
        }
    }

    private fun tabName(item: Any?): String? = try {
        item?.javaClass?.getMethod("getTitle")?.invoke(item) as? String
    } catch (_: ReflectiveOperationException) { null }

    private fun pageSignature(page: Any?): MarketPageSignature? {
        if (page == null || page.javaClass.name !=
            "com.nearme.module.ui.fragment.CdoFragmentPagerAdapter\$a") return null
        val type = page.javaClass
        val title = type.getDeclaredField("\u0528").apply { isAccessible = true }.get(page) as? String
            ?: return null
        val fragment = type.getDeclaredField("\u037f").apply { isAccessible = true }.get(page)
            ?: return null
        val names = generateSequence(fragment.javaClass as Class<*>?) { it.superclass }.map { it.name }
        val actionParam = type.getDeclaredField("\u052a").apply { isAccessible = true }.get(page) as? String
        return MarketPageSignature(title, RebornMarketTabRules.pageKind(names, actionParam))
    }
}
