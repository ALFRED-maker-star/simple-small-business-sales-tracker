package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BusinessProfileDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.entity.BusinessProfile
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Product::class, Sale::class, SaleItem::class, BusinessProfile::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun businessProfileDao(): BusinessProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salesnap_database.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val productDao = database.productDao()
            val profileDao = database.businessProfileDao()
            val saleDao = database.saleDao()

            // Default business profile
            if (profileDao.getProfileSync() == null) {
                profileDao.insertOrUpdate(
                    BusinessProfile(
                        id = 1,
                        businessName = "SaleSnap Artisan Cafe & Co.",
                        ownerName = "Alex Rivera",
                        email = "alex@salesnap.biz",
                        phone = "+1 (555) 392-8172",
                        address = "420 Market Street, Suite 10",
                        currencySymbol = "$",
                        taxRatePercent = 0.0,
                        receiptFooter = "Thank you for shopping local with SaleSnap!",
                        pinCode = "1234",
                        isLoggedIn = false
                    )
                )
            }

            // Default quick products & services
            if (productDao.countProducts() == 0) {
                val starterItems = listOf(
                    Product(name = "Espresso", price = 3.50, category = "Beverage", type = "PRODUCT", sku = "BEV-001"),
                    Product(name = "Vanilla Latte", price = 5.25, category = "Beverage", type = "PRODUCT", sku = "BEV-002"),
                    Product(name = "Iced Cold Brew", price = 4.75, category = "Beverage", type = "PRODUCT", sku = "BEV-003"),
                    Product(name = "Chai Tea Latte", price = 4.95, category = "Beverage", type = "PRODUCT", sku = "BEV-004"),
                    Product(name = "Butter Croissant", price = 4.00, category = "Bakery", type = "PRODUCT", sku = "BAK-001"),
                    Product(name = "Blueberry Muffin", price = 3.85, category = "Bakery", type = "PRODUCT", sku = "BAK-002"),
                    Product(name = "Avocado Toast", price = 8.50, category = "Food", type = "PRODUCT", sku = "FOOD-001"),
                    Product(name = "Bagel & Cream Cheese", price = 4.50, category = "Food", type = "PRODUCT", sku = "FOOD-002"),
                    Product(name = "Retail Coffee Beans (12oz)", price = 16.00, category = "Retail", type = "PRODUCT", sku = "RET-001"),
                    Product(name = "Barista Masterclass (1 hr)", price = 45.00, category = "Service", type = "SERVICE", sku = "SRV-001")
                )
                productDao.insertProducts(starterItems)
            }
        }
    }
}
