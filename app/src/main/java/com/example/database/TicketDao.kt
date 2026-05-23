package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun getAllTicketsSync(): List<Ticket>

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun getTicketById(id: Long): Ticket?

    @Query("SELECT * FROM tickets WHERE customerName LIKE :query OR customerPhone LIKE :query OR deviceModel LIKE :query ORDER BY createdAt DESC")
    fun searchTickets(query: String): Flow<List<Ticket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket): Long

    @Update
    suspend fun updateTicket(ticket: Ticket)

    @Delete
    suspend fun deleteTicket(ticket: Ticket)

    @Query("SELECT * FROM settings")
    fun getAllSettingsFlow(): Flow<List<Setting>>

    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSettingByKey(key: String): Setting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: Setting)
}
