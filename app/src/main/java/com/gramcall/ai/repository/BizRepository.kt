package com.gramcall.ai.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.gramcall.ai.biz.*
import com.gramcall.ai.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ReportData(
    val calls: List<CallHistoryEntity>,
    val orders: List<OrderEntity>,
    val items: List<OrderItemEntity>
)

class BizRepository(private val db: AppDatabase) {
    private val productDao = db.productDao()
    private val orderDao = db.orderDao()
    private val settingDao = db.settingDao()
    private val customerDao = db.customerDao()
    private val callDao = db.callHistoryDao()

    // ---------- products ----------
    fun products(q: String, category: String?): Flow<List<ProductEntity>> = productDao.observe(q, category)
    fun activeProducts(): Flow<List<ProductEntity>> = productDao.observeActive()
    suspend fun activeProductsNow(): List<ProductEntity> = productDao.getActive()
    fun lowStockCount(): Flow<Int> = productDao.observeLowStockCount()

    suspend fun saveProduct(p: ProductEntity): OpResult {
        val name = p.name.trim()
        if (name.isEmpty()) return OpResult.fail("प्रोडक्ट का नाम ज़रूरी है")
        if (p.price < 0) return OpResult.fail("कीमत सही नहीं है")
        val clean = p.copy(
            name = name, sku = p.sku.trim(), description = p.description.trim(),
            aliases = p.aliases.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")
        )
        return try {
            if (p.id == 0L) productDao.insert(clean) else productDao.update(clean)
            OpResult.success("प्रोडक्ट सेव हो गया")
        } catch (e: SQLiteConstraintException) {
            OpResult.fail("इस नाम का प्रोडक्ट पहले से है")
        }
    }

    suspend fun deleteProduct(p: ProductEntity): OpResult {
        productDao.delete(p)
        return OpResult.success("प्रोडक्ट हटा दिया गया")
    }

    suspend fun setStock(id: Long, newStock: Double) {
        val p = productDao.getById(id) ?: return
        productDao.update(p.copy(stock = newStock))
    }

    // ---------- orders ----------
    fun orders(
        from: Long, to: Long, villageId: Long?, cityId: Long?, status: String?, customerId: Long?, q: String
    ): Flow<List<OrderEntity>> = orderDao.observe(from, to, villageId, cityId, status, customerId, q)

    fun order(id: Long): Flow<OrderEntity?> = orderDao.observeById(id)
    fun orderItems(id: Long): Flow<List<OrderItemEntity>> = orderDao.observeItems(id)
    suspend fun orderNow(id: Long): OrderEntity? = orderDao.getById(id)
    suspend fun orderItemsNow(id: Long): List<OrderItemEntity> = orderDao.getItems(id)
    fun orderCountForCampaign(id: Long): Flow<Int> = orderDao.observeCountForCampaign(id)
    fun ordersSince(since: Long): Flow<Int> = orderDao.observeCountSince(since)
    fun salesSince(since: Long): Flow<Double> = orderDao.observeSalesSince(since)
    fun openOrders(): Flow<Int> = orderDao.observeOpenCount()
    fun deliveredOrders(): Flow<Int> = orderDao.observeDeliveredCount()

    /** Customer ka naam/gaon/shehar ka snapshot order mein rakhne ke liye. */
    suspend fun newOrderFor(customerId: Long, campaignId: Long?, source: String): OrderEntity? {
        val cw = customerDao.getWithPlace(customerId) ?: return null
        val c = cw.customer
        return OrderEntity(
            customerId = c.id, customerName = c.name, customerMobile = c.mobile,
            villageId = c.villageId, cityId = c.cityId,
            villageName = cw.villageName.orEmpty(), cityName = cw.cityName.orEmpty(),
            subtotal = 0.0, total = 0.0, deliveryAddress = c.address,
            campaignId = campaignId, status = OrderStatus.CONFIRMED, source = source
        )
    }

