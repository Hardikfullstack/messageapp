package com.message.sms.texting.app.model

import androidx.room.Embedded

data class MessageSearchResult(
    @Embedded val message: SmsMessage,
    val matchCount: Int
)
