package com.luckyzyx.luckytool.utils

import org.junit.Assert.*
import org.junit.Test

class RebornReleasePolicyTest {
    private fun asset(tag: String = "v0.10") = RebornReleasePolicy.Asset(
        "LuckyTool-Reborn-$tag.apk",
        "${RebornReleasePolicy.REPOSITORY}/releases/download/$tag/LuckyTool-Reborn-$tag.apk",
        1234, 0)
    private fun select(tag: String = "v0.10", assets: List<RebornReleasePolicy.Asset> = listOf(asset(tag))) =
        RebornReleasePolicy.select(tag, "${RebornReleasePolicy.REPOSITORY}/releases/tag/$tag",
            false, false, assets)

    @Test fun selectsApkRatherThanFirstAsset() {
        val apk = asset()
        val zip = apk.copy(name = "source.zip")
        assertEquals(23311, select(assets = listOf(zip, apk))?.code)
        assertEquals(apk, select(assets = listOf(zip, apk))?.asset)
        assertEquals(23312, select("v0.11")?.code)
    }

    @Test fun rejectsOldMalformedOrOverflowingVersions() {
        listOf("v0.9", "23310-release", "", "v0.010", "v1.0", "v0.2147483647",
            "v0.999999999999", "v0.10-beta").forEach { assertNull(it, select(it)) }
    }

    @Test fun rejectsAmbiguousForeignAndMissingAssets() {
        val apk = asset()
        assertNull(select(assets = emptyList()))
        assertNull(select(assets = listOf(apk, apk)))
        assertNull(select(assets = listOf(apk.copy(url = "https://evil.example/file.apk"))))
        assertNull(select(assets = listOf(apk.copy(name = "../file.apk"))))
        assertNull(select(assets = listOf(apk.copy(size = 0))))
        assertNull(select(assets = listOf(apk.copy(downloads = -1))))
    }

    @Test fun rejectsDraftPrereleaseAndForeignPage() {
        val page = "${RebornReleasePolicy.REPOSITORY}/releases/tag/v0.10"
        assertNull(RebornReleasePolicy.select("v0.10", page, true, false, listOf(asset())))
        assertNull(RebornReleasePolicy.select("v0.10", page, false, true, listOf(asset())))
        assertNull(RebornReleasePolicy.select("v0.10", "https://evil.example", false, false, listOf(asset())))
    }

    @Test fun installerRequiresNewPackageAndUpgrade() {
        assertTrue(RebornReleasePolicy.installable(RebornReleasePolicy.APPLICATION_ID, 23312, 23311))
        assertFalse(RebornReleasePolicy.installable("com.luckyzyx.luckytool", 23312, 23311))
        assertFalse(RebornReleasePolicy.installable(null, 23312, 23311))
        assertFalse(RebornReleasePolicy.installable(RebornReleasePolicy.APPLICATION_ID, 23310, 23309))
        assertFalse(RebornReleasePolicy.installable(RebornReleasePolicy.APPLICATION_ID, 23311, 23311))
    }
}
