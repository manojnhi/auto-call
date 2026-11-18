package com.gramcall.ai.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Insert suspend fun insert(c: CampaignEntity): Long
    @Query("SELECT * FROM campaigns") suspend fun getAll(): List<CampaignEntity>
    @Query("DELETE FROM campaigns") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<CampaignEntity>)
    @Update suspend fun update(c: CampaignEntity)
    @Query("SELECT * FROM campaigns WHERE id = :id") suspend fun getById(id: Long): CampaignEntity?
    @Query("SELECT * FROM campaigns WHERE id = :id") fun observeById(id: Long): Flow<CampaignEntity?>
    @Query("SELECT * FROM campaigns WHERE status IN ('RUNNING','PAUSED') ORDER BY id DESC LIMIT 1")
    suspend fun getActive(): CampaignEntity?
    @Query("SELECT * FROM campaigns WHERE status IN ('RUNNING','PAUSED') ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<CampaignEntity?>
}

@Dao
interface CallHistoryDao {
    @Insert suspend fun insertAll(rows: List<CallHistoryEntity>)
    @Query("SELECT * FROM call_history") suspend fun getAll(): List<CallHistoryEntity>
    @Query("DELETE FROM call_history") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreAll(rows: List<CallHistoryEntity>)

    @Query(
        "SELECT h.* FROM call_history h LEFT JOIN customers c ON c.id = h.customerId " +
            "WHERE h.startedAt BETWEEN :from AND :to AND h.status NOT IN ('PENDING','CALLING','SKIPPED') " +
            "AND (:villageId IS NULL OR c.villageId = :villageId) " +
            "AND (:cityId IS NULL OR c.cityId = :cityId) " +
            "AND (:customerId IS NULL OR h.customerId = :customerId) ORDER BY h.startedAt, h.id"
    )
    suspend fun reportCalls(from: Long, to: Long, villageId: Long?, cityId: Long?, customerId: Long?): List<CallHistoryEntity>
    @Update suspend fun update(row: CallHistoryEntity)
    @Query("SELECT * FROM call_history WHERE id = :id") suspend fun getById(id: Long): CallHistoryEntity?

    @Query("SELECT * FROM call_history WHERE campaignId = :campaignId ORDER BY position")
    fun observeForCampaign(campaignId: Long): Flow<List<CallHistoryEntity>>

    @Query("SELECT * FROM call_history WHERE campaignId = :campaignId AND status = 'PENDING' ORDER BY position LIMIT 1")
    suspend fun nextPending(campaignId: Long): CallHistoryEntity?

    @Query("SELECT COUNT(*) FROM call_history WHERE campaignId = :campaignId AND status = 'CALLING'")
    suspend fun countCalling(campaignId: Long): Int

    @Query("SELECT COUNT(*) FROM call_history WHERE campaignId = :campaignId AND status IN ('PENDING','CALLING')")
    suspend fun countOpen(campaignId: Long): Int

    @Query("UPDATE call_history SET status = 'SKIPPED', note = :note WHERE campaignId = :campaignId AND status = 'PENDING'")
    suspend fun skipAllPending(campaignId: Long, note: String)

    @Query("UPDATE call_history SET endedAt = :now WHERE status = 'CALLING' AND endedAt IS NULL")
    suspend fun markInterrupted(now: Long)

    @Query(
        "SELECT * FROM call_history WHERE status NOT IN ('PENDING','SKIPPED') " +
            "ORDER BY COALESCE(startedAt, 0) DESC, id DESC LIMIT 300"
    )
    fun observeHistory(): Flow<List<CallHistoryEntity>>

    @Query(
        "SELECT * FROM call_history WHERE customerId = :customerId AND status NOT IN ('PENDING','SKIPPED') " +
            "ORDER BY COALESCE(startedAt, 0) DESC, id DESC LIMIT 50"
    )
    fun observeForCustomer(customerId: Long): Flow<List<CallHistoryEntity>>

    @Query(
        "SELECT DISTINCT customerId FROM call_history WHERE status IN ('NO_ANSWER','REJECTED') " +
            "AND startedAt >= :since AND customerId IS NOT NULL"
    )
    suspend fun recentUnansweredCustomerIds(since: Long): List<Long>

    @Query(
        "SELECT COUNT(*) FROM call_history WHERE startedAt >= :since " +
            "AND status NOT IN ('PENDING','SKIPPED','CALLING')"
    )
    fun observeCallsSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_history WHERE startedAt >= :since AND status = :status")
    fun observeCountByStatusSince(since: Long, status: String): Flow<Int>
}
