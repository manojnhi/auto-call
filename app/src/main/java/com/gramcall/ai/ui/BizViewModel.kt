package com.gramcall.ai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gramcall.ai.GramCallApp
import com.gramcall.ai.biz.*
import com.gramcall.ai.database.*
import com.gramcall.ai.repository.Keys
import com.gramcall.ai.repository.ReportData
import com.gramcall.ai.utils.CsvUtils
import com.gramcall.ai.utils.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class BizViewModel(app: Application) : AndroidViewModel(app) {
    private val biz = (app as GramCallApp).biz
    private val repo = (app as GramCallApp).repository
    private val started = SharingStarted.WhileSubscribed(5000)

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    // ---- dashboard ----
    private val dayStart: Long = DateRanges.resolve(DatePreset.TODAY, System.currentTimeMillis()).first
    val ordersToday: StateFlow<Int?> = biz.ordersSince(dayStart).stateIn(viewModelScope, started, null)
    val salesToday: StateFlow<Double?> = biz.salesSince(dayStart).stateIn(viewModelScope, started, null)
    val openOrders: StateFlow<Int?> = biz.openOrders().stateIn(viewModelScope, started, null)
    val deliveredOrders: StateFlow<Int?> = biz.deliveredOrders().stateIn(viewModelScope, started, null)

    // ---- settings ----
    val settings: StateFlow<Map<String, String>> = biz.settings().stateIn(viewModelScope, started, emptyMap())
    fun saveSettings(values: Map<String, String>, done: () -> Unit = {}) {
        viewModelScope.launch {
            biz.putSettings(values)
            messages.tryEmit("सेटिंग्स सेव हो गईं")
            done()
        }
    }

    // ---- products ----
    val productQuery = MutableStateFlow("")
    val productCategory = MutableStateFlow<String?>(null)
    val products: StateFlow<List<ProductEntity>?> =
        combine(productQuery, productCategory) { q, c -> q.trim() to c }
            .flatMapLatest { (q, c) -> biz.products(q, c) }.stateIn(viewModelScope, started, null)
    val activeProducts: StateFlow<List<ProductEntity>> =
        biz.activeProducts().stateIn(viewModelScope, started, emptyList())
    val lowStockCount: StateFlow<Int> = biz.lowStockCount().stateIn(viewModelScope, started, 0)

    fun saveProduct(p: ProductEntity, done: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val r = biz.saveProduct(p)
            messages.tryEmit(r.message)
            done(r.ok)
        }
    }

    fun deleteProduct(p: ProductEntity) {
        viewModelScope.launch { messages.tryEmit(biz.deleteProduct(p).message) }
    }

    fun setStock(id: Long, stock: Double) { viewModelScope.launch { biz.setStock(id, stock) } }

    // ---- orders ----
    val orderQuery = MutableStateFlow("")
    val orderStatus = MutableStateFlow<String?>(null)
    val orderPreset = MutableStateFlow<DatePreset?>(DatePreset.LAST30)
    val orders: StateFlow<List<OrderEntity>?> =
        combine(orderQuery, orderStatus, orderPreset) { q, s, p -> Triple(q.trim(), s, p) }
            .flatMapLatest { (q, s, p) ->
                val range = if (p == null) 0L to Long.MAX_VALUE else DateRanges.resolve(p, System.currentTimeMillis())
                biz.orders(range.first, range.second, null, null, s, null, q)
            }.stateIn(viewModelScope, started, null)

    fun order(id: Long): Flow<OrderEntity?> = biz.order(id)
    fun orderItems(id: Long): Flow<List<OrderItemEntity>> = biz.orderItems(id)
    suspend fun loadOrder(id: Long): Pair<OrderEntity?, List<OrderItemEntity>> = biz.orderNow(id) to biz.orderItemsNow(id)
    suspend fun newOrderFor(customerId: Long, campaignId: Long?, source: String) =
        biz.newOrderFor(customerId, campaignId, source)

    fun pickCustomer(customerId: Long, campaignId: Long? = null, source: String = "MANUAL", done: (OrderEntity?) -> Unit) {
        viewModelScope.launch { done(biz.newOrderFor(customerId, campaignId, source)) }
    }

    fun saveOrder(order: OrderEntity, items: List<OrderItemEntity>, done: (Long?) -> Unit) {
        viewModelScope.launch {
            val (id, msg) = biz.saveOrder(order, items)
            messages.tryEmit(msg)
            done(id)
        }
    }

    fun setOrderStatus(id: Long, status: String) {
        viewModelScope.launch { messages.tryEmit(biz.setOrderStatus(id, status).message) }
    }

    fun customerOrders(customerId: Long): Flow<List<OrderEntity>> = ordersForCustomer(customerId)

    fun ordersForCustomer(customerId: Long): Flow<List<OrderEntity>> =
        biz.orders(0L, Long.MAX_VALUE, null, null, null, customerId, "")

    // ---- customer picker ----
    val pickerQuery = MutableStateFlow("")
    val pickerCustomers: StateFlow<List<CustomerWithPlace>> =
        pickerQuery.flatMapLatest { repo.customers(it.trim(), null, null) }
            .stateIn(viewModelScope, started, emptyList())

    // ---- invoice ----
    suspend fun invoiceText(orderId: Long): String? {
        val (o, items) = loadOrder(orderId)
        o ?: return null
        val s = settings.value
        return Invoice.text(s[Keys.SHOP_NAME].orEmpty(), s[Keys.SHOP_ADDRESS].orEmpty(), s[Keys.SHOP_MOBILE].orEmpty(), o, items)
    }

    // ---- reports ----
    suspend fun report(
        preset: DatePreset, customFrom: Long?, customTo: Long?,
        villageId: Long?, cityId: Long?, status: String?
    ): ReportData {
        val (a, b) = DateRanges.resolve(preset, System.currentTimeMillis(), customFrom, customTo)
        return biz.reportData(a, b, villageId, cityId, null, status)
    }

    // ---- order assistant ----
    private val provider: OrderUnderstandingProvider = RuleBasedProvider()
    suspend fun parseOrder(text: String): Pair<List<ParsedLine>, List<ProductEntity>> {
        val products = biz.activeProductsNow()
        val catalog = products.map { p ->
            CatalogItem(p.id, p.name, p.unit, p.aliases.split(',').map { it.trim() }.filter { it.isNotEmpty() })
        }
        return provider.parse(text, catalog) to products
    }

    // ---- files ----
    fun writeText(uri: Uri, text: String, okMessage: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(("\uFEFF" + text).toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("फ़ाइल नहीं खुल सकी")
                }
                messages.tryEmit(okMessage)
            } catch (e: Exception) {
                messages.tryEmit("फ़ाइल सेव नहीं हुई: ${e.message}")
            }
        }
    }

    fun exportOrders(uri: Uri, list: List<OrderEntity>) {
        val rows = mutableListOf(
            listOf("order_id", "date", "customer", "mobile", "village", "city", "subtotal", "discount", "total", "status", "address")
        )
        list.forEach {
            rows.add(listOf(
                it.id.toString(), Fmt.dateTime(it.orderAt), it.customerName, it.customerMobile, it.villageName,
                it.cityName, it.subtotal.toString(), it.discount.toString(), it.total.toString(),
                OrderStatus.label(it.status), it.deliveryAddress
            ))
        }
        writeText(uri, CsvUtils.toCsv(rows), "ऑर्डर एक्सपोर्ट हो गए")
    }
}
