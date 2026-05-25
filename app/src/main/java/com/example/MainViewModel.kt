package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.database.AppDatabase
import com.example.database.Ticket
import com.example.database.TicketRepository
import com.example.utils.LicenseManager
import com.example.workers.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TicketRepository
    
    // States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isActivated = MutableStateFlow(false)
    val isActivated: StateFlow<Boolean> = _isActivated.asStateFlow()

    val deviceId: String = LicenseManager.getDeviceId(application)

    // Reactive Tickets State - updates on database changes or search query modification
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val ticketsState: StateFlow<List<Ticket>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allTickets
            } else {
                repository.searchTickets(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Unfiltered, complete list of tickets specifically for Dashboard / Stats Screen
    val allTicketsState: StateFlow<List<Ticket>> by lazy {
        repository.allTickets
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // Settings map
    private val _settings = MutableStateFlow<Map<String, String>>(emptyMap())
    val settingsState: StateFlow<Map<String, String>> = _settings.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TicketRepository(database.ticketDao())

        // Load local cache immediately for maximum user interface responsiveness
        _isActivated.value = LicenseManager.isLocallyVerified(application)

        // Collect DB settings
        viewModelScope.launch {
            repository.allSettings.collect { settingsList ->
                val map = settingsList.associate { it.key to it.value }
                _settings.value = map
            }
        }

        // Run async internet health check/validation automatically
        viewModelScope.launch {
            val valid = LicenseManager.runIntegrityCheckRoutine(application)
            _isActivated.value = valid
        }
        
        // Enforce scheduling WorkManager
        scheduleReminder()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun checkAndSetActivation(key: String): Boolean {
        val valid = LicenseManager.executeSecuredInitSequence(getApplication(), key)
        if (valid) {
            _isActivated.value = true
        }
        return valid
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
            if (key == "reminder_enabled" || key == "reminder_hour") {
                scheduleReminder()
            }
        }
    }

    fun addTicket(
        customerName: String,
        customerPhone: String,
        deviceModel: String,
        fault: String,
        price: Double,
        advance: Double,
        notes: String,
        signaturePath: String?,
        frontImage: String?,
        backImage: String?,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val remain = maxOf(0.0, price - advance)
            val newTicket = Ticket(
                customerName = customerName,
                customerPhone = customerPhone,
                deviceModel = deviceModel,
                faultDescription = fault,
                totalPrice = price,
                advancePayment = advance,
                remainingAmount = remain,
                notes = notes,
                signaturePath = signaturePath,
                frontImagePath = frontImage,
                backImagePath = backImage,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
            val insertedId = repository.insertTicket(newTicket)
            
            // Automatically launch Telegram message sending if configured
            val botToken = _settings.value["telegram_bot_token"] ?: ""
            val chatId = _settings.value["telegram_chat_id"] ?: ""
            if (botToken.isNotBlank() && chatId.isNotBlank()) {
                val ticketWithId = newTicket.copy(id = insertedId)
                com.example.utils.TelegramManager.sendNewTicket(ticketWithId, botToken, chatId)
            }
            
            onComplete(insertedId)
        }
    }

    fun updateTicketStatus(ticketId: Long, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                val updatedTicket = if (newStatus == "PART_NEEDED") {
                    ticket.copy(
                        status = newStatus,
                        partNeededDate = ticket.partNeededDate ?: System.currentTimeMillis()
                    )
                } else {
                    ticket.copy(
                        status = newStatus,
                        partNeededDate = null
                    )
                }
                repository.updateTicket(updatedTicket)
            }
        }
    }

    fun updateTicket(ticket: Ticket) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTicket(ticket)
        }
    }

    fun scheduleDeviceReminder(ticketId: Long, deviceModel: String, delayMinutes: Int, ringtoneUri: String?) {
        val context = getApplication<Application>()
        try {
            val workManager = WorkManager.getInstance(context)
            val uniqueWorkName = "device_reminder_$ticketId"
            
            val inputData = Data.Builder()
                .putLong("ticket_id", ticketId)
                .putString("device_model", deviceModel)
                .putString("ringtone_uri", ringtoneUri ?: "")
                .build()
            
            val reminderRequest = OneTimeWorkRequestBuilder<com.example.workers.DeviceReminderWorker>()
                .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
                .setInputData(inputData)
                .addTag("device_reminder_tag")
                .build()
            
            workManager.enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteTicket(ticket: Ticket) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTicket(ticket)
        }
    }

    // Schedule WorkManager Background Notification check
    private fun scheduleReminder() {
        val context = getApplication<Application>()
        try {
            val workManager = WorkManager.getInstance(context)
            
            // Cancel existing
            workManager.cancelAllWorkByTag("forgotten_tickets_reminder")
            
            val isEnabled = _settings.value["reminder_enabled"]?.toBoolean() ?: true
            if (!isEnabled) return

            val hour = _settings.value["reminder_hour"]?.toIntOrNull() ?: 10
            
            // Calculate initial delay to run at the specific hour
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val now = Calendar.getInstance()
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // next day
            }
            val delayMinutes = (calendar.timeInMillis - now.timeInMillis) / 1000 / 60

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag("forgotten_tickets_reminder")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "forgotten_tickets_reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                reminderRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
