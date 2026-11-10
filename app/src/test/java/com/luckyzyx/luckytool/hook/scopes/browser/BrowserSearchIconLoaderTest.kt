package com.luckyzyx.luckytool.hook.scopes.browser

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [32], application = android.app.Application::class)
class BrowserSearchIconLoaderTest {
    private fun png(): ByteArray = ByteArrayOutputStream().apply {
        Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
            .compress(Bitmap.CompressFormat.PNG, 100, this)
    }.toByteArray()

    @Test fun `network is scheduled not run during bind and same origin coalesces`() {
        val work = ArrayList<Runnable>()
        var calls = 0; var callbacks = 0
        val loader = BrowserSearchIconLoader(Executor { work.add(it) }, { it() }, { calls++; png() })
        val fallback = loader.request("https://example.com", "Example") { callbacks++ }
        loader.request("https://example.com", "Another label") { callbacks++ }
        assertEquals(0, calls)
        assertEquals(1, work.size)
        assertEquals(64, fallback.bitmap.width)
        assertTrue(fallback.uri.startsWith("data:image/png;base64,"))
        work.single().run()
        assertEquals(1, calls); assertEquals(2, callbacks)
        val cached = loader.request("https://example.com", "Example") { fail("fresh cache needs no async callback") }
        assertEquals(4, cached.bitmap.width)
        assertEquals(1, work.size)
    }
    @Test fun `failure is negatively cached and retried only after expiry`() {
        val work = ArrayList<Runnable>(); var time = 0L; var calls = 0
        val loader = BrowserSearchIconLoader(Executor { work.add(it) }, { it() }, { calls++; null }, { time })
        val first = loader.request("https://example.com", "Example") {}
        work.removeAt(0).run()
        assertSame(first, loader.request("https://example.com", "Example") {})
        assertTrue(work.isEmpty()); assertEquals(1, calls)
        time = 301000
        loader.request("https://example.com", "Example") {}
        assertEquals(1, work.size)
    }
    @Test fun `PNG and PNG-contained ICO decode with bounded dimensions`() {
        val png = png()
        assertEquals(4, BrowserSearchIconLoader.decode(png)!!.width)
        val ico = ByteBuffer.allocate(22 + png.size).order(ByteOrder.LITTLE_ENDIAN)
        ico.putShort(0); ico.putShort(1); ico.putShort(1)
        ico.put(4); ico.put(4); ico.put(0); ico.put(0); ico.putShort(1); ico.putShort(32)
        ico.putInt(png.size); ico.putInt(22); ico.put(png)
        assertEquals(4, BrowserSearchIconLoader.decode(ico.array())!!.width)
    }
    @Test fun `32 bit DIB ICO decodes rather than depending on Android ICO support`() {
        val ico = ByteBuffer.allocate(22 + 40 + 16 + 8).order(ByteOrder.LITTLE_ENDIAN)
        ico.putShort(0); ico.putShort(1); ico.putShort(1)
        ico.put(2); ico.put(2); ico.put(0); ico.put(0); ico.putShort(1); ico.putShort(32)
        ico.putInt(64); ico.putInt(22)
        ico.putInt(40); ico.putInt(2); ico.putInt(4); ico.putShort(1); ico.putShort(32)
        repeat(6) { ico.putInt(0) }
        repeat(4) { ico.putInt(Color.RED) }; ico.putLong(0)
        val bitmap = BrowserSearchIconLoader.decode(ico.array())!!
        assertEquals(2, bitmap.width); assertEquals(Color.RED, bitmap.getPixel(0, 0))
    }
    @Test fun `actual Bing Google and custom DuckDuckGo ICO fixtures decode`() {
        listOf("bing", "google", "duckduckgo").forEach { name ->
            val bytes = javaClass.getResourceAsStream("/search-icons/$name.ico")!!.use { it.readBytes() }
            val decoded = BrowserSearchIconLoader.decode(bytes)
            assertNotNull(name, decoded)
            assertTrue(name, decoded!!.width in 1..64 && decoded.height in 1..64)
            assertTrue(name, (0 until decoded.height).any { y ->
                (0 until decoded.width).any { x -> Color.alpha(decoded.getPixel(x, y)) > 0 }
            })
        }
    }

    @Test fun `WebP is decoded and downsampled by bounded native bitmap decoder`() {
        val bytes = ByteArrayOutputStream().apply {
            Bitmap.createBitmap(128, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
                .compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, this)
        }.toByteArray()
        val decoded = BrowserSearchIconLoader.decode(bytes)!!
        assertTrue(decoded.width <= 64 && decoded.height <= 64)
    }

    @Test fun `corrupt and oversized image inputs fail closed`() {
        assertNull(BrowserSearchIconLoader.decode(ByteArray(BrowserSearchIconPolicy.MAX_BYTES + 1)))
        assertNull(BrowserSearchIconLoader.decode(byteArrayOf(0, 0, 1, 0, 32, 0)))
        assertNull(BrowserSearchIconLoader.decode("<html>not an image</html>".toByteArray()))
    }
    @Test fun `invalid origin never requests network`() {
        val loader = BrowserSearchIconLoader(Executor { fail("must not schedule") }, { it() }, { fail("must not fetch"); null })
        val icon = loader.request(null, "Local") {}
        assertEquals(64, icon.bitmap.width)
    }
}
