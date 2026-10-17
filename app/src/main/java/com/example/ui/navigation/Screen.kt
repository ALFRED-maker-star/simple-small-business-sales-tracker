package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val title: String,
    val icon: ImageVector? = null,
    val isBottomNav: Boolean = false
) {
    LOGIN("Owner Login"),
    DASHBOARD("Dashboard", Icons.Default.Dashboard, isBottomNav = true),
    RECORD_SALE("Record Sale", Icons.Default.AddShoppingCart, isBottomNav = true),
    SALES_LIST("Sales", Icons.Default.ReceiptLong, isBottomNav = true),
    PRODUCTS("Products", Icons.Default.Inventory2, isBottomNav = true),
    REPORTS("Reports", Icons.Default.Assessment, isBottomNav = true),
    SETTINGS("Settings", Icons.Default.Settings, isBottomNav = false);

    companion object {
        val bottomNavScreens = listOf(DASHBOARD, RECORD_SALE, SALES_LIST, PRODUCTS, REPORTS)
    }
}
