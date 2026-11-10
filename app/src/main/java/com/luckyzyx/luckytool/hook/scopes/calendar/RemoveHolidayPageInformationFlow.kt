package com.luckyzyx.luckytool.hook.scopes.calendar

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.diagnostics.HookDiagnostics
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveHolidayPageInformationFlow : YukiBaseHooker() {
    private const val KEY = "remove_holiday_page_information_flow"

    override fun onHook() {
        // HookCalendar loads this hook only when this exact setting is enabled.
        val status = HookDiagnostics.store
        status.begin(KEY, true, tracksChanges = true)
        try {
            // Source SpecialHolidayWebViewDetailViewModel > V13.9.16.
            val target = "com.coloros.calendar.app.specialholiday.SpecialHolidayWebViewDetailViewModel"
                .toClassOrNull() ?: throw ClassNotFoundException()
            target.resolve().firstMethod { name = "buildHolidayH5UrlInner" }.hook {
                after {
                    status.triggered(KEY)
                    try {
                        val original = result<String>() ?: return@after
                        val updated = formatCalendarUrl(original)
                        result = updated
                        if (updated != original) status.changed(KEY)
                    } catch (error: Throwable) {
                        status.failed(KEY, error)
                        throw error
                    }
                }
            }
            // A returned DSL handle is not a registration receipt.
        } catch (error: Throwable) {
            status.failed(KEY, error)
            YLog.error("Reborn: holiday information-flow lookup failed", error)
        }
    }

    private fun formatCalendarUrl(url: String): String {
        if (url.isBlank()) return url
        var cacheUrl = url
        if (cacheUrl.contains("&showAd=1")) cacheUrl = cacheUrl.replace(
            "&showAd=1", ""
        )
        return cacheUrl
    }
}
