package com.luckyzyx.luckytool.hook.scopes.browser

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

/**
 * ColorOS 17 browser build 40.10.25.2: both concrete home-page fragments use
 * yh(Context, LayoutInflater, ViewGroup): View. Never intercept normal URL tabs.
 * The original view remains attached behind our home view to preserve its lifecycle.
 */
@Obfuscate
class BrowserHomeReplacement(
    private val mode: String,
    private val configuredUrl: String,
    private val shortcuts: String,
) : YukiBaseHooker() {
    override fun onHook() {
        listOf(
            "com.heytap.browser.home.NormalHomePageFragment",
            "com.heytap.browser.app.search.content.SearchHomePageFragment",
        ).forEach { className ->
            try {
                val cls = className.toClassOrNull() ?: return@forEach
                cls.resolve().firstMethod {
                    name = "yh"
                    parameters(Context::class, LayoutInflater::class, ViewGroup::class)
                }.hook {
                    after {
                        try {
                            val old = result as? View ?: return@after
                            // Build the replacement before detaching the original; if WebView
                            // initialization fails, the original homepage stays usable.
                            val replacement = buildHome(old.context)
                            val wrapper = FrameLayout(old.context)
                            val parent = old.parent as? ViewGroup
                            parent?.removeView(old)
                            // Keep original hierarchy alive for its own lifecycle callbacks.
                            wrapper.addView(old, FrameLayout.LayoutParams(-1, -1))
                            wrapper.addView(replacement, FrameLayout.LayoutParams(-1, -1))
                            old.visibility = View.INVISIBLE
                            result = wrapper
                        } catch (error: Throwable) {
                            YLog.error("Reborn: home view replacement failed", error)
                        }
                    }
                }
            } catch (error: Throwable) {
                YLog.error("Reborn: home entry not found: $className", error)
            }
        }
    }

    private fun buildHome(context: Context): View = when (mode) {
        RebornBrowserConfig.URL -> buildUrlHome(context)
        RebornBrowserConfig.SHORTCUTS -> buildShortcutHome(context)
        else -> FrameLayout(context).apply { setBackgroundColor(background(context)) }
    }

    private fun buildUrlHome(context: Context): View {
        val url = RebornBrowserConfig.validHttpUrl(configuredUrl)
            ?: return message(context, "主页网址无效，请在 LuckyTool 中重新设置")
        return WebView(context).apply {
            setBackgroundColor(background(context))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val req = request ?: return false
                    val target = req.url.toString()
                    if (!req.isForMainFrame || target == url ||
                        RebornBrowserConfig.validHttpUrl(target) == null
                    ) return false
                    return openNativeTab(context, target)
                }
            }
            loadUrl(url)
        }
    }

    private fun buildShortcutHome(context: Context): View {
        val fg = foreground(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(dp(context, 20), dp(context, 52), dp(context, 20), dp(context, 24))
            setBackgroundColor(background(context))
        }
        container.addView(TextView(context).apply {
            text = "我的主页"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(fg)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(context, 64)))
        val grid = GridLayout(context).apply { columnCount = 3; alignmentMode = GridLayout.ALIGN_BOUNDS }
        val links = RebornBrowserConfig.parseShortcuts(shortcuts)
        if (links.isEmpty()) {
            container.addView(TextView(context).apply {
                text = "在 LuckyTool → 浏览器中添加快捷链接\n每行填写：名称 | https://网址"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(fg)
            }, LinearLayout.LayoutParams(-1, dp(context, 120)))
        } else {
            links.forEach { link ->
                grid.addView(Button(context).apply {
                    text = link.title
                    isAllCaps = false
                    setOnClickListener { openNativeTab(context, link.url) }
                }, GridLayout.LayoutParams().apply {
                    width = (context.resources.displayMetrics.widthPixels - dp(context, 56)) / 3
                    height = dp(context, 68)
                    setMargins(dp(context, 2), dp(context, 5), dp(context, 2), dp(context, 5))
                })
            }
            container.addView(grid)
        }
        return ScrollView(context).apply {
            isFillViewport = true
            addView(container, FrameLayout.LayoutParams(-1, -2))
        }
    }

    private fun message(context: Context, msg: String) = TextView(context).apply {
        text = msg
        textSize = 17f
        gravity = Gravity.CENTER
        setTextColor(foreground(context))
        setBackgroundColor(background(context))
    }

    private fun openNativeTab(context: Context, url: String): Boolean {
        val safe = RebornBrowserConfig.validHttpUrl(url) ?: return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safe)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage("com.heytap.browser")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            YLog.error("Reborn: cannot open native browser tab", error)
            false
        }
    }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density + .5f).toInt()
    private fun background(context: Context): Int =
        if (dark(context)) Color.BLACK else Color.WHITE
    private fun foreground(context: Context): Int =
        if (dark(context)) Color.WHITE else Color.BLACK
    private fun dark(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
