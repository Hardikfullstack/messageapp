package com.message.sms.texting.app.mms

import java.io.ByteArrayOutputStream

/**
 * Minimal encoder for an MMS "M-Send.req" PDU (WAP-230-WSP / OMA-MMS-ENC encapsulation).
 * Builds just enough of the spec to carry one recipient, an optional text part and one
 * image part â€” no charset/parameter extensions, since those are the parts most likely to
 * be gotten subtly wrong without a real carrier to test against.
 */
object PduComposer {

    private const val WSP_QUOTE = 0x7F

    private const val FIELD_MESSAGE_TYPE = 0x8C
    private const val FIELD_TRANSACTION_ID = 0x98
    private const val FIELD_MMS_VERSION = 0x8D
    private const val FIELD_DATE = 0x85
    private const val FIELD_TO = 0x97
    private const val FIELD_CONTENT_TYPE = 0x84
    private const val FIELD_CONTENT_LOCATION = 0x83

    private const val MESSAGE_TYPE_SEND_REQ = 0x80
    private const val MMS_VERSION_1_2 = 0x92

    private fun uintVar(value: Long): ByteArray {
        require(value >= 0)
        var v = value
        val groups = ArrayList<Int>()
        groups.add((v and 0x7F).toInt())
        v = v shr 7
        while (v > 0) {
            groups.add(((v and 0x7F) or 0x80).toInt())
            v = v shr 7
        }
        return groups.reversed().map { it.toByte() }.toByteArray()
    }

    private fun textString(s: String): ByteArray {
        val bytes = s.toByteArray(Charsets.UTF_8)
        val needsQuote = bytes.isNotEmpty() && ((bytes[0].toInt() and 0xFF) >= 0x80 || (bytes[0].toInt() and 0xFF) == WSP_QUOTE)
        val out = ByteArrayOutputStream()
        if (needsQuote) out.write(WSP_QUOTE)
        out.write(bytes)
        out.write(0x00)
        return out.toByteArray()
    }

    private fun longInteger(value: Long): ByteArray {
        var v = value
        val bytes = ArrayList<Byte>()
        if (v == 0L) bytes.add(0)
        while (v > 0) {
            bytes.add(0, (v and 0xFF).toByte())
            v = v shr 8
        }
        val out = ByteArrayOutputStream()
        out.write(bytes.size)
        bytes.forEach { out.write(it.toInt()) }
        return out.toByteArray()
    }

    data class Part(val contentType: String, val fileName: String, val data: ByteArray)

    fun buildSendReqPdu(
        recipient: String,
        text: String?,
        imageBytes: ByteArray,
        imageMimeType: String,
        transactionId: String
    ): ByteArray {
        val out = ByteArrayOutputStream()

        out.write(FIELD_MESSAGE_TYPE)
        out.write(MESSAGE_TYPE_SEND_REQ)

        out.write(FIELD_TRANSACTION_ID)
        out.write(textString(transactionId))

        out.write(FIELD_MMS_VERSION)
        out.write(MMS_VERSION_1_2)

        out.write(FIELD_DATE)
        out.write(longInteger(System.currentTimeMillis() / 1000))

        out.write(FIELD_TO)
        out.write(textString("$recipient/TYPE=PLMN"))

        out.write(FIELD_CONTENT_TYPE)
        out.write(textString("application/vnd.wap.multipart.mixed"))

        val parts = mutableListOf<Part>()
        if (!text.isNullOrBlank()) {
            parts.add(Part("text/plain", "text.txt", text.toByteArray(Charsets.UTF_8)))
        }
        parts.add(Part(imageMimeType, "image.jpg", imageBytes))

        out.write(uintVar(parts.size.toLong()))
        for (part in parts) {
            val partHeaders = ByteArrayOutputStream()
            partHeaders.write(textString(part.contentType))
            partHeaders.write(FIELD_CONTENT_LOCATION)
            partHeaders.write(textString(part.fileName))
            val headerBytes = partHeaders.toByteArray()

            out.write(uintVar(headerBytes.size.toLong()))
            out.write(uintVar(part.data.size.toLong()))
            out.write(headerBytes)
            out.write(part.data)
        }

        return out.toByteArray()
    }
}
