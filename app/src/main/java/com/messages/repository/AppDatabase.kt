package com.messages.repository

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.messages.model.DeletedMessage
import com.messages.model.SmsMessage
import com.messages.model.ScheduledMessage
import com.messages.model.BlockedContact
import com.messages.model.Draft
import com.messages.model.Group
import com.messages.model.GroupMessage
import com.messages.model.Reminder
import androidx.room.TypeConverters

@Database(
    entities = [SmsMessage::class, DeletedMessage::class, ScheduledMessage::class, BlockedContact::class, Draft::class, Group::class, GroupMessage::class, Reminder::class],
    version = 11,
    exportSchema = false
)
@TypeConverters(GroupConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun blockedContactDao(): BlockedContactDao
    abstract fun draftDao(): DraftDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // No migrations — app isn't published yet, so a schema bump just wipes and recreates
        // the local DB (fallbackToDestructiveMigration below) instead of maintaining incremental
        // migrations for data nobody has yet.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "messages_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
