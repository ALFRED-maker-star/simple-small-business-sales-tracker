package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey
    val id: Int = 1,
    val businessName: String = "SaleSnap Boutique",
    val ownerName: String = "Alex Rivera",
    val email: String = "alex@salesnap.biz",
    val phone: String = "+1 (555) 234-5678",
    val address: String = "104 Market Street, Suite A",
    val currencySymbol: String = "$",
    val taxRatePercent: Double = 0.0,
    val receiptFooter: String = "Thank you for supporting small business!",
    val pinCode: String = "1234",
    val isLoggedIn: Boolean = false
)
