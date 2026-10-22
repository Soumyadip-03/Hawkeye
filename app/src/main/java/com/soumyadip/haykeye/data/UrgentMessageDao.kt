package com.soumyadip.haykeye.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UrgentMessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: UrgentMessage): Long

    @Query("SELECT * FROM urgent_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<UrgentMessage>>

    @Query("SELECT * FROM urgent_messages WHERE provider = :provider ORDER BY timestamp DESC")
    fun getMessagesByProvider(provider: String): Flow<List<UrgentMessage>>

    @Query("UPDATE urgent_messages SET isRead = :isRead WHERE id = :id")
    suspend fun markRead(id: Long, isRead: Boolean = true)

    @Delete
    suspend fun deleteMessage(message: UrgentMessage)

    @Query("DELETE FROM urgent_messages WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM urgent_messages WHERE isRead = 1")
    suspend fun deleteReadMessages(): Int

    @Query("DELETE FROM urgent_messages WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM urgent_messages")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM urgent_messages")
    suspend fun getMessageCount(): Int
}
