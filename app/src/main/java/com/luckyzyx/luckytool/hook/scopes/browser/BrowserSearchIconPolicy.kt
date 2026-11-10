package com.luckyzyx.luckytool.hook.scopes.browser

import java.net.InetAddress
import java.net.URI

/** Requests contain only the configured site's HTTPS origin, never a search path/query. */
object BrowserSearchIconPolicy {
    const val MAX_BYTES = 256 * 1024
    const val MAX_DIMENSION = 1024
    const val ICON_SIZE = 64

    fun origin(template: String): String? { return try {
        val uri = URI(RebornBrowserConfig.validSearchTemplate(template)?.replace("%s", "probe") ?: return null)
        val host = uri.host?.lowercase()?.takeIf {
            it.contains('.') && it != "localhost" && !it.endsWith(".localhost") && !it.endsWith(".local") &&
                !it.contains(':') && !it.all { char -> char.isDigit() || char == '.' }
        } ?: return null
        if (uri.port !in listOf(-1, 443)) null else "https://$host"
    } catch (_: Exception) { null } }

    fun candidates(origin: String) = listOf("$origin/favicon.ico", "$origin/apple-touch-icon.png")

    fun redirect(origin: String, from: String, location: String): String? = try {
        val next = URI(from).resolve(location)
        val base = URI(origin)
        next.toASCIIString().takeIf {
            next.scheme == "https" && next.host?.equals(base.host, true) == true &&
                next.port in listOf(-1, 443) && next.rawUserInfo == null &&
                next.rawQuery == null && next.rawFragment == null && it.length <= 2048
        }
    } catch (_: Exception) { null }

    fun publicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address
        if (bytes.size == 16) {
            if ((0 until 10).all { bytes[it] == 0.toByte() } && bytes[10] == (-1).toByte() && bytes[11] == (-1).toByte()) {
                return publicAddress(InetAddress.getByAddress(bytes.copyOfRange(12, 16)))
            }
            return (bytes[0].toInt() and 0xfe) != 0xfc
        }
        val first = bytes[0].toInt() and 255
        val second = bytes[1].toInt() and 255
        return first != 0 && first < 224 && !(first == 100 && second in 64..127) &&
            !(first == 198 && second in 18..19)
    }

    fun validDimensions(width: Int, height: Int) = width in 1..MAX_DIMENSION && height in 1..MAX_DIMENSION
}
