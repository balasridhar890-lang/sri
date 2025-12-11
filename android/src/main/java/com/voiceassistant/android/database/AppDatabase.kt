package com.voiceassistant.android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Call log database entity
 */
@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val direction: String, // "incoming" or "outgoing"
    val timestamp: Long,
    val durationSeconds: Long = 0,
    val success: Boolean = true,
    val errorMessage: String? = null,
    val synced: Boolean = false
)

/**
 * SMS log database entity
 */
@Entity(tableName = "sms_logs")
data class SMSLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val messageBody: String,
    val decision: String, // "yes", "no", or "error"
    val replyText: String,
    val replySent: Boolean = false,
    val timestamp: Long,
    val errorMessage: String? = null,
    val synced: Boolean = false
)

/**
 * Call log DAO
 */
@Dao
interface CallLogDao {
    @Insert
    suspend fun insert(entity: CallLogEntity): Long
    
    @Update
    suspend fun update(entity: CallLogEntity)
    
    @Query("SELECT * FROM call_logs WHERE synced = 0 ORDER BY timestamp DESC")
    suspend fun getUnsyncedLogs(): List<CallLogEntity>
    
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<CallLogEntity>
    
    @Query("SELECT * FROM call_logs WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    suspend fun getByPhoneNumber(phoneNumber: String): List<CallLogEntity>
    
    @Query("DELETE FROM call_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOlderThan(beforeTime: Long)
}

/**
 * SMS log DAO
 */
@Dao
interface SMSLogDao {
    @Insert
    suspend fun insert(entity: SMSLogEntity): Long
    
    @Update
    suspend fun update(entity: SMSLogEntity)
    
    @Query("SELECT * FROM sms_logs WHERE synced = 0 ORDER BY timestamp DESC")
    suspend fun getUnsyncedLogs(): List<SMSLogEntity>
    
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<SMSLogEntity>
    
    @Query("SELECT * FROM sms_logs WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    suspend fun getByPhoneNumber(phoneNumber: String): List<SMSLogEntity>
    
    @Query("DELETE FROM sms_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOlderThan(beforeTime: Long)
}

/**
 * App database
 */
@Database(
    entities = [CallLogEntity::class, SMSLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun smsLogDao(): SMSLogDao
}

/**
 * Dependency injection module for database
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voice_assistant_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Singleton
    @Provides
    fun provideCallLogDao(database: AppDatabase): CallLogDao {
        return database.callLogDao()
    }
    
    @Singleton
    @Provides
    fun provideSMSLogDao(database: AppDatabase): SMSLogDao {
        return database.smsLogDao()
    }
}
