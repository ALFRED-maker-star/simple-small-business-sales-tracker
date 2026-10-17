package com.example

import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleWithItems
import com.example.data.repository.DaySalesMetric
import com.example.data.repository.PaymentMethodBreakdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleSnapUnitTest {

    @Test
    fun testSaleItemTotalCalculation() {
        val item = SaleItem(
            saleId = 1L,
            productName = "Latte",
            unitPrice = 5.25,
            quantity = 3
        )
        assertEquals(15.75, item.itemTotal, 0.001)
    }

    @Test
    fun testPaymentMethodBreakdownAggregation() {
        val sale1 = SaleWithItems(
            sale = Sale(id = 1L, paymentMethod = "CASH", totalAmount = 50.0),
            items = emptyList()
        )
        val sale2 = SaleWithItems(
            sale = Sale(id = 2L, paymentMethod = "CARD", totalAmount = 150.0),
            items = emptyList()
        )
        val list = listOf(sale1, sale2)
        val totalRevenue = list.sumOf { it.sale.totalAmount }
        assertEquals(200.0, totalRevenue, 0.001)

        val cashSales = list.filter { it.sale.paymentMethod == "CASH" }
        assertEquals(1, cashSales.size)
        assertEquals(50.0, cashSales.sumOf { it.sale.totalAmount }, 0.001)
    }
}
