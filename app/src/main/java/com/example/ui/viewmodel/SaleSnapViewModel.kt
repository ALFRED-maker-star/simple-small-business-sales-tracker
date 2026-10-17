package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.BusinessProfile
import com.example.data.entity.Product
import com.example.data.entity.SaleWithItems
import com.example.data.repository.DaySalesMetric
import com.example.data.repository.PaymentMethodBreakdown
import com.example.data.repository.SaleSnapRepository
import com.example.data.repository.TopProductMetric
import com.example.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class SalesDateFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("7 Days"),
    THIS_MONTH("This Month"),
    ALL("All Time")
}

enum class ReportsDateFilter(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class DashboardUiState(
    val todayRevenue: Double = 0.0,
    val todayTransactionsCount: Int = 0,
    val todayAvgSale: Double = 0.0,
    val todayItemsSold: Int = 0,
    val last7DaysMetrics: List<DaySalesMetric> = emptyList(),
    val recentSales: List<SaleWithItems> = emptyList()
)

data class ReportsUiState(
    val totalRevenue: Double = 0.0,
    val totalTransactions: Int = 0,
    val averageSaleValue: Double = 0.0,
    val totalItemsSold: Int = 0,
    val topSellingItems: List<TopProductMetric> = emptyList(),
    val dailyMetrics: List<DaySalesMetric> = emptyList(),
    val paymentBreakdown: List<PaymentMethodBreakdown> = emptyList()
)

class SaleSnapViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = SaleSnapRepository(
        productDao = db.productDao(),
        saleDao = db.saleDao(),
        profileDao = db.businessProfileDao(),
        context = application
    )

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Authentication state
    val profile: StateFlow<BusinessProfile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val products: StateFlow<List<Product>> = repository.activeProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<SaleWithItems>> = repository.allSalesWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Record Sale State ---
    // Product ID -> Quantity in Cart
    private val _cart = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val cart: StateFlow<Map<Long, Int>> = _cart.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _paymentMethod = MutableStateFlow("CASH")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _saleNotes = MutableStateFlow("")
    val saleNotes: StateFlow<String> = _saleNotes.asStateFlow()

    private val _selectedProductCategory = MutableStateFlow("All")
    val selectedProductCategory: StateFlow<String> = _selectedProductCategory.asStateFlow()

    private val _completedSale = MutableStateFlow<SaleWithItems?>(null)
    val completedSale: StateFlow<SaleWithItems?> = _completedSale.asStateFlow()

    private val _showReceiptDialog = MutableStateFlow(false)
    val showReceiptDialog: StateFlow<Boolean> = _showReceiptDialog.asStateFlow()

    // --- Sales List Filtering ---
    private val _salesSearchQuery = MutableStateFlow("")
    val salesSearchQuery: StateFlow<String> = _salesSearchQuery.asStateFlow()

    private val _salesDateFilter = MutableStateFlow(SalesDateFilter.ALL)
    val salesDateFilter: StateFlow<SalesDateFilter> = _salesDateFilter.asStateFlow()

    private val _selectedSaleDetail = MutableStateFlow<SaleWithItems?>(null)
    val selectedSaleDetail: StateFlow<SaleWithItems?> = _selectedSaleDetail.asStateFlow()

    // --- Reports Filtering ---
    private val _reportsDateFilter = MutableStateFlow(ReportsDateFilter.LAST_7_DAYS)
    val reportsDateFilter: StateFlow<ReportsDateFilter> = _reportsDateFilter.asStateFlow()

    // --- Product Management Dialog ---
    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct: StateFlow<Product?> = _editingProduct.asStateFlow()

    private val _showProductDialog = MutableStateFlow(false)
    val showProductDialog: StateFlow<Boolean> = _showProductDialog.asStateFlow()

    // --- Dashboard Combined State ---
    val dashboardState: StateFlow<DashboardUiState> = allSales.combine(products) { sales, _ ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        val todaySales = sales.filter { it.sale.timestamp >= todayStart }
        val todayRevenue = todaySales.sumOf { it.sale.totalAmount }
        val todayCount = todaySales.size
        val avgSale = if (todayCount > 0) todayRevenue / todayCount else 0.0
        val itemsSold = todaySales.sumOf { it.sale.itemsCount }

        val last7Days = repository.calculateLast7DaysMetrics(sales)
        val recent = sales.take(5)

        DashboardUiState(
            todayRevenue = todayRevenue,
            todayTransactionsCount = todayCount,
            todayAvgSale = avgSale,
            todayItemsSold = itemsSold,
            last7DaysMetrics = last7Days,
            recentSales = recent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // --- Reports Combined State ---
    val reportsState: StateFlow<ReportsUiState> = allSales.combine(_reportsDateFilter) { sales, filter ->
        val filteredSales = filterSalesForReports(sales, filter)
        val totalRevenue = filteredSales.sumOf { it.sale.totalAmount }
        val totalCount = filteredSales.size
        val avgSale = if (totalCount > 0) totalRevenue / totalCount else 0.0
        val totalItems = filteredSales.sumOf { it.sale.itemsCount }
        val topItems = repository.calculateTopSellingItems(filteredSales, limit = 5)
        val dailyMetrics = repository.calculateLast7DaysMetrics(filteredSales)
        val paymentBreakdown = repository.calculatePaymentMethodBreakdown(filteredSales)

        ReportsUiState(
            totalRevenue = totalRevenue,
            totalTransactions = totalCount,
            averageSaleValue = avgSale,
            totalItemsSold = totalItems,
            topSellingItems = topItems,
            dailyMetrics = dailyMetrics,
            paymentBreakdown = paymentBreakdown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    // Filtered Sales List
    val filteredSalesList: StateFlow<List<SaleWithItems>> = combine(allSales, _salesSearchQuery, _salesDateFilter) { sales, query, filter ->
        val queryLower = query.trim().lowercase()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        val yesterdayStart = todayStart - (24 * 60 * 60 * 1000L)
        val last7DaysStart = todayStart - (6 * 24 * 60 * 60 * 1000L)

        val calMonth = Calendar.getInstance()
        calMonth.set(Calendar.DAY_OF_MONTH, 1)
        calMonth.set(Calendar.HOUR_OF_DAY, 0)
        calMonth.set(Calendar.MINUTE, 0)
        calMonth.set(Calendar.SECOND, 0)
        calMonth.set(Calendar.MILLISECOND, 0)
        val monthStart = calMonth.timeInMillis

        sales.filter { saleWithItems ->
            val sale = saleWithItems.sale
            // Date filter
            val matchesDate = when (filter) {
                SalesDateFilter.TODAY -> sale.timestamp >= todayStart
                SalesDateFilter.YESTERDAY -> sale.timestamp in yesterdayStart until todayStart
                SalesDateFilter.LAST_7_DAYS -> sale.timestamp >= last7DaysStart
                SalesDateFilter.THIS_MONTH -> sale.timestamp >= monthStart
                SalesDateFilter.ALL -> true
            }

            // Search query filter (matches receipt number, customer name, notes, or item name)
            val matchesQuery = if (queryLower.isEmpty()) {
                true
            } else {
                sale.receiptNumber.lowercase().contains(queryLower) ||
                        (sale.customerName?.lowercase()?.contains(queryLower) == true) ||
                        (sale.notes?.lowercase()?.contains(queryLower) == true) ||
                        saleWithItems.items.any { it.productName.lowercase().contains(queryLower) }
            }

            matchesDate && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Check initial login state
        viewModelScope.launch {
            val prof = repository.getProfileSync()
            if (prof == null || !prof.isLoggedIn) {
                _currentScreen.value = Screen.LOGIN
            } else {
                _currentScreen.value = Screen.DASHBOARD
            }
        }
    }

    private fun filterSalesForReports(sales: List<SaleWithItems>, filter: ReportsDateFilter): List<SaleWithItems> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        return when (filter) {
            ReportsDateFilter.TODAY -> sales.filter { it.sale.timestamp >= todayStart }
            ReportsDateFilter.LAST_7_DAYS -> {
                val sevenDaysAgo = todayStart - (6 * 24 * 60 * 60 * 1000L)
                sales.filter { it.sale.timestamp >= sevenDaysAgo }
            }
            ReportsDateFilter.THIS_MONTH -> {
                val calMonth = Calendar.getInstance()
                calMonth.set(Calendar.DAY_OF_MONTH, 1)
                calMonth.set(Calendar.HOUR_OF_DAY, 0)
                calMonth.set(Calendar.MINUTE, 0)
                calMonth.set(Calendar.SECOND, 0)
                calMonth.set(Calendar.MILLISECOND, 0)
                sales.filter { it.sale.timestamp >= calMonth.timeInMillis }
            }
            ReportsDateFilter.ALL_TIME -> sales
        }
    }

    // --- Navigation Actions ---
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Cart & Record Sale Actions ---
    fun addToCart(product: Product) {
        val currentQty = _cart.value[product.id] ?: 0
        val updated = _cart.value.toMutableMap()
        updated[product.id] = currentQty + 1
        _cart.value = updated
    }

    fun decrementFromCart(product: Product) {
        val currentQty = _cart.value[product.id] ?: return
        val updated = _cart.value.toMutableMap()
        if (currentQty <= 1) {
            updated.remove(product.id)
        } else {
            updated[product.id] = currentQty - 1
        }
        _cart.value = updated
    }

    fun removeFromCart(productId: Long) {
        val updated = _cart.value.toMutableMap()
        updated.remove(productId)
        _cart.value = updated
    }

    fun clearCart() {
        _cart.value = emptyMap()
        _customerName.value = ""
        _paymentMethod.value = "CASH"
        _saleNotes.value = ""
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
    }

    fun setSaleNotes(notes: String) {
        _saleNotes.value = notes
    }

    fun setSelectedProductCategory(category: String) {
        _selectedProductCategory.value = category
    }

    fun completeSale() {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) {
            Toast.makeText(getApplication(), "Cart is empty. Select items first.", Toast.LENGTH_SHORT).show()
            return
        }

        val allProds = products.value.associateBy { it.id }
        val saleItems = currentCart.mapNotNull { (productId, qty) ->
            allProds[productId]?.let { prod -> prod to qty }
        }

        if (saleItems.isEmpty()) return

        val taxRate = profile.value?.taxRatePercent ?: 0.0

        viewModelScope.launch {
            val result = repository.recordSale(
                customerName = _customerName.value,
                paymentMethod = _paymentMethod.value,
                notes = _saleNotes.value,
                items = saleItems,
                taxPercent = taxRate
            )
            _completedSale.value = result
            _showReceiptDialog.value = true
            clearCart()
        }
    }

    fun dismissReceiptDialog() {
        _showReceiptDialog.value = false
        _completedSale.value = null
    }

    // --- Sales List Actions ---
    fun setSalesSearchQuery(query: String) {
        _salesSearchQuery.value = query
    }

    fun setSalesDateFilter(filter: SalesDateFilter) {
        _salesDateFilter.value = filter
    }

    fun showSaleDetail(sale: SaleWithItems) {
        _selectedSaleDetail.value = sale
    }

    fun dismissSaleDetail() {
        _selectedSaleDetail.value = null
    }

    fun deleteSale(saleId: Long) {
        viewModelScope.launch {
            repository.deleteSale(saleId)
            if (_selectedSaleDetail.value?.sale?.id == saleId) {
                _selectedSaleDetail.value = null
            }
            Toast.makeText(getApplication(), "Sale deleted", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Reports Actions ---
    fun setReportsDateFilter(filter: ReportsDateFilter) {
        _reportsDateFilter.value = filter
    }

    fun exportCsv() {
        viewModelScope.launch {
            try {
                val currentSales = allSales.value
                val currency = profile.value?.currencySymbol ?: "$"
                val csvFile = repository.exportSalesToCsv(currentSales, currency)

                val uri: Uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    csvFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "SaleSnap Sales Report")
                    putExtra(Intent.EXTRA_TEXT, "Exported sales transaction log from SaleSnap.")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(shareIntent, "Share Sales CSV Report").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                getApplication<Application>().startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Failed to export CSV: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Products Actions ---
    fun openAddProductDialog() {
        _editingProduct.value = null
        _showProductDialog.value = true
    }

    fun openEditProductDialog(product: Product) {
        _editingProduct.value = product
        _showProductDialog.value = true
    }

    fun dismissProductDialog() {
        _showProductDialog.value = false
        _editingProduct.value = null
    }

    fun saveProduct(name: String, price: Double, category: String, type: String, sku: String) {
        viewModelScope.launch {
            val current = _editingProduct.value
            if (current == null) {
                repository.addProduct(
                    Product(
                        name = name.trim(),
                        price = price,
                        category = category.trim().ifEmpty { "General" },
                        type = type,
                        sku = sku.trim()
                    )
                )
                Toast.makeText(getApplication(), "Product added", Toast.LENGTH_SHORT).show()
            } else {
                repository.updateProduct(
                    current.copy(
                        name = name.trim(),
                        price = price,
                        category = category.trim().ifEmpty { "General" },
                        type = type,
                        sku = sku.trim()
                    )
                )
                Toast.makeText(getApplication(), "Product updated", Toast.LENGTH_SHORT).show()
            }
            dismissProductDialog()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            Toast.makeText(getApplication(), "Product removed", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Settings & Auth ---
    fun updateBusinessProfile(
        businessName: String,
        ownerName: String,
        email: String,
        phone: String,
        address: String,
        currencySymbol: String,
        taxRatePercent: Double,
        receiptFooter: String
    ) {
        viewModelScope.launch {
            val existing = repository.getProfileSync() ?: BusinessProfile()
            repository.updateProfile(
                existing.copy(
                    businessName = businessName.trim(),
                    ownerName = ownerName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    currencySymbol = currencySymbol.trim(),
                    taxRatePercent = taxRatePercent,
                    receiptFooter = receiptFooter.trim()
                )
            )
            Toast.makeText(getApplication(), "Settings saved successfully", Toast.LENGTH_SHORT).show()
        }
    }

    fun loginOwner(ownerName: String, businessName: String, pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val existing = repository.getProfileSync() ?: BusinessProfile()
            repository.updateProfile(
                existing.copy(
                    ownerName = if (ownerName.isNotBlank()) ownerName.trim() else existing.ownerName,
                    businessName = if (businessName.isNotBlank()) businessName.trim() else existing.businessName,
                    pinCode = if (pin.isNotBlank()) pin else existing.pinCode,
                    isLoggedIn = true
                )
            )
            _currentScreen.value = Screen.DASHBOARD
            onSuccess()
        }
    }

    fun quickDemoLogin() {
        viewModelScope.launch {
            val existing = repository.getProfileSync() ?: BusinessProfile()
            repository.updateProfile(
                existing.copy(
                    businessName = "SaleSnap Artisan Cafe & Co.",
                    ownerName = "Alex Rivera",
                    isLoggedIn = true
                )
            )
            // If database has no sales yet, populate sample sales for an instant rich experience!
            if (allSales.value.isEmpty()) {
                repository.seedSampleSales()
            }
            _currentScreen.value = Screen.DASHBOARD
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.setLoggedIn(false)
            _currentScreen.value = Screen.LOGIN
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleSales()
            Toast.makeText(getApplication(), "Sample sales added!", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllSales()
            clearCart()
            Toast.makeText(getApplication(), "Sales data cleared", Toast.LENGTH_SHORT).show()
        }
    }
}
