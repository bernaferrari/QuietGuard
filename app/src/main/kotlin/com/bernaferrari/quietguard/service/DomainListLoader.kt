package com.bernaferrari.quietguard.service

import java.io.File
import java.io.IOException

/**
 * Parses hosts-style domain lists without leaking file or line-handling details into the VPN
 * service.
 */
internal object DomainListLoader {
    private val whitespace = Regex("\\s+")

    fun load(
        file: File,
        onInvalidLine: (String) -> Unit,
    ): Set<String>? =
        try {
            file.bufferedReader().useLines { lines ->
                buildSet {
                    lines.forEach { rawLine ->
                        val fields = rawLine.substringBefore('#').trim().split(whitespace)
                        if (fields.size == 1 && fields.single().isEmpty()) return@forEach
                        if (fields.size < 2) {
                            onInvalidLine(rawLine.trim())
                        } else {
                            addAll(fields.drop(1))
                        }
                    }
                }
            }
        } catch (_: IOException) {
            null
        }
}
