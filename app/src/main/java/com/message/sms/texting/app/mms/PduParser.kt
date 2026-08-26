package com.message.sms.texting.app.mms

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Minimal decoder for the two incoming MMS PDUs we care about: M-Notification.ind (the small
 * "you have an MMS waiting" push, which carries the download URL) and M-Retrieve.conf (the full
 * message, fetched separately via [MmsDownloader]). Mirrors [PduComposer]'s scope â€” just enough
 * of WAP-230-WSP to reliably pull out Content-Location / From / text / one image part.
 */
object PduParser {

    // Field codes with the header's high bit already stripped.
    private const val FIELD_FROM = 0x09
    private const val FIELD_TRANSACTION_ID = 0x18
    private const val FIELD_CONTENT_TYPE = 0x04
    private const val FIELD_CONTENT_LOCATION = 0x03

    data class Notification(val transactionId: String?, val contentLocation: String?)

    data class RetrievedMessage(
        val from: String?,
        val text: String,
        val imageBytes: ByteArray?,
        val imageMimeType: String?
    )

    private fun readUintVar(stream: ByteArrayInputStream): Long {
        var value = 0L
        while (true) {
            val b = stream.read()
            if (b == -1) break
            value = (value shl 7) or (b.toLong() and 0x7F)
            if (b and 0x80 == 0) break
        }
        return value
    }

    private fun readFully(stream: ByteArrayInputStream, len: Int): ByteArray {
        if (len <= 0) return ByteArray(0)
        val bytes = ByteArray(len)
        var offset = 0
        while (offset < len) {
            val read = stream.read(bytes, offset, len - offset)
            if (read <= 0) break
            offset += read
        }
        return bytes
    }

    private fun readTextStringBytes(stream: ByteArrayInputStream): ByteArray {
        var b = stream.read()
        if (b == 0x7F) { // Quote octet â€” value follows, not part of the text
            b = stream.read()
        }
        val out = ByteArrayOutputStream()
        while (b > 0 && b != -1) {
            out.write(b)
            b = stream.read()
        }
        return out.toByteArray()
    }

    /**
     * Reads one header value using whichever of the three common WSP value grammars applies
     * (Value-length-prefixed block, single-byte Short-Integer, or NUL-terminated Text-string),
     * returning the raw payload bytes. This covers every header we don't need to interpret
     * (Expiry, Message-Class, Message-Size, ...) as well as the ones we do (From, Content-Location).
     */
    private fun readGenericValue(stream: ByteArrayInputStream): ByteArray {
        stream.mark(1)
        val first = stream.read()
        if (first == -1) return ByteArray(0)
        return when {
            first in 0..31 -> {
                val len = if (first == 0x1F) readUintVar(stream).toInt() else first
                readFully(stream, len)
            }
            first >= 0x80 -> byteArrayOf(first.toByte())
            else -> {
                stream.reset()
                readTextStringBytes(stream)
            }
        }
    }

    private fun decodeFromHeader(raw: ByteArray?): String? {
        if (raw == null || raw.isEmpty()) return null
        val marker = raw[0].toInt() and 0xFF
        val textBytes = when (marker) {
            0x81 -> return null // insert-address-token: sender withheld from this PDU
            0x80 -> raw.copyOfRange(1, raw.size)
            else -> raw
        }
        return textBytes.toString(Charsets.UTF_8).substringBefore("/TYPE=").ifBlank { null }
    }

    fun parseNotificationInd(data: ByteArray): Notification {
        val stream = ByteArrayInputStream(data)
        val headers = mutableMapOf<Int, ByteArray>()
        while (stream.available() > 0) {
            val fieldByte = stream.read()
            if (fieldByte == -1) break
            val fieldCode = fieldByte and 0x7F
            try {
                headers[fieldCode] = readGenericValue(stream)
            } catch (e: Exception) {
                break
            }
        }
        return Notification(
            transactionId = headers[FIELD_TRANSACTION_ID]?.toString(Charsets.UTF_8),
            contentLocation = headers[FIELD_CONTENT_LOCATION]?.toString(Charsets.UTF_8)
        )
    }

    fun parseRetrieveConf(data: ByteArray): RetrievedMessage {
        val stream = ByteArrayInputStream(data)
        val headers = mutableMapOf<Int, ByteArray>()
        val text = StringBuilder()
        var imageBytes: ByteArray? = null
        var imageMime: String? = null

        while (stream.available() > 0) {
            val fieldByte = stream.read()
            if (fieldByte == -1) break
            val fieldCode = fieldByte and 0x7F

            if (fieldCode == FIELD_CONTENT_TYPE) {
                // Content-Type value, then the multipart body immediately follows â€” this is
                // always the last part of an M-Retrieve.conf, so we're done after this.
                try {
                    readGenericValue(stream)
                    val partCount = readUintVar(stream).toInt()
                    repeat(partCount) {
                        val headersLen = readUintVar(stream).toInt()
                        val dataLen = readUintVar(stream).toInt()
                        val partHeaderBytes = readFully(stream, headersLen)
                        val partData = readFully(stream, dataLen)
                        val partContentType = readGenericValue(ByteArrayInputStream(partHeaderBytes))
                            .toString(Charsets.UTF_8)
                        when {
                            partContentType.startsWith("text/") -> {
                                if (text.isNotEmpty()) text.append("\n")
                                text.append(partData.toString(Charsets.UTF_8))
                            }
                            partContentType.startsWith("image/") && imageBytes == null -> {
                                imageBytes = partData
                                imageMime = partContentType
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Best-effort: keep whatever we managed to parse before the failure.
                }
                break
            } else {
                try {
                    headers[fieldCode] = readGenericValue(stream)
                } catch (e: Exception) {
                    break
                }
            }
        }

        return RetrievedMessage(
            from = decodeFromHeader(headers[FIELD_FROM]),
            text = text.toString(),
            imageBytes = imageBytes,
            imageMimeType = imageMime
        )
    }
}
