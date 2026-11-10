package com.luckyzyx.luckytool.hook.scopes.browser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Base64
import okhttp3.Authenticator
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Bounded process-local cache; never holds an Activity or View. Disk/network work is off-main. */
class BrowserSearchIconLoader(
    private val executor: Executor = ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, ArrayBlockingQueue(32),
        { task -> Thread(task, "Reborn-search-icon").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy()),
    private val dispatch: ((() -> Unit) -> Unit) = { Handler(Looper.getMainLooper()).post(it) },
    private val fetch: (String) -> ByteArray? = ::fetchIcon,
    private val now: () -> Long = System::currentTimeMillis,
) {
    data class Icon(val bitmap: Bitmap, val uri: String)
    private class Entry(var icon: Icon, var pending: Boolean = true, var expires: Long = Long.MAX_VALUE) {
        val callbacks = ArrayList<(Icon) -> Unit>()
    }
    private val cache = LinkedHashMap<String, Entry>(64, 0.75f, true)

    fun request(origin: String?, label: String, ready: (Icon) -> Unit): Icon {
        val key = origin ?: "fallback:${label.take(40)}"
        val entry: Entry
        synchronized(cache) {
            val old = cache[key]
            if (old != null && (old.pending || old.expires > now())) {
                if (old.pending && old.callbacks.size < 128) old.callbacks.add(ready)
                return old.icon
            }
            entry = Entry(fallback(origin?.let { URI(it).host.removePrefix("www.") } ?: label))
            cache[key] = entry
            while (cache.size > 64) cache.remove(cache.entries.first().key)
            if (origin == null) { entry.pending = false; return entry.icon }
            entry.callbacks.add(ready)
        }
        val site = origin ?: return entry.icon
        try {
            executor.execute {
                val downloaded = runCatching { fetch(site)?.let(::decode)?.let(::icon) }.getOrNull()
                complete(entry, downloaded)
            }
        } catch (_: RuntimeException) { complete(entry, null) }
        return entry.icon
    }

    private fun complete(entry: Entry, downloaded: Icon?) {
        val callbacks: List<(Icon) -> Unit>
        synchronized(cache) {
            if (downloaded != null) entry.icon = downloaded
            entry.pending = false
            entry.expires = now() + if (downloaded != null) TimeUnit.HOURS.toMillis(24) else TimeUnit.MINUTES.toMillis(5)
            callbacks = entry.callbacks.toList()
            entry.callbacks.clear()
        }
        dispatch { callbacks.forEach { runCatching { it(entry.icon) } } }
    }

    companion object {
        private val client by lazy {
            OkHttpClient.Builder().cookieJar(CookieJar.NO_COOKIES)
                .authenticator(Authenticator.NONE).proxyAuthenticator(Authenticator.NONE)
                .followRedirects(false).followSslRedirects(false)
                .connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS)
                .callTimeout(4, TimeUnit.SECONDS)
                .dns(object : Dns {
                    override fun lookup(hostname: String): List<java.net.InetAddress> =
                        Dns.SYSTEM.lookup(hostname).filter(BrowserSearchIconPolicy::publicAddress)
                            .ifEmpty { throw UnknownHostException("Non-public favicon address") }
                })
                .build()
        }

        private fun fetchIcon(origin: String): ByteArray? {
            for (candidate in BrowserSearchIconPolicy.candidates(origin)) {
                var url = candidate
                for (redirectCount in 0..2) {
                    val response = runCatching {
                        client.newCall(Request.Builder().url(url).header("User-Agent", "RebornSearchIcon/1")
                            .header("Accept", "image/*").build()).execute()
                    }.getOrNull() ?: break
                    try {
                        if (response.code in 300..399) {
                            url = BrowserSearchIconPolicy.redirect(origin, url, response.header("Location") ?: break) ?: break
                            continue
                        }
                        if (!response.isSuccessful) break
                        val body = response.body ?: break
                        if (body.contentLength() > BrowserSearchIconPolicy.MAX_BYTES) break
                        val bytes = body.byteStream().use(::readBounded)
                        val probe = bytes?.let(::decode)
                        if (probe != null) { probe.recycle(); return bytes }
                        break
                    } finally { response.close() }
                }
            }
            return null
        }

        private fun readBounded(stream: java.io.InputStream): ByteArray? {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) return out.toByteArray()
                if (out.size() + read > BrowserSearchIconPolicy.MAX_BYTES) return null
                out.write(buffer, 0, read)
            }
        }

        private fun icon(bitmap: Bitmap): Icon {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            return Icon(bitmap, "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP))
        }

        private fun fallback(label: String): Icon {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val canvas = Canvas(bitmap)
            paint.color = Color.rgb(42, 105 + (label.hashCode() and 31), 170)
            canvas.drawCircle(32f, 32f, 30f, paint)
            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 34f
            paint.textAlign = Paint.Align.CENTER
            val letter = label.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
            canvas.drawText(letter, 32f, 32f - (paint.ascent() + paint.descent()) / 2, paint)
            return icon(bitmap)
        }

        internal fun decode(bytes: ByteArray): Bitmap? {
            if (bytes.isEmpty() || bytes.size > BrowserSearchIconPolicy.MAX_BYTES) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (BrowserSearchIconPolicy.validDimensions(bounds.outWidth, bounds.outHeight)) {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 64).coerceAtLeast(1)
                }
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.let(::thumbnail)
            }
            return decodeIco(bytes)?.let(::thumbnail)
        }

        private fun thumbnail(bitmap: Bitmap): Bitmap = if (bitmap.width <= 64 && bitmap.height <= 64) bitmap else {
            val scale = 64f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1), true).also { if (it !== bitmap) bitmap.recycle() }
        }

        /** ICO PNG payloads and bounded 24/32-bit uncompressed DIBs (common site favicons). */
        private fun decodeIco(b: ByteArray): Bitmap? {
            fun u16(p: Int) = (b[p].toInt() and 255) or ((b[p + 1].toInt() and 255) shl 8)
            fun u32(p: Int): Long = (0..3).fold(0L) { value, i -> value or ((b[p + i].toLong() and 255) shl (i * 8)) }
            if (b.size < 6 || u16(0) != 0 || u16(2) != 1) return null
            val count = u16(4)
            if (count !in 1..32 || 6 + count * 16 > b.size) return null
            val ordered = (0 until count).sortedByDescending { index ->
                val p = 6 + index * 16
                (b[p].toInt() and 255).let { if (it == 0) 256 else it } *
                    (b[p + 1].toInt() and 255).let { if (it == 0) 256 else it }
            }
            for (index in ordered) {
                val pos = 6 + index * 16
                val length = u32(pos + 8); val offset = u32(pos + 12)
                if (offset < 6 + count * 16 || length < 40 || offset + length > b.size) continue
                val data = b.copyOfRange(offset.toInt(), (offset + length).toInt())
                if (data[0] == 0x89.toByte() && data[1] == 0x50.toByte()) {
                    decode(data)?.let { return it }
                    continue
                }
                fun d32(p: Int) = (0..3).fold(0) { value, i -> value or ((data[p + i].toInt() and 255) shl (i * 8)) }
                val header = d32(0); val width = d32(4); val doubleHeight = d32(8)
                val height = doubleHeight / 2
                val bits = (data[14].toInt() and 255) or ((data[15].toInt() and 255) shl 8)
                if (header != 40 || width !in 1..256 || height !in 1..256 || doubleHeight % 2 != 0 ||
                    bits !in setOf(24, 32) || d32(16) != 0) continue
                val stride = ((width * bits + 31) / 32) * 4
                val end = header + stride * height
                if (end > data.size) continue
                val pixels = IntArray(width * height)
                var hasAlpha = false
                for (y in 0 until height) for (x in 0 until width) {
                    val p = header + (height - 1 - y) * stride + x * (bits / 8)
                    val alpha = if (bits == 32) data[p + 3].toInt() and 255 else 255
                    if (alpha > 0) hasAlpha = true
                    pixels[y * width + x] = Color.argb(alpha, data[p + 2].toInt() and 255,
                        data[p + 1].toInt() and 255, data[p].toInt() and 255)
                }
                val maskStride = ((width + 31) / 32) * 4
                for (y in 0 until height) for (x in 0 until width) {
                    val mask = end + (height - 1 - y) * maskStride + x / 8
                    val transparent = mask < data.size && (data[mask].toInt() and (0x80 shr (x % 8))) != 0
                    val p = y * width + x
                    if (transparent) pixels[p] = pixels[p] and 0xffffff
                    else if (!hasAlpha) pixels[p] = pixels[p] or Color.BLACK
                }
                return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
            }
            return null
        }
    }
}
