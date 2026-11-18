package com.gramcall.ai

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gramcall.ai.biz.OrderStatus
import com.gramcall.ai.biz.Units
import com.gramcall.ai.database.*
import com.gramcall.ai.repository.BackupManager
import com.gramcall.ai.repository.BizRepository
import com.gramcall.ai.repository.GramRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: GramRepository
    private lateinit var biz: BizRepository
    private var customerId = 0L
    private var atta = ProductEntity(name = "आटा", category = "आटा", unit = Units.KG, price = 40.0, stock = 100.0)

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = GramRepository(db); biz = BizRepository(db)
        repo.saveVillage(VillageEntity(name = "इटारौरा"))
        val vid = repo.villages("").first().first().village.id
        repo.saveCustomer(CustomerEntity(name = "राम", mobile = "9876543210", villageId = vid, callConsent = true))
        customerId = repo.customers("", null, null).first().first().customer.id
        biz.saveProduct(atta)
        atta = biz.activeProductsNow().first()
        Unit
    }

    @After fun tearDown() = db.close()

    private suspend fun item(qty: Double) =
        OrderItemEntity(orderId = 0, productId = atta.id, productName = atta.name, unit = atta.unit, quantity = qty, unitPrice = atta.price, lineTotal = 0.0)

    private suspend fun stock() = biz.activeProductsNow().first().stock

    @Test fun duplicateProductNameRejected() = runBlocking {
        assertFalse(biz.saveProduct(ProductEntity(name = "आटा", category = "आटा", unit = Units.KG, price = 1.0)).ok)
    }

    @Test fun orderTotalsComputedAndStockReduced() = runBlocking {
        val o = biz.newOrderFor(customerId, null, "MANUAL")!!.copy(discount = 20.0)
        val (id, _) = biz.saveOrder(o, listOf(item(5.0)))
        val saved = biz.orderNow(id!!)!!
        assertEquals(200.0, saved.subtotal, 0.001); assertEquals(180.0, saved.total, 0.001)
        assertEquals(95.0, stock(), 0.001)
        assertEquals("राम", saved.customerName); assertEquals("इटारौरा", saved.villageName)
    }

    @Test fun cancelRestoresStockAndReopenReducesAgain() = runBlocking {
        val id = biz.saveOrder(biz.newOrderFor(customerId, null, "MANUAL")!!, listOf(item(5.0))).first!!
        biz.setOrderStatus(id, OrderStatus.CANCELLED); assertEquals(100.0, stock(), 0.001)
        biz.setOrderStatus(id, OrderStatus.CONFIRMED); assertEquals(95.0, stock(), 0.001)
    }

    @Test fun editOrderAdjustsStockAndItems() = runBlocking {
        val base = biz.newOrderFor(customerId, null, "MANUAL")!!
        val id = biz.saveOrder(base, listOf(item(5.0))).first!!
        biz.saveOrder(biz.orderNow(id)!!, listOf(item(2.0)))
        assertEquals(98.0, stock(), 0.001)
        assertEquals(1, biz.orderItemsNow(id).size); assertEquals(80.0, biz.orderNow(id)!!.total, 0.001)
    }

    @Test fun emptyOrderAndZeroQuantityRejected() = runBlocking {
        val base = biz.newOrderFor(customerId, null, "MANUAL")!!
        assertNull(biz.saveOrder(base, emptyList()).first)
        assertNull(biz.saveOrder(base, listOf(item(0.0))).first)
    }

    @Test fun orderSurvivesCustomerAndProductDeletion() = runBlocking {
        val id = biz.saveOrder(biz.newOrderFor(customerId, null, "MANUAL")!!, listOf(item(1.0))).first!!
        biz.deleteProduct(atta)
        repo.deleteCustomer(repo.customers("", null, null).first().first().customer)
        val o = biz.orderNow(id)!!
        assertEquals("राम", o.customerName); assertNull(o.customerId)
        assertEquals("आटा", biz.orderItemsNow(id).first().productName)
    }

    @Test fun backupRoundTripRestoresEverything() = runBlocking {
        biz.saveOrder(biz.newOrderFor(customerId, null, "MANUAL")!!, listOf(item(3.0)))
        val json = BackupManager.export(db)
        db.orderDao().deleteAllItems(); db.orderDao().deleteAll()
        repo.deleteCustomer(repo.customers("", null, null).first().first().customer)
        BackupManager.restore(db, json)
        assertEquals(1, repo.customers("", null, null).first().size)
        assertEquals(1, biz.orders(0, Long.MAX_VALUE, null, null, null, null, "").first().size)
        assertEquals(1, biz.activeProductsNow().size)
    }

    @Test fun restoreRejectsForeignFile() = runBlocking {
        try { BackupManager.restore(db, "{\"format\":\"other\"}"); fail("should throw") }
        catch (e: IllegalArgumentException) { assertEquals(1, biz.activeProductsNow().size) }
        Unit
    }

    @Test fun optOutIsLoggedAndBlocksEligibility() = runBlocking {
        val c = repo.customers("", null, null).first().first().customer
        repo.optOutCustomer(c)
        assertFalse(repo.customers("", null, null).first().first().customer.callConsent)
        assertTrue(db.consentLogDao().getAll().any { it.source == "OPT_OUT" })
    }
}
