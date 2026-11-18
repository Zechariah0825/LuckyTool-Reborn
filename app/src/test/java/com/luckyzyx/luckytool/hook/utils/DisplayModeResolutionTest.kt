package com.luckyzyx.luckytool.hook.utils

import android.app.Application
import android.os.Binder
import android.os.IBinder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DisplayModeResolutionTest {

    class FakeFrameworkMode(
        @JvmField val id: Int,
        @JvmField val width: Int,
        @JvmField val height: Int,
        @JvmField val xDpi: Float,
        @JvmField val yDpi: Float,
        @JvmField val peakRefreshRate: Float,
    )

    class FakeDynamicDisplayInfo(
        @JvmField val supportedDisplayModes: Array<FakeFrameworkMode>,
    )

    class PhysicalAddress(private val displayId: Long) {
        fun getPhysicalDisplayId(): Long = displayId
    }

    class StablePhysicalAddress(private val displayId: Long) {
        fun getPhysicalDisplayId(): Long = displayId
    }

    open class BasePhysicalAddress(private val displayId: Long) {
        fun getPhysicalDisplayId(): Long = displayId
    }

    class SubclassPhysicalAddress(displayId: Long) : BasePhysicalAddress(displayId)

    class NetworkAddress

    class ModernSurfaceControl {
        companion object {
            var lastRequestedDisplayId: Long? = null
            var nextResult: Any? = null

            fun reset() {
                lastRequestedDisplayId = null
                nextResult = null
            }

            @JvmStatic
            fun getDynamicDisplayInfo(displayId: Long): Any? {
                lastRequestedDisplayId = displayId
                return nextResult
            }
        }
    }

    class LegacySurfaceControl {
        companion object {
            var lastRequestedPhysicalId: Long? = null
            var lastRequestedToken: IBinder? = null
            var physicalTokenResult: IBinder? = null
            var internalTokenResult: IBinder? = null
            var nextResult: Any? = null

            fun reset() {
                lastRequestedPhysicalId = null
                lastRequestedToken = null
                physicalTokenResult = null
                internalTokenResult = null
                nextResult = null
            }

            @JvmStatic
            fun getPhysicalDisplayToken(physicalDisplayId: Long): IBinder? {
                lastRequestedPhysicalId = physicalDisplayId
                return physicalTokenResult
            }

            @JvmStatic
            fun getInternalDisplayToken(): IBinder? {
                return internalTokenResult
            }

            @JvmStatic
            fun getDynamicDisplayInfo(displayToken: IBinder?): Any? {
                lastRequestedToken = displayToken
                return nextResult
            }
        }
    }

    class TransitionalSurfaceControl {
        companion object {
            var lastRequestedToken: IBinder? = null
            var internalTokenResult: IBinder? = null
            var nextResult: Any? = null

            fun reset() {
                lastRequestedToken = null
                internalTokenResult = null
                nextResult = null
            }

            @JvmStatic
            fun getInternalDisplayToken(): IBinder? {
                return internalTokenResult
            }

            @JvmStatic
            fun getDynamicDisplayInfo(displayToken: IBinder?): Any? {
                lastRequestedToken = displayToken
                return nextResult
            }
        }
    }

    private val testClassLoader: ClassLoader
        get() = DisplayModeResolutionTest::class.java.classLoader!!

    @Before
    fun setUp() {
        ModernSurfaceControl.reset()
        LegacySurfaceControl.reset()
        TransitionalSurfaceControl.reset()
    }

    @Test
    fun `modern SurfaceControl without IBinder overload returns false and resolves dynamic info`() {
        val physicalId = 4630947090715748499L
        val expectedInfo = FakeDynamicDisplayInfo(
            arrayOf(
                FakeFrameworkMode(0, 1440, 3168, 510f, 510f, 120f),
                FakeFrameworkMode(1, 1440, 3168, 510f, 510f, 144f),
                FakeFrameworkMode(2, 1080, 2376, 382f, 382f, 60f),
            )
        )
        ModernSurfaceControl.nextResult = expectedInfo

        val surfaceControl = SurfaceControlUtils(testClassLoader, ModernSurfaceControl::class.java)
        val displayManager = DisplayManagerUtils(testClassLoader)

        assertFalse(surfaceControl.isDisplayToken())

        for (address in listOf(
            PhysicalAddress(physicalId),
            StablePhysicalAddress(physicalId),
            SubclassPhysicalAddress(physicalId),
        )) {
            ModernSurfaceControl.lastRequestedDisplayId = null
            val resolved = displayManager.resolveDynamicDisplayInfo(address, surfaceControl)
            assertSame(expectedInfo, resolved)
            assertEquals(physicalId, ModernSurfaceControl.lastRequestedDisplayId)

            val parsedModes = DynamicDisplayInfoUtils(resolved!!).let { utils ->
                utils.getSupportedDisplayModes().mapNotNull { utils.getDisplayMode(it)?.second }
            }
            assertEquals(3, parsedModes.size)
            assertEquals(1440, parsedModes[0].width)
            assertEquals(120f, parsedModes[0].refreshRate)
            assertEquals(144f, parsedModes[1].refreshRate)
            assertEquals(1080, parsedModes[2].width)
            assertEquals(60f, parsedModes[2].refreshRate)
        }
    }

    @Test
    fun `legacy SurfaceControl resolves physical token and falls back to internal token`() {
        val physicalToken = Binder()
        val internalToken = Binder()
        val expectedInfo = FakeDynamicDisplayInfo(emptyArray())
        LegacySurfaceControl.physicalTokenResult = physicalToken
        LegacySurfaceControl.internalTokenResult = internalToken
        LegacySurfaceControl.nextResult = expectedInfo

        val surfaceControl = SurfaceControlUtils(testClassLoader, LegacySurfaceControl::class.java)
        val displayManager = DisplayManagerUtils(testClassLoader)

        assertTrue(surfaceControl.isDisplayToken())

        val physicalId = 4630947090715748499L
        assertSame(
            expectedInfo,
            displayManager.resolveDynamicDisplayInfo(PhysicalAddress(physicalId), surfaceControl)
        )
        assertEquals(physicalId, LegacySurfaceControl.lastRequestedPhysicalId)
        assertSame(physicalToken, LegacySurfaceControl.lastRequestedToken)

        LegacySurfaceControl.lastRequestedPhysicalId = null
        LegacySurfaceControl.lastRequestedToken = null
        assertSame(
            expectedInfo,
            displayManager.resolveDynamicDisplayInfo(PhysicalAddress(0L), surfaceControl)
        )
        assertNull(LegacySurfaceControl.lastRequestedPhysicalId)
        assertSame(internalToken, LegacySurfaceControl.lastRequestedToken)

        LegacySurfaceControl.lastRequestedPhysicalId = null
        LegacySurfaceControl.lastRequestedToken = null
        assertSame(
            expectedInfo,
            displayManager.resolveDynamicDisplayInfo(null, surfaceControl)
        )
        assertNull(LegacySurfaceControl.lastRequestedPhysicalId)
        assertSame(internalToken, LegacySurfaceControl.lastRequestedToken)
    }

    @Test
    fun `transitional SurfaceControl without getPhysicalDisplayToken falls back to internal token`() {
        val internalToken = Binder()
        val expectedInfo = FakeDynamicDisplayInfo(emptyArray())
        TransitionalSurfaceControl.internalTokenResult = internalToken
        TransitionalSurfaceControl.nextResult = expectedInfo

        val surfaceControl =
            SurfaceControlUtils(testClassLoader, TransitionalSurfaceControl::class.java)
        val displayManager = DisplayManagerUtils(testClassLoader)

        assertTrue(surfaceControl.isDisplayToken())
        assertNull(surfaceControl.getPhysicalDisplayToken(4630947090715748499L))

        assertSame(
            expectedInfo,
            displayManager.resolveDynamicDisplayInfo(
                PhysicalAddress(4630947090715748499L),
                surfaceControl
            )
        )
        assertSame(internalToken, TransitionalSurfaceControl.lastRequestedToken)
    }

    @Test
    fun `non-physical DisplayAddress returns null physicalDisplayId without throwing`() {
        val expectedInfo = FakeDynamicDisplayInfo(emptyArray())
        ModernSurfaceControl.nextResult = expectedInfo

        val surfaceControl = SurfaceControlUtils(testClassLoader, ModernSurfaceControl::class.java)
        val displayManager = DisplayManagerUtils(testClassLoader)

        assertNull(displayManager.getPhysicalDisplayId(NetworkAddress()))
        assertSame(
            expectedInfo,
            displayManager.resolveDynamicDisplayInfo(NetworkAddress(), surfaceControl)
        )
        assertEquals(-1L, ModernSurfaceControl.lastRequestedDisplayId)
    }

    @Test
    fun `null displayId returns null without invoking reflection`() {
        val surfaceControl = SurfaceControlUtils(testClassLoader, ModernSurfaceControl::class.java)
        assertNull(surfaceControl.getDynamicDisplayInfo(null as Long?))
        assertNull(ModernSurfaceControl.lastRequestedDisplayId)
    }
}
