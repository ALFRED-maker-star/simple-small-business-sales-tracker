package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val price: Double,
    val category: String = "General",
    val type: String = "PRODUCT", // "PRODUCT" or "SERVICE"
    val sku: String = "",
    val isArchived: Boolean = false,
    val salesCount: Int = 0
)
