package com.gramcall.ai.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class VillageWithCount(@Embedded val village: VillageEntity, val customerCount: Int)
data class CityWithCount(@Embedded val city: CityEntity, val customerCount: Int)
data class CustomerWithPlace(
    @Embedded val customer: CustomerEntity,
    val villageName: String?,
    val cityName: String?
)

@Dao
interface VillageDao {
    @Insert suspend fun insert(v: VillageEntity): Long
    @Query("SELECT * FROM villages") suspend fun getAll(): List<VillageEntity>
    @Query("DELETE FROM villages") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<VillageEntity>)
    @Update suspend fun update(v: VillageEntity)
    @Delete suspend fun delete(v: VillageEntity)

    @Query(
        "SELECT v.*, (SELECT COUNT(*) FROM customers c WHERE c.villageId = v.id) AS customerCount " +
            "FROM villages v WHERE v.name LIKE '%' || :q || '%' ORDER BY v.name COLLATE NOCASE"
    )
    fun observe(q: String): Flow<List<VillageWithCount>>

    @Query("SELECT * FROM villages ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<VillageEntity>>

    @Query("SELECT COUNT(*) FROM villages") fun observeCount(): Flow<Int>

    @Query("SELECT * FROM villages WHERE id = :id") suspend fun getById(id: Long): VillageEntity?

    @Query("SELECT * FROM villages WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): VillageEntity?
}

@Dao
interface CityDao {
    @Insert suspend fun insert(c: CityEntity): Long
    @Query("SELECT * FROM cities") suspend fun getAll(): List<CityEntity>
    @Query("DELETE FROM cities") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<CityEntity>)
    @Update suspend fun update(c: CityEntity)
    @Delete suspend fun delete(c: CityEntity)

    @Query(
        "SELECT ci.*, (SELECT COUNT(*) FROM customers c WHERE c.cityId = ci.id) AS customerCount " +
            "FROM cities ci WHERE ci.name LIKE '%' || :q || '%' ORDER BY ci.name COLLATE NOCASE"
    )
    fun observe(q: String): Flow<List<CityWithCount>>

    @Query("SELECT * FROM cities ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CityEntity>>

    @Query("SELECT COUNT(*) FROM cities") fun observeCount(): Flow<Int>

    @Query("SELECT * FROM cities WHERE id = :id") suspend fun getById(id: Long): CityEntity?

    @Query("SELECT * FROM cities WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): CityEntity?
}

@Dao
interface AreaDao {
    @Insert suspend fun insert(a: AreaEntity): Long
    @Query("SELECT * FROM areas") suspend fun getAll(): List<AreaEntity>
    @Query("DELETE FROM areas") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<AreaEntity>)
    @Delete suspend fun delete(a: AreaEntity)

    @Query("SELECT * FROM areas WHERE cityId = :cityId ORDER BY name COLLATE NOCASE")
    fun observe(cityId: Long): Flow<List<AreaEntity>>
}

@Dao
interface CustomerDao {
    @Insert suspend fun insert(c: CustomerEntity): Long
    @Query("SELECT * FROM customers") suspend fun getAll(): List<CustomerEntity>
    @Query("DELETE FROM customers") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<CustomerEntity>)
    @Query(
        "SELECT c.*, v.name AS villageName, ci.name AS cityName FROM customers c " +
            "LEFT JOIN villages v ON v.id = c.villageId " +
            "LEFT JOIN cities ci ON ci.id = c.cityId WHERE c.id = :id"
    )
    suspend fun getWithPlace(id: Long): CustomerWithPlace?
    @Update suspend fun update(c: CustomerEntity)
    @Delete suspend fun delete(c: CustomerEntity)

    @Query(
        "SELECT c.*, v.name AS villageName, ci.name AS cityName FROM customers c " +
            "LEFT JOIN villages v ON v.id = c.villageId " +
            "LEFT JOIN cities ci ON ci.id = c.cityId " +
            "WHERE (:q = '' OR c.name LIKE '%' || :q || '%' OR c.mobile LIKE '%' || :q || '%') " +
            "AND (:villageId IS NULL OR c.villageId = :villageId) " +
            "AND (:cityId IS NULL OR c.cityId = :cityId) " +
            "ORDER BY c.name COLLATE NOCASE"
    )
    fun observe(q: String, villageId: Long?, cityId: Long?): Flow<List<CustomerWithPlace>>

    @Query(
        "SELECT c.*, v.name AS villageName, ci.name AS cityName FROM customers c " +
            "LEFT JOIN villages v ON v.id = c.villageId " +
            "LEFT JOIN cities ci ON ci.id = c.cityId WHERE c.id = :id"
    )
    fun observeById(id: Long): Flow<CustomerWithPlace?>

    @Query(
        "SELECT c.*, v.name AS villageName, ci.name AS cityName FROM customers c " +
            "LEFT JOIN villages v ON v.id = c.villageId " +
            "LEFT JOIN cities ci ON ci.id = c.cityId ORDER BY c.name COLLATE NOCASE"
    )
    suspend fun getAllWithPlace(): List<CustomerWithPlace>

    @Query("SELECT * FROM customers WHERE villageId = :villageId ORDER BY name COLLATE NOCASE")
    suspend fun getByVillage(villageId: Long): List<CustomerEntity>
    @Query("SELECT * FROM customers WHERE cityId = :cityId ORDER BY name COLLATE NOCASE")
    suspend fun getByCity(cityId: Long): List<CustomerEntity>
    @Query("SELECT * FROM customers WHERE id = :id") suspend fun getById(id: Long): CustomerEntity?
    @Query("SELECT * FROM customers WHERE mobile = :mobile LIMIT 1")
    suspend fun findByMobile(mobile: String): CustomerEntity?
    @Query("SELECT COUNT(*) FROM customers") fun observeCount(): Flow<Int>
}
