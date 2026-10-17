package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ReceiptDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.RecordSaleScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberDark
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.SaleSnapTheme
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.SaleSnapViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SaleSnapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaleSnapTheme {
                SaleSnapApp(viewModel)
            }
        }
    }
}

@Composable
fun SaleSnapApp(viewModel: SaleSnapViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val customerName by viewModel.customerName.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val saleNotes by viewModel.saleNotes.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val reportsState by viewModel.reportsState.collectAsStateWithLifecycle()
    val filteredSales by viewModel.filteredSalesList.collectAsStateWithLifecycle()
    val salesSearchQuery by viewModel.salesSearchQuery.collectAsStateWithLifecycle()
    val salesDateFilter by viewModel.salesDateFilter.collectAsStateWithLifecycle()
    val reportsDateFilter by viewModel.reportsDateFilter.collectAsStateWithLifecycle()
    val showProductDialog by viewModel.showProductDialog.collectAsStateWithLifecycle()
    val editingProduct by viewModel.editingProduct.collectAsStateWithLifecycle()
    val showReceiptDialog by viewModel.showReceiptDialog.collectAsStateWithLifecycle()
    val completedSale by viewModel.completedSale.collectAsStateWithLifecycle()
    val selectedSaleDetail by viewModel.selectedSaleDetail.collectAsStateWithLifecycle()

    val currency = profile?.currencySymbol ?: "$"
    val businessName = profile?.businessName ?: "SaleSnap Store"
    val receiptFooter = profile?.receiptFooter ?: "Thank you for shopping local!"

    // BackHandler for secondary screens
    if (currentScreen != Screen.DASHBOARD && currentScreen != Screen.LOGIN) {
        BackHandler {
            viewModel.navigateTo(Screen.DASHBOARD)
        }
    }

    if (currentScreen == Screen.LOGIN) {
        LoginScreen(
            initialOwnerName = profile?.ownerName ?: "Alex Rivera",
            initialBusinessName = profile?.businessName ?: "SaleSnap Artisan Cafe",
            onLoginSuccess = { owner, biz, pin ->
                viewModel.loginOwner(owner, biz, pin) {}
            },
            onQuickDemo = {
                viewModel.quickDemoLogin()
            }
        )
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            bottomBar = {
                if (currentScreen != Screen.SETTINGS) {
                    NavigationBar(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .shadow(elevation = 12.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = TextDark
                    ) {
                        Screen.bottomNavScreens.forEach { screen ->
                            val isSelected = currentScreen == screen
                            val isRecordSale = screen == Screen.RECORD_SALE

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(screen) },
                                icon = {
                                    if (isRecordSale) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) AmberAccent else EmeraldPrimary),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddShoppingCart,
                                                contentDescription = screen.title,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        screen.icon?.let { icon ->
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = screen.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldDark,
                                    selectedTextColor = EmeraldDark,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = EmeraldContainer
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.DASHBOARD -> {
                        DashboardScreen(
                            dashboardState = dashboardState,
                            profile = profile,
                            onNavigateToRecordSale = { viewModel.navigateTo(Screen.RECORD_SALE) },
                            onNavigateToSalesList = { viewModel.navigateTo(Screen.SALES_LIST) },
                            onNavigateToSettings = { viewModel.navigateTo(Screen.SETTINGS) },
                            onSelectSale = { saleWithItems ->
                                viewModel.showSaleDetail(saleWithItems)
                            }
                        )
                    }

                    Screen.RECORD_SALE -> {
                        RecordSaleScreen(
                            products = products,
                            cart = cart,
                            customerName = customerName,
                            paymentMethod = paymentMethod,
                            saleNotes = saleNotes,
                            currencySymbol = currency,
                            onAddToCart = { viewModel.addToCart(it) },
                            onDecrementFromCart = { viewModel.decrementFromCart(it) },
                            onRemoveFromCart = { viewModel.removeFromCart(it) },
                            onClearCart = { viewModel.clearCart() },
                            onCustomerNameChange = { viewModel.setCustomerName(it) },
                            onPaymentMethodChange = { viewModel.setPaymentMethod(it) },
                            onSaleNotesChange = { viewModel.setSaleNotes(it) },
                            onCompleteSale = { viewModel.completeSale() },
                            onNavigateToAddProduct = {
                                viewModel.navigateTo(Screen.PRODUCTS)
                                viewModel.openAddProductDialog()
                            }
                        )
                    }

                    Screen.SALES_LIST -> {
                        SalesListScreen(
                            sales = filteredSales,
                            searchQuery = salesSearchQuery,
                            dateFilter = salesDateFilter,
                            currencySymbol = currency,
                            onSearchChange = { viewModel.setSalesSearchQuery(it) },
                            onDateFilterChange = { viewModel.setSalesDateFilter(it) },
                            onSelectSale = { viewModel.showSaleDetail(it) },
                            onDeleteSale = { viewModel.deleteSale(it) }
                        )
                    }

                    Screen.PRODUCTS -> {
                        ProductsScreen(
                            products = products,
                            currencySymbol = currency,
                            onAddProductClick = { viewModel.openAddProductDialog() },
                            onEditProductClick = { viewModel.openEditProductDialog(it) },
                            onDeleteProduct = { viewModel.deleteProduct(it) },
                            showDialog = showProductDialog,
                            editingProduct = editingProduct,
                            onDismissDialog = { viewModel.dismissProductDialog() },
                            onSaveProduct = { name, price, category, type, sku ->
                                viewModel.saveProduct(name, price, category, type, sku)
                            }
                        )
                    }

                    Screen.REPORTS -> {
                        ReportsScreen(
                            reportsState = reportsState,
                            dateFilter = reportsDateFilter,
                            currencySymbol = currency,
                            onDateFilterChange = { viewModel.setReportsDateFilter(it) },
                            onExportCsv = { viewModel.exportCsv() }
                        )
                    }

                    Screen.SETTINGS -> {
                        SettingsScreen(
                            profile = profile,
                            onSaveProfile = { name, owner, email, phone, addr, curr, tax, footer ->
                                viewModel.updateBusinessProfile(name, owner, email, phone, addr, curr, tax, footer)
                            },
                            onSeedSampleData = { viewModel.seedSampleData() },
                            onClearAllData = { viewModel.clearAllData() },
                            onLogout = { viewModel.logout() },
                            onBack = { viewModel.navigateTo(Screen.DASHBOARD) }
                        )
                    }

                    Screen.LOGIN -> {}
                }
            }
        }
    }

    // Receipt Modal for just-completed sale
    if (showReceiptDialog && completedSale != null) {
        ReceiptDialog(
            saleWithItems = completedSale!!,
            currencySymbol = currency,
            businessName = businessName,
            receiptFooter = receiptFooter,
            onDismiss = { viewModel.dismissReceiptDialog() }
        )
    }

    // Receipt Modal for selected past sale detail
    if (selectedSaleDetail != null) {
        ReceiptDialog(
            saleWithItems = selectedSaleDetail!!,
            currencySymbol = currency,
            businessName = businessName,
            receiptFooter = receiptFooter,
            onDismiss = { viewModel.dismissSaleDetail() }
        )
    }
}
