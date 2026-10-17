package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"])]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val saleId: Long,
    val productId: Long? = null,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int = 1,
    val itemTotal: Double = unitPrice * quantity
)
