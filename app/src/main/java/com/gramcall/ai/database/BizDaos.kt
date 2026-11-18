package com.gramcall.ai.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert suspend fun insert(p: ProductEntity): Long
    @Update suspend fun update(p: ProductEntity)
    @Delete suspend fun delete(p: ProductEntity)

    @Query(
        "SELECT * FROM products WHERE (name LIKE '%' || :q || '%' OR sku LIKE '%' || :q || '%') " +
            "AND (:category IS NULL OR category = :category) ORDER BY name COLLATE NOCASE"
    )
    fun observe(q: String, category: String?): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name COLLATE NOCASE")
    suspend fun getActive(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id") suspend fun getById(id: Long): ProductEntity?
    @Query("SELECT COUNT(*) FROM products WHERE active = 1 AND stock <= minStock")
    fun observeLowStockCount(): Flow<Int>
    @Query("UPDATE products SET stock = stock + :delta WHERE id = :id")
    suspend fun addStock(id: Long, delta: Double)

    @Query("SELECT * FROM products") suspend fun getAll(): List<ProductEntity>
    @Query("DELETE FROM products") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<ProductEntity>)
}

@Dao
interface OrderDao {
    @Insert suspend fun insert(o: OrderEntity): Long
    @Update suspend fun update(o: OrderEntity)
    @Insert suspend fun insertItems(items: List<OrderItemEntity>)
    @Query("DELETE FROM order_items WHERE orderId = :orderId") suspend fun deleteItems(orderId: Long)
    @Query("SELECT * FROM orders WHERE id = :id") suspend fun getById(id: Long): OrderEntity?
    @Query("SELECT * FROM orders WHERE id = :id") fun observeById(id: Long): Flow<OrderEntity?>
    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY id")
    suspend fun getItems(orderId: Long): List<OrderItemEntity>
    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY id")
    fun observeItems(orderId: Long): Flow<List<OrderItemEntity>>
    @Query("SELECT * FROM order_items WHERE orderId IN (:ids)")
    suspend fun getItemsForOrders(ids: List<Long>): List<OrderItemEntity>

    @Query(
        "SELECT * FROM orders WHERE orderAt BETWEEN :from AND :to " +
            "AND (:villageId IS NULL OR villageId = :villageId) " +
            "AND (:cityId IS NULL OR cityId = :cityId) " +
            "AND (:status IS NULL OR status = :status) " +
            "AND (:customerId IS NULL OR customerId = :customerId) " +
            "AND (:q = '' OR customerName LIKE '%' || :q || '%' OR customerMobile LIKE '%' || :q || '%' " +
            "OR CAST(id AS TEXT) = :q) ORDER BY orderAt DESC, id DESC"
    )
    fun observe(
        from: Long, to: Long, villageId: Long?, cityId: Long?,
        status: String?, customerId: Long?, q: String
    ): Flow<List<OrderEntity>>

    @Query(
        "SELECT * FROM orders WHERE orderAt BETWEEN :from AND :to " +
            "AND (:villageId IS NULL OR villageId = :villageId) " +
            "AND (:cityId IS NULL OR cityId = :cityId) " +
            "AND (:status IS NULL OR status = :status) " +
            "AND (:customerId IS NULL OR customerId = :customerId) ORDER BY orderAt, id"
    )
    suspend fun list(
        from: Long, to: Long, villageId: Long?, cityId: Long?, status: String?, customerId: Long?
    ): List<OrderEntity>

    @Query("SELECT COUNT(*) FROM orders WHERE campaignId = :campaignId AND status != 'CANCELLED'")
    fun observeCountForCampaign(campaignId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders WHERE orderAt >= :since AND status != 'CANCELLED'")
    fun observeCountSince(since: Long): Flow<Int>
    @Query("SELECT COALESCE(SUM(total), 0) FROM orders WHERE orderAt >= :since AND status != 'CANCELLED'")
    fun observeSalesSince(since: Long): Flow<Double>
    @Query("SELECT COUNT(*) FROM orders WHERE status IN ('PENDING','CONFIRMED','PROCESSING','OUT_FOR_DELIVERY')")
    fun observeOpenCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM orders WHERE status = 'DELIVERED'")
    fun observeDeliveredCount(): Flow<Int>

    @Query("SELECT * FROM orders") suspend fun getAll(): List<OrderEntity>
    @Query("SELECT * FROM order_items") suspend fun getAllItems(): List<OrderItemEntity>
    @Query("DELETE FROM orders") suspend fun deleteAll()
    @Query("DELETE FROM order_items") suspend fun deleteAllItems()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<OrderEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAllItems(list: List<OrderItemEntity>)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings") fun observeAll(): Flow<List<SettingEntity>>
    @Query("SELECT * FROM settings") suspend fun getAll(): List<SettingEntity>
    @Query("SELECT value FROM settings WHERE id = :id") suspend fun get(id: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(s: SettingEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAll(list: List<SettingEntity>)
    @Query("DELETE FROM settings") suspend fun deleteAll()
}

@Dao
interface ConsentLogDao {
    @Insert suspend fun insert(l: ConsentLogEntity): Long
    @Query("SELECT * FROM consent_log ORDER BY at DESC, id DESC LIMIT 200")
    fun observeRecent(): Flow<List<ConsentLogEntity>>
    @Query("SELECT * FROM consent_log") suspend fun getAll(): List<ConsentLogEntity>
    @Query("DELETE FROM consent_log") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<ConsentLogEntity>)
}
