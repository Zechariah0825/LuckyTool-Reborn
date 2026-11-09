package com.luckyzyx.luckytool.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class MonochromeColorSchemeTest {
    @Test
    fun `every role stays neutral in light dark and AMOLED`() {
        for ((dark, amoled) in listOf(false to false, true to false, true to true)) {
            for (color in roles(monochromeColorScheme(dark, amoled))) {
                assertEquals(color.red, color.green, 0f)
                assertEquals(color.green, color.blue, 0f)
                assertEquals(1f, color.alpha, 0f)
            }
        }
    }

    @Test
    fun `AMOLED keeps all background containers black`() {
        val scheme = monochromeColorScheme(true, true)
        for (color in listOf(scheme.background, scheme.surface, scheme.surfaceDim,
            scheme.surfaceContainerLowest, scheme.surfaceContainerLow, scheme.surfaceContainer,
            scheme.surfaceContainerHigh, scheme.surfaceContainerHighest)) {
            assertEquals(Color.Black, color)
        }
        assertEquals(roles(monochromeColorScheme(false)), roles(monochromeColorScheme(false, true)))
    }

    @Test
    fun `text retains AA contrast on principal backgrounds`() {
        for (scheme in listOf(monochromeColorScheme(false), monochromeColorScheme(true),
            monochromeColorScheme(true, true))) {
            for ((background, foreground) in listOf(scheme.primary to scheme.onPrimary,
                scheme.secondary to scheme.onSecondary, scheme.tertiary to scheme.onTertiary,
                scheme.background to scheme.onBackground, scheme.surface to scheme.onSurface,
                scheme.secondaryContainer to scheme.onSecondaryContainer,
                scheme.errorContainer to scheme.onErrorContainer,
                scheme.inverseSurface to scheme.inverseOnSurface)) {
                val a = background.luminance()
                val b = foreground.luminance()
                assertTrue((maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f) >= 4.5f)
            }
        }
    }

    @Test
    fun `explicit color preferences are not overridden`() {
        assertTrue(AppSettings().usesMonochrome)
        assertFalse(AppSettings(dynamicColor = true).usesMonochrome)
        assertFalse(AppSettings(keyColor = 0xFF0066FF.toInt()).usesMonochrome)
    }

    private fun roles(scheme: ColorScheme) = with(scheme) {
        listOf(
            primary,
            onPrimary,
            primaryContainer,
            onPrimaryContainer,
            inversePrimary,
            secondary,
            onSecondary,
            secondaryContainer,
            onSecondaryContainer,
            tertiary,
            onTertiary,
            tertiaryContainer,
            onTertiaryContainer,
            background,
            onBackground,
            surface,
            onSurface,
            surfaceVariant,
            onSurfaceVariant,
            surfaceTint,
            inverseSurface,
            inverseOnSurface,
            error,
            onError,
            errorContainer,
            onErrorContainer,
            outline,
            outlineVariant,
            scrim,
            surfaceBright,
            surfaceDim,
            surfaceContainer,
            surfaceContainerLow,
            surfaceContainerLowest,
            surfaceContainerHigh,
            surfaceContainerHighest,
            primaryFixed,
            primaryFixedDim,
            onPrimaryFixed,
            onPrimaryFixedVariant,
            secondaryFixed,
            secondaryFixedDim,
            onSecondaryFixed,
            onSecondaryFixedVariant,
            tertiaryFixed,
            tertiaryFixedDim,
            onTertiaryFixed,
            onTertiaryFixedVariant,
        )
    }
}
