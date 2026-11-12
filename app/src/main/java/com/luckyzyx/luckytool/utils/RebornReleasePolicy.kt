package com.luckyzyx.luckytool.utils

/** Community releases start at v0.10; earlier releases use the upstream application ID. */
internal object RebornReleasePolicy {
    const val APPLICATION_ID = "io.github.zechariah0825.luckytoolreborn"
    const val REPOSITORY = "https://github.com/Zechariah0825/LuckyTool-Reborn"
    const val API = "https://api.github.com/repos/Zechariah0825/LuckyTool-Reborn/releases/latest"

    data class Asset(val name: String, val url: String, val size: Long, val downloads: Long)
    data class Release(val code: Int, val asset: Asset)

    fun select(tag: String, page: String, draft: Boolean, prerelease: Boolean,
               assets: List<Asset>): Release? {
        if (draft || prerelease || !Regex("v0\\.[0-9]+").matches(tag)) return null
        val revision = tag.removePrefix("v0.").toIntOrNull() ?: return null
        if (revision < 10 || revision > Int.MAX_VALUE - 23301) return null
        if (tag != "v0.$revision" || page != "$REPOSITORY/releases/tag/$tag") return null
        val expectedName = "LuckyTool-Reborn-$tag.apk"
        // Fail closed on duplicates, ZIPs, variant ambiguity, unsafe paths or foreign repositories.
        val asset = assets.filter {
            it.name == expectedName &&
                it.url == "$REPOSITORY/releases/download/$tag/$expectedName" &&
                it.size > 0 && it.downloads >= 0
        }.singleOrNull() ?: return null
        return Release(23301 + revision, asset)
    }

    fun installable(packageName: String?, versionCode: Long, currentCode: Long): Boolean =
        packageName == APPLICATION_ID && versionCode >= 23311 && versionCode > currentCode
}
