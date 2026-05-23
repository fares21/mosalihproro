package com.example.database

import kotlinx.coroutines.flow.Flow

class TicketRepository(private val ticketDao: TicketDao) {
    val allTickets: Flow<List<Ticket>> = ticketDao.getAllTickets()
    val allSettings: Flow<List<Setting>> = ticketDao.getAllSettingsFlow()

    suspend fun getTicketById(id: Long): Ticket? {
         return ticketDao.getTicketById(id)
    }

    fun searchTickets(query: String): Flow<List<Ticket>> {
        return ticketDao.searchTickets("%$query%")
    }

    suspend fun insertTicket(ticket: Ticket): Long {
        return ticketDao.insertTicket(ticket)
    }

    suspend fun updateTicket(ticket: Ticket) {
        ticketDao.updateTicket(ticket)
    }

    suspend fun deleteTicket(ticket: Ticket) {
        ticketDao.deleteTicket(ticket)
    }

    suspend fun getSetting(key: String): String? {
        return ticketDao.getSettingByKey(key)?.value
    }

    suspend fun saveSetting(key: String, value: String) {
        ticketDao.insertSetting(Setting(key, value))
    }
}
