package com.message.sms.texting.app.ui.theme

/**
 * When the After Call overlay opens a chat (dismissing itself + launching MainActivity, since
 * ChatScreen can't run inside the overlay window), this remembers that context so pressing back
 * from that chat can bring the After Call screen back up instead of just landing on Home.
 * One-shot: consumed (nulled out) the moment the target Chat screen reads it.
 */
object AfterCallReturnState {
    data class Info(
        val address: String,
        val displayName: String?,
        val isKnownContact: Boolean,
        val callInfoLine1: String,
        val callInfoLine2: String
    )

    var pending: Info? = null
}
