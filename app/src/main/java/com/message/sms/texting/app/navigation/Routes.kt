package com.message.sms.texting.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Routes(val route: String) {
    @Serializable
    object Splash : Routes("splash_screen")
    
    @Serializable
    object Onboarding : Routes("onboarding_screen")
    
    @Serializable
    object Permissions : Routes("permissions_screen")

    @Serializable
    object ScheduledMessages : Routes("scheduled_messages_screen")
    
    @Serializable
    object DefaultSms : Routes("default_sms_screen")

    @Serializable
    object Home : Routes("home_screen")

    @Serializable
    object Search : Routes("search_screen")

    @Serializable
    object Dashboard : Routes("dashboard_screen")

    @Serializable
    object Archived : Routes("archived_screen")
    
    @Serializable
    object StarredMessages : Routes("starred_messages_screen")
    
    @Serializable
    object Settings : Routes("settings_screen")

    @Serializable
    object SwipeActions : Routes("swipe_actions_screen")

    @Serializable
    object NotificationSettings : Routes("notification_settings_screen")

    @Serializable
    object About : Routes("about_screen")

    @Serializable
    object LegalWebView : Routes("legal_webview_screen/{type}") {
        /** [type] is "privacy" or "terms" â€” resolves which remote-config URL to load. */
        fun createRoute(type: String): String = "legal_webview_screen/$type"
    }

    @Serializable
    object ChooseLanguage : Routes("choose_language_screen?firstRun={firstRun}") {
        fun createRoute(firstRun: Boolean = false): String = "choose_language_screen?firstRun=$firstRun"
    }

    @Serializable
    object BlockedMessages : Routes("blocked_messages_screen")

    @Serializable
    object BackupRestore : Routes("backup_restore_screen")

    @Serializable
    object Chat : Routes("chat_screen/{threadId}?address={address}&contactName={contactName}&highlightMsgId={highlightMsgId}&isScheduling={isScheduling}&scheduledMessageId={scheduledMessageId}&searchQuery={searchQuery}&forwardText={forwardText}&groupId={groupId}") {
        fun createRoute(threadId: Long, address: String, contactName: String?, highlightMsgId: Long? = null, isScheduling: Boolean = false, scheduledMessageId: Long? = null, searchQuery: String? = null, forwardText: String? = null, groupId: Long? = null): String {
            val encodedAddress = android.net.Uri.encode(if (address.isEmpty()) " " else address)
            val name = contactName ?: "null"
            val encodedName = android.net.Uri.encode(name)
            var route = "chat_screen/$threadId?address=$encodedAddress&contactName=$encodedName"
            if (highlightMsgId != null) {
                route += "&highlightMsgId=$highlightMsgId"
            }
            if (isScheduling) {
                route += "&isScheduling=true"
            }
            if (scheduledMessageId != null) {
                route += "&scheduledMessageId=$scheduledMessageId"
            }
            if (searchQuery != null) {
                val encodedQuery = android.net.Uri.encode(searchQuery)
                route += "&searchQuery=$encodedQuery"
            }
            if (forwardText != null) {
                val encodedForwardText = android.net.Uri.encode(forwardText)
                route += "&forwardText=$encodedForwardText"
            }
            if (groupId != null) {
                route += "&groupId=$groupId"
            }
            return route
        }
    }

    @Serializable
    object ChatDetails : Routes("chat_details_screen/{threadId}?address={address}&contactName={contactName}&isArchived={isArchived}&groupId={groupId}") {
        fun createRoute(threadId: Long, address: String, contactName: String?, isArchived: Boolean, groupId: Long? = null): String {
            val encodedAddress = android.net.Uri.encode(if (address.isEmpty()) " " else address)
            val name = contactName ?: "null"
            val encodedName = android.net.Uri.encode(name)
            var route = "chat_details_screen/$threadId?address=$encodedAddress&contactName=$encodedName&isArchived=$isArchived"
            if (groupId != null) {
                route += "&groupId=$groupId"
            }
            return route
        }
    }

    @Serializable
    object ContactNotification : Routes("contact_notification_screen/{threadId}?contactName={contactName}") {
        fun createRoute(threadId: Long, contactName: String?): String {
            val name = contactName ?: "null"
            val encodedName = android.net.Uri.encode(name)
            return "contact_notification_screen/$threadId?contactName=$encodedName"
        }
    }

    @Serializable
    object NewChat : Routes("new_chat_screen?isScheduling={isScheduling}&forwardText={forwardText}&groupId={groupId}") {
        fun createRoute(isScheduling: Boolean = false, forwardText: String? = null, groupId: Long? = null): String {
            var route = "new_chat_screen?isScheduling=$isScheduling"
            if (forwardText != null) {
                val encodedForwardText = android.net.Uri.encode(forwardText)
                route += "&forwardText=$encodedForwardText"
            }
            if (groupId != null) {
                route += "&groupId=$groupId"
            }
            return route
        }
    }

    @Serializable
    object ImageViewer : Routes("image_viewer_screen?imagePath={imagePath}&name={name}&date={date}") {
        fun createRoute(imagePath: String, name: String?, date: Long): String {
            val encodedPath = android.net.Uri.encode(imagePath)
            val encodedName = android.net.Uri.encode(name ?: "null")
            return "image_viewer_screen?imagePath=$encodedPath&name=$encodedName&date=$date"
        }
    }

    @Serializable
    object AfterCall : Routes("after_call_screen?address={address}&displayName={displayName}&isKnown={isKnown}&line1={line1}&line2={line2}") {
        fun createRoute(address: String, displayName: String?, isKnownContact: Boolean, callInfoLine1: String, callInfoLine2: String): String {
            val encodedAddress = android.net.Uri.encode(address)
            val encodedName = android.net.Uri.encode(displayName ?: "null")
            val encodedLine1 = android.net.Uri.encode(callInfoLine1)
            val encodedLine2 = android.net.Uri.encode(callInfoLine2)
            return "after_call_screen?address=$encodedAddress&displayName=$encodedName&isKnown=$isKnownContact&line1=$encodedLine1&line2=$encodedLine2"
        }
    }

    @Serializable
    object AddGroupName : Routes("add_group_name_screen?members={members}") {
        fun createRoute(members: List<com.message.sms.texting.app.model.GroupMember>): String {
            val membersJson = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(com.message.sms.texting.app.model.GroupMember.serializer()),
                members
            )
            return "add_group_name_screen?members=${android.net.Uri.encode(membersJson)}"
        }
    }
}