    private suspend fun applyStock(items: List<OrderItemEntity>, sign: Int) {
        for (it in items) {
            val pid = it.productId ?: continue
            productDao.addStock(pid, sign * it.quantity)
        }
    }

    /**
     * Ek transaction mein order + items save. Naya order: stock ghatta hai.
     * Edit: purana stock lauta kar naya laga diya jaata hai. Cancelled order stock nahi ghatata.
     */
    suspend fun saveOrder(order: OrderEntity, items: List<OrderItemEntity>): Pair<Long?, String> {
        if (items.isEmpty()) return null to "कम से कम एक सामान जोड़ें"
        if (items.any { it.quantity <= 0 }) return null to "मात्रा 0 से ज़्यादा होनी चाहिए"
        val totals = OrderCalc.totals(items.map { it.quantity to it.unitPrice }, order.discount)
        val fixed = order.copy(subtotal = totals.subtotal, discount = totals.discount, total = totals.total)
        val id = db.withTransaction {
            val existing = if (order.id != 0L) orderDao.getById(order.id) else null
            val oid: Long
            if (existing != null) {
                if (existing.status != OrderStatus.CANCELLED) applyStock(orderDao.getItems(existing.id), +1)
                orderDao.deleteItems(existing.id)
                orderDao.update(fixed.copy(orderAt = existing.orderAt))
                oid = existing.id
            } else {
                oid = orderDao.insert(fixed.copy(id = 0))
            }
            val rows = items.map {
                it.copy(id = 0, orderId = oid, lineTotal = OrderCalc.lineTotal(it.quantity, it.unitPrice))
            }
            orderDao.insertItems(rows)
            if (fixed.status != OrderStatus.CANCELLED) applyStock(rows, -1)
            oid
        }
        return id to "ऑर्डर #$id सेव हो गया"
    }

    suspend fun setOrderStatus(orderId: Long, status: String): OpResult {
        db.withTransaction {
            val o = orderDao.getById(orderId) ?: return@withTransaction
            if (o.status == status) return@withTransaction
            val items = orderDao.getItems(orderId)
            if (status == OrderStatus.CANCELLED) applyStock(items, +1)
            else if (o.status == OrderStatus.CANCELLED) applyStock(items, -1)
            orderDao.update(o.copy(status = status))
        }
        return OpResult.success("स्थिति बदल गई")
    }

    // ---------- reports ----------
    suspend fun reportData(
        from: Long, to: Long, villageId: Long?, cityId: Long?, customerId: Long?, status: String?
    ): ReportData {
        val calls = callDao.reportCalls(from, to, villageId, cityId, customerId)
        val orders = orderDao.list(from, to, villageId, cityId, status, customerId)
        val items = orders.map { it.id }.chunked(500).flatMap { orderDao.getItemsForOrders(it) }
        return ReportData(calls, orders, items)
    }

    // ---------- settings ----------
    fun settings(): Flow<Map<String, String>> = settingDao.observeAll().map { l -> l.associate { it.id to it.value } }
    suspend fun setting(id: String): String? = settingDao.get(id)
    suspend fun putSettings(values: Map<String, String>) =
        settingDao.putAll(values.map { SettingEntity(it.key, it.value) })
}

object Keys {
    const val ONBOARDED = "onboarded"
    const val SHOP_NAME = "shop_name"
    const val SHOP_ADDRESS = "shop_address"
    const val SHOP_MOBILE = "shop_mobile"
    const val AGENT_NAME = "agent_name"
    const val LANGUAGE = "language"
    const val VOICE_TYPE = "voice_type"
    const val VOICE_SPEED = "voice_speed"
    const val GREETING = "greeting"
    const val CONFIRM_SCRIPT = "confirm_script"
    const val THANKS_SCRIPT = "thanks_script"
    const val DELIVERY_INFO = "delivery_info"
    const val PRODUCT_INFO = "product_info"
    const val PRICE_INFO = "price_info"
}
