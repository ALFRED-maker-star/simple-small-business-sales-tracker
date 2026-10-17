package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val customerName: String? = null,
    val paymentMethod: String = "CASH", // CASH, CARD, MOBILE, TRANSFER
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val receiptNumber: String = "",
    val notes: String? = null,
    val itemsCount: Int = 0
)
