package com.example.data.repository

import android.content.Context
import androidx.core.content.FileProvider
import com.example.data.dao.BusinessProfileDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.db.AppDatabase
import com.example.data.entity.BusinessProfile
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DaySalesMetric(
    val dayLabel: String,
    val dayTimestamp: Long,
    val totalRevenue: Double,
    val transactionCount: Int,
    val isToday: Boolean
)

data class TopProductMetric(
    val productName: String,
    val totalQuantitySold: Int,
    val totalRevenue: Double,
    val percentageOfSales: Float
)

data class PaymentMethodBreakdown(
    val method: String,
    val count: Int,
    val totalAmount: Double,
    val percentage: Float
)

class SaleSnapRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val profileDao: BusinessProfileDao,
    private val context: Context
) {
    // --- Products ---
    val activeProducts: Flow<List<Product>> = productDao.getActiveProducts().flowOn(Dispatchers.IO)
    val allProducts: Flow<List<Product>> = productDao.getAllProducts().flowOn(Dispatchers.IO)

    suspend fun addProduct(product: Product): Long = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    // --- Sales ---
    val allSalesWithItems: Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItems().flowOn(Dispatchers.IO)

    suspend fun getSaleById(id: Long): SaleWithItems? = withContext(Dispatchers.IO) {
        saleDao.getSaleWithItemsById(id)
    }

    suspend fun recordSale(
        customerName: String?,
        paymentMethod: String,
        notes: String?,
        items: List<Pair<Product, Int>>,
        taxPercent: Double = 0.0
    ): SaleWithItems = withContext(Dispatchers.IO) {
        require(items.isNotEmpty()) { "Sale must contain at least one item." }

        val subtotal = items.sumOf { (product, qty) -> product.price * qty }
        val taxAmount = subtotal * (taxPercent / 100.0)
        val totalAmount = subtotal + taxAmount
        val totalQuantity = items.sumOf { it.second }

        val receiptNum = "SS-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date())

        val sale = Sale(
            timestamp = System.currentTimeMillis(),
            customerName = customerName?.trim()?.ifEmpty { null },
            paymentMethod = paymentMethod,
            subtotal = subtotal,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            receiptNumber = receiptNum,
            notes = notes?.trim()?.ifEmpty { null },
            itemsCount = totalQuantity
        )

        val saleId = saleDao.insertSale(sale)

        val saleItems = items.map { (product, qty) ->
            SaleItem(
                saleId = saleId,
                productId = product.id,
                productName = product.name,
                unitPrice = product.price,
                quantity = qty,
                itemTotal = product.price * qty
            )
        }
        saleDao.insertSaleItems(saleItems)

        // Increment product sales count
        items.forEach { (product, qty) ->
            productDao.incrementSalesCount(product.id, qty)
        }

        SaleWithItems(
            sale = sale.copy(id = saleId),
            items = saleItems
        )
    }

    suspend fun deleteSale(saleId: Long) = withContext(Dispatchers.IO) {
        saleDao.deleteSaleById(saleId)
    }

    suspend fun clearAllSales() = withContext(Dispatchers.IO) {
        saleDao.deleteAllSales()
    }

    // --- Profile & Business Settings ---
    val profile: Flow<BusinessProfile?> = profileDao.getProfile().flowOn(Dispatchers.IO)

    suspend fun getProfileSync(): BusinessProfile? = withContext(Dispatchers.IO) {
        profileDao.getProfileSync()
    }

    suspend fun updateProfile(profile: BusinessProfile) = withContext(Dispatchers.IO) {
        profileDao.insertOrUpdate(profile)
    }

    suspend fun setLoggedIn(loggedIn: Boolean) = withContext(Dispatchers.IO) {
        profileDao.setLoginState(loggedIn)
    }

    // --- Metrics & Charts ---
    fun calculateLast7DaysMetrics(sales: List<SaleWithItems>): List<DaySalesMetric> {
        val result = mutableListOf<DaySalesMetric>()
        val calendar = Calendar.getInstance()

        // Normalize calendar to end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        val dayFormat = SimpleDateFormat("EEE", Locale.US)

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            val startOfDay = dayCal.timeInMillis

            dayCal.set(Calendar.HOUR_OF_DAY, 23)
            dayCal.set(Calendar.MINUTE, 59)
            dayCal.set(Calendar.SECOND, 59)
            dayCal.set(Calendar.MILLISECOND, 999)
            val endOfDay = dayCal.timeInMillis

            val matchingSales = sales.filter { it.sale.timestamp in startOfDay..endOfDay }
            val dayRevenue = matchingSales.sumOf { it.sale.totalAmount }
            val dayCount = matchingSales.size
            val label = if (i == 0) "Today" else dayFormat.format(dayCal.time)

            result.add(
                DaySalesMetric(
                    dayLabel = label,
                    dayTimestamp = startOfDay,
                    totalRevenue = dayRevenue,
                    transactionCount = dayCount,
                    isToday = (i == 0)
                )
            )
        }

        return result
    }

    fun calculateTopSellingItems(sales: List<SaleWithItems>, limit: Int = 5): List<TopProductMetric> {
        val allItems = sales.flatMap { it.items }
        val totalRevenue = sales.sumOf { it.sale.totalAmount }

        val grouped = allItems.groupBy { it.productName }
        return grouped.map { (name, itemsList) ->
            val totalQty = itemsList.sumOf { it.quantity }
            val revenue = itemsList.sumOf { it.itemTotal }
            val percentage = if (totalRevenue > 0) ((revenue / totalRevenue) * 100).toFloat() else 0f
            TopProductMetric(
                productName = name,
                totalQuantitySold = totalQty,
                totalRevenue = revenue,
                percentageOfSales = percentage
            )
        }
            .sortedByDescending { it.totalRevenue }
            .take(limit)
    }

    fun calculatePaymentMethodBreakdown(sales: List<SaleWithItems>): List<PaymentMethodBreakdown> {
        if (sales.isEmpty()) return emptyList()
        val totalRevenue = sales.sumOf { it.sale.totalAmount }

        return sales.groupBy { it.sale.paymentMethod }
            .map { (method, list) ->
                val amount = list.sumOf { it.sale.totalAmount }
                val pct = if (totalRevenue > 0) ((amount / totalRevenue) * 100).toFloat() else 0f
                PaymentMethodBreakdown(
                    method = method,
                    count = list.size,
                    totalAmount = amount,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    // --- CSV Export ---
    suspend fun exportSalesToCsv(sales: List<SaleWithItems>, currencySymbol: String): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val csvFile = File(exportDir, "SaleSnap_Report_$timeStamp.csv")

        FileWriter(csvFile).use { writer ->
            writer.append("Receipt Number,Date,Time,Customer Name,Items Count,Items Summary,Payment Method,Subtotal,Tax,Total ($currencySymbol),Notes\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

            for (saleWithItems in sales) {
                val sale = saleWithItems.sale
                val dateStr = dateFormat.format(Date(sale.timestamp))
                val timeStr = timeFormat.format(Date(sale.timestamp))
                val customer = escapeCsv(sale.customerName ?: "Walk-in")
                val itemsSummary = escapeCsv(saleWithItems.items.joinToString("; ") { "${it.quantity}x ${it.productName} (@$currencySymbol${String.format(Locale.US, "%.2f", it.unitPrice)})" })
                val notes = escapeCsv(sale.notes ?: "")

                writer.append("${sale.receiptNumber},$dateStr,$timeStr,$customer,${sale.itemsCount},$itemsSummary,${sale.paymentMethod},${String.format(Locale.US, "%.2f", sale.subtotal)},${String.format(Locale.US, "%.2f", sale.taxAmount)},${String.format(Locale.US, "%.2f", sale.totalAmount)},$notes\n")
            }
            writer.flush()
        }

        csvFile
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    // --- Demo Data Seeder for testing / immediate rich experience ---
    suspend fun seedSampleSales() = withContext(Dispatchers.IO) {
        val products = productDao.countProducts()
        if (products == 0) {
            AppDatabase.populateInitialData(AppDatabase.getDatabase(context))
        }

        // Generate past 7 days of realistic sales
        val allProds = productDao.getAllProducts()
        // We will read sync or take initial
        val pList = listOf(
            Product(name = "Espresso", price = 3.50, category = "Beverage", type = "PRODUCT"),
            Product(name = "Vanilla Latte", price = 5.25, category = "Beverage", type = "PRODUCT"),
            Product(name = "Iced Cold Brew", price = 4.75, category = "Beverage", type = "PRODUCT"),
            Product(name = "Butter Croissant", price = 4.00, category = "Bakery", type = "PRODUCT"),
            Product(name = "Avocado Toast", price = 8.50, category = "Food", type = "PRODUCT"),
            Product(name = "Retail Coffee Beans (12oz)", price = 16.00, category = "Retail", type = "PRODUCT")
        )

        val customers = listOf("Sarah Jenkins", "Michael Chang", "Emily Watson", "David Miller", "Jessica Taylor", null, null, "Carlos R.")
        val paymentMethods = listOf("CARD", "CARD", "CASH", "MOBILE", "TRANSFER")

        val cal = Calendar.getInstance()
        val random = java.util.Random()

        for (daysAgo in 6 downTo 0) {
            val transactionsCount = if (daysAgo == 0) 6 + random.nextInt(5) else 4 + random.nextInt(8)
            for (t in 0 until transactionsCount) {
                cal.timeInMillis = System.currentTimeMillis()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                cal.set(Calendar.HOUR_OF_DAY, 8 + random.nextInt(10))
                cal.set(Calendar.MINUTE, random.nextInt(60))

                val chosenCustomer = customers[random.nextInt(customers.size)]
                val chosenPayment = paymentMethods[random.nextInt(paymentMethods.size)]

                val itemCount = 1 + random.nextInt(3)
                val chosenItems = mutableListOf<Pair<Product, Int>>()
                for (i in 0 until itemCount) {
                    val prod = pList[random.nextInt(pList.size)]
                    chosenItems.add(prod to (1 + random.nextInt(2)))
                }

                val subtotal = chosenItems.sumOf { (p, q) -> p.price * q }
                val receiptNumber = "SS-" + SimpleDateFormat("yyMMdd-", Locale.US).format(cal.time) + (1000 + random.nextInt(9000))

                val sale = Sale(
                    timestamp = cal.timeInMillis,
                    customerName = chosenCustomer,
                    paymentMethod = chosenPayment,
                    subtotal = subtotal,
                    taxAmount = 0.0,
                    totalAmount = subtotal,
                    receiptNumber = receiptNumber,
                    notes = if (random.nextBoolean()) "Quick regular customer" else null,
                    itemsCount = chosenItems.sumOf { it.second }
                )
                val saleId = saleDao.insertSale(sale)

                val saleItems = chosenItems.map { (p, q) ->
                    SaleItem(
                        saleId = saleId,
                        productName = p.name,
                        unitPrice = p.price,
                        quantity = q,
                        itemTotal = p.price * q
                    )
                }
                saleDao.insertSaleItems(saleItems)
            }
        }
    }
}
