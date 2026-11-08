package com.zakeleo877.saeedcore.util

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Links {
    /** Null rather than launching a malformed or non-WhatsApp URL. */
    fun whatsapp(number: String, message: String = ""): String? {
        if (!number.matches(Regex("^[1-9][0-9]{7,14}$"))) return null
        val text = URLEncoder.encode(message, StandardCharsets.UTF_8.name()).replace("+", "%20")
        return "https://wa.me/$number" + if (text.isEmpty()) "" else "?text=$text"
    }

    fun safeHttpsUrl(value: String): Boolean = try {
        URI(value).let { it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank() && it.userInfo == null }
    } catch (_: java.net.URISyntaxException) {
        false
    }
}
