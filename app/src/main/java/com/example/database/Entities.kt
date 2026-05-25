package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val customerPhone: String,
    val deviceModel: String,
    val faultDescription: String,
    val totalPrice: Double,
    val advancePayment: Double,
    val remainingAmount: Double,
    val notes: String = "",
    val signaturePath: String? = null,
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, DELIVERED
    val createdAt: Long = System.currentTimeMillis(),
    val partNeededDate: Long? = null
)

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey val key: String,
    val value: String
)
