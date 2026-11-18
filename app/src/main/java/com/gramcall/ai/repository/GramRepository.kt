package com.gramcall.ai.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.gramcall.ai.calling.CallStatus
import com.gramcall.ai.calling.CallingRules
import com.gramcall.ai.calling.CampaignStatus
import com.gramcall.ai.database.*
import com.gramcall.ai.utils.CsvUtils
import com.gramcall.ai.utils.PhoneUtils
import kotlinx.coroutines.flow.Flow

data class OpResult(val ok: Boolean, val message: String = "") {
    companion object {
        fun success(msg: String = "") = OpResult(true, msg)
        fun fail(msg: String) = OpResult(false, msg)
    }
}

data class EligibilityPreview(
    val totalCustomers: Int,
    val noConsent: Int,
    val recentlyUnanswered: Int,
    val eligible: List<CustomerEntity>
)

data class StartResult(val campaignId: Long?, val message: String)

data class ImportSummary(val added: Int, val duplicates: Int, val invalid: Int) {
    fun message() = "इम्पोर्ट पूरा: $added जोड़े गए, $duplicates डुप्लिकेट, $invalid अमान्य"
}

class GramRepository(private val db: AppDatabase) {
    private val villageDao = db.villageDao()
    private val cityDao = db.cityDao()
    private val areaDao = db.areaDao()
    private val customerDao = db.customerDao()
    private val campaignDao = db.campaignDao()
    private val callDao = db.callHistoryDao()
    private val consentDao = db.consentLogDao()

    // ---------- observe ----------
    fun villages(q: String): Flow<List<VillageWithCount>> = villageDao.observe(q)
    fun allVillages(): Flow<List<VillageEntity>> = villageDao.observeAll()
    fun cities(q: String): Flow<List<CityWithCount>> = cityDao.observe(q)
    fun allCities(): Flow<List<CityEntity>> = cityDao.observeAll()
    fun areas(cityId: Long): Flow<List<AreaEntity>> = areaDao.observe(cityId)
    fun customers(q: String, villageId: Long?, cityId: Long?): Flow<List<CustomerWithPlace>> =
        customerDao.observe(q, villageId, cityId)
    fun customer(id: Long): Flow<CustomerWithPlace?> = customerDao.observeById(id)
    suspend fun customerById(id: Long): CustomerEntity? = customerDao.getById(id)
    fun customerCount(): Flow<Int> = customerDao.observeCount()
    fun villageCount(): Flow<Int> = villageDao.observeCount()
    fun cityCount(): Flow<Int> = cityDao.observeCount()

    // ---------- villages ----------
    suspend fun saveVillage(v: VillageEntity): OpResult {
        val name = v.name.trim()
        if (name.isEmpty()) return OpResult.fail("गाँव का नाम ज़रूरी है")
        if (v.pinCode.isNotBlank() && !v.pinCode.trim().matches(Regex("\\d{6}")))
            return OpResult.fail("PIN कोड 6 अंकों का होना चाहिए")
        val clean = v.copy(
            name = name, district = v.district.trim(), state = v.state.trim(),
            pinCode = v.pinCode.trim(), notes = v.notes.trim()
        )
        return try {
            if (v.id == 0L) villageDao.insert(clean) else villageDao.update(clean)
            OpResult.success("गाँव सेव हो गया")
        } catch (e: SQLiteConstraintException) {
            OpResult.fail("यह गाँव (इसी ज़िले में) पहले से मौजूद है")
        }
    }

    suspend fun deleteVillage(v: VillageEntity): OpResult {
        villageDao.delete(v)
        return OpResult.success("गाँव हटा दिया गया")
    }

    // ---------- cities ----------
    suspend fun saveCity(c: CityEntity): OpResult {
        val name = c.name.trim()
        if (name.isEmpty()) return OpResult.fail("शहर का नाम ज़रूरी है")
        val clean = c.copy(
            name = name, district = c.district.trim(), state = c.state.trim(), notes = c.notes.trim()
        )
        return try {
            if (c.id == 0L) cityDao.insert(clean) else cityDao.update(clean)
            OpResult.success("शहर सेव हो गया")
        } catch (e: SQLiteConstraintException) {
            OpResult.fail("यह शहर (इसी राज्य में) पहले से मौजूद है")
        }
    }

    suspend fun deleteCity(c: CityEntity): OpResult {
        cityDao.delete(c)
        return OpResult.success("शहर हटा दिया गया")
    }

    suspend fun addArea(cityId: Long, name: String): OpResult {
        val n = name.trim()
        if (n.isEmpty()) return OpResult.fail("इलाके का नाम लिखें")
        return try {
            areaDao.insert(AreaEntity(cityId = cityId, name = n))
            OpResult.success("इलाका जोड़ दिया गया")
        } catch (e: SQLiteConstraintException) {
            OpResult.fail("यह इलाका पहले से मौजूद है")
        }
    }

    suspend fun deleteArea(a: AreaEntity) = areaDao.delete(a)

    // ---------- customers ----------
    suspend fun saveCustomer(c: CustomerEntity): OpResult {
        val name = c.name.trim()
        if (name.isEmpty()) return OpResult.fail("ग्राहक का नाम ज़रूरी है")
        val mobile = PhoneUtils.normalize(c.mobile)
        if (!PhoneUtils.isValid(mobile))
            return OpResult.fail("मोबाइल नंबर 10 अंकों का (6-9 से शुरू) होना चाहिए")
        val alt = PhoneUtils.normalize(c.altMobile)
        if (alt.isNotEmpty() && !PhoneUtils.isValid(alt))
            return OpResult.fail("दूसरा मोबाइल नंबर सही नहीं है")
        if (c.villageId == null && c.cityId == null)
            return OpResult.fail("गाँव या शहर चुनें")
        val dup = customerDao.findByMobile(mobile)
        if (dup != null && dup.id != c.id)
            return OpResult.fail("यह नंबर पहले से ${dup.name} के नाम से दर्ज है")
        val clean = c.copy(
            name = name, mobile = mobile, altMobile = alt, address = c.address.trim(),
            preferredTime = c.preferredTime.trim(), notes = c.notes.trim()
        )
        val prev = if (c.id != 0L) customerDao.getById(c.id) else null
        return try {
            val newId = if (c.id == 0L) customerDao.insert(clean) else { customerDao.update(clean); c.id }
            val changed = if (prev == null) clean.callConsent else prev.callConsent != clean.callConsent
            if (changed) {
                consentDao.insert(
                    ConsentLogEntity(
                        customerId = newId, customerName = name, granted = clean.callConsent,
                        source = if (clean.callConsent) "FORM_GRANT" else "FORM_REVOKE"
                    )
                )
            }
            OpResult.success("ग्राहक सेव हो गया")
        } catch (e: SQLiteConstraintException) {
            OpResult.fail("यह नंबर पहले से दर्ज है")
        }
    }

    /** Ek tap mein sahmati hatao (opt-out) aur log mein likho. */
    suspend fun optOutCustomer(c: CustomerEntity): OpResult {
        val cur = customerDao.getById(c.id) ?: return OpResult.fail("ग्राहक नहीं मिला")
        if (!cur.callConsent) return OpResult.success("पहले से कॉल बंद है")
        customerDao.update(cur.copy(callConsent = false))
        consentDao.insert(ConsentLogEntity(customerId = cur.id, customerName = cur.name, granted = false, source = "OPT_OUT"))
        return OpResult.success("कॉल बंद (opt-out) कर दी गई")
    }

    fun consentLog() = consentDao.observeRecent()

    suspend fun deleteCustomer(c: CustomerEntity): OpResult {
        customerDao.delete(c)
        return OpResult.success("ग्राहक हटा दिया गया")
    }

    // ---------- calling campaigns ----------
    fun campaign(id: Long): Flow<CampaignEntity?> = campaignDao.observeById(id)
    fun activeCampaign(): Flow<CampaignEntity?> = campaignDao.observeActive()
    suspend fun activeCampaignId(): Long? = campaignDao.getActive()?.id
    fun callsForCampaign(id: Long): Flow<List<CallHistoryEntity>> = callDao.observeForCampaign(id)
    fun callHistory(): Flow<List<CallHistoryEntity>> = callDao.observeHistory()
    fun callsForCustomer(id: Long): Flow<List<CallHistoryEntity>> = callDao.observeForCustomer(id)
    fun callsSince(since: Long): Flow<Int> = callDao.observeCallsSince(since)
    fun callsWithStatusSince(since: Long, status: String): Flow<Int> =
        callDao.observeCountByStatusSince(since, status)

    /** Kaun-kaun call ke layak hai: sahmati ho, 24 ghante mein NO_ANSWER/REJECTED na ho. */
    suspend fun previewEligible(villageId: Long?, cityId: Long?): EligibilityPreview {
        val all = when {
            villageId != null -> customerDao.getByVillage(villageId)
            cityId != null -> customerDao.getByCity(cityId)
            else -> emptyList()
        }
        val consented = all.filter { it.callConsent }
        val since = System.currentTimeMillis() - CallingRules.RECENT_UNANSWERED_HOURS * 3_600_000L
        val blocked = callDao.recentUnansweredCustomerIds(since).toSet()
        val eligible = consented.filter { it.id !in blocked && PhoneUtils.isValid(it.mobile) }
        return EligibilityPreview(
            totalCustomers = all.size,
            noConsent = all.size - consented.size,
            recentlyUnanswered = consented.count { it.id in blocked },
            eligible = eligible
        )
    }

    suspend fun startCampaign(villageId: Long?, cityId: Long?): StartResult = db.withTransaction {
        if (villageId == null && cityId == null) return@withTransaction StartResult(null, "गाँव या शहर चुनें")
        if (campaignDao.getActive() != null)
            return@withTransaction StartResult(null, "एक अभियान पहले से चालू है — पहले उसे पूरा या बंद करें")
        val preview = previewEligible(villageId, cityId)
        if (preview.eligible.isEmpty())
            return@withTransaction StartResult(null, "कॉल के लिए कोई योग्य ग्राहक नहीं (सहमति/हाल का नो-आंसर देखें)")
        val name = if (villageId != null) villageDao.getById(villageId)?.name else cityDao.getById(cityId!!)?.name
        val title = "${name ?: "अभियान"} कॉल अभियान"
        val cid = campaignDao.insert(
            CampaignEntity(
                title = title, villageId = villageId, cityId = cityId, status = CampaignStatus.RUNNING
            )
        )
        callDao.insertAll(
            preview.eligible.mapIndexed { i, c ->
                CallHistoryEntity(
                    campaignId = cid, customerId = c.id, customerName = c.name, mobile = c.mobile,
                    status = CallStatus.PENDING, position = i
                )
            }
        )
        StartResult(cid, "")
    }

    suspend fun nextPending(campaignId: Long): CallHistoryEntity? = callDao.nextPending(campaignId)
    suspend fun hasCallingRow(campaignId: Long): Boolean = callDao.countCalling(campaignId) > 0

    suspend fun markCalling(rowId: Long, now: Long) {
        val r = callDao.getById(rowId) ?: return
        callDao.update(r.copy(status = CallStatus.CALLING, startedAt = now, endedAt = null, durationSec = 0))
    }

    /** Call khatam hui (ya shuru hi nahi hui); natija abhi owner chunega. */
    suspend fun finishCall(rowId: Long, endedAt: Long, durationSec: Int) {
        val r = callDao.getById(rowId) ?: return
        callDao.update(r.copy(endedAt = endedAt, durationSec = durationSec))
    }

    suspend fun setResult(rowId: Long, status: String, note: String = "") {
        val r = callDao.getById(rowId) ?: return
        callDao.update(
            r.copy(
                status = status,
                endedAt = r.endedAt ?: System.currentTimeMillis(),
                note = note.ifEmpty { r.note }
            )
        )
    }

    /** App restart ke baad: adhuri CALLING rows ko "natija chuno" state mein daalo. */
    suspend fun recoverInterruptedCalls() = callDao.markInterrupted(System.currentTimeMillis())

    suspend fun setCampaignStatus(id: Long, status: String) {
        val c = campaignDao.getById(id) ?: return
        campaignDao.update(c.copy(status = status))
    }

    suspend fun skipNext(campaignId: Long) {
        val r = callDao.nextPending(campaignId) ?: return
        callDao.update(r.copy(status = CallStatus.SKIPPED, note = "छोड़ दिया गया"))
        finishIfDone(campaignId)
    }

    suspend fun stopCampaign(campaignId: Long) {
        db.withTransaction {
            callDao.skipAllPending(campaignId, "अभियान रोका गया")
            val c = campaignDao.getById(campaignId) ?: return@withTransaction
            campaignDao.update(c.copy(status = CampaignStatus.STOPPED, finishedAt = System.currentTimeMillis()))
        }
    }

    suspend fun finishIfDone(campaignId: Long) {
        val c = campaignDao.getById(campaignId) ?: return
        val active = c.status == CampaignStatus.RUNNING || c.status == CampaignStatus.PAUSED
        if (active && callDao.countOpen(campaignId) == 0) {
            campaignDao.update(c.copy(status = CampaignStatus.COMPLETED, finishedAt = System.currentTimeMillis()))
        }
    }

    // ---------- CSV ----------
    suspend fun exportCsv(): String {
        val rows = mutableListOf<List<String>>(CSV_COLUMNS)
        for (cw in customerDao.getAllWithPlace()) {
            val c = cw.customer
            rows.add(
                listOf(
                    c.name, c.mobile, c.altMobile, cw.villageName.orEmpty(), cw.cityName.orEmpty(),
                    c.address, c.category, c.language, if (c.callConsent) "yes" else "no",
                    c.preferredTime, c.notes
                )
            )
        }
        return CsvUtils.toCsv(rows)
    }

    suspend fun importCsv(text: String): ImportSummary {
        val rows = CsvUtils.parse(text)
        if (rows.isEmpty()) return ImportSummary(0, 0, 0)
        val first = rows.first().map { it.trim().lowercase() }
        val hasHeader = "mobile" in first || "name" in first
        val cols = if (hasHeader) first else CSV_COLUMNS
        val idx = cols.withIndex().associate { (i, h) -> h to i }
        val data = if (hasHeader) rows.drop(1) else rows

        fun cell(r: List<String>, key: String): String =
            idx[key]?.let { r.getOrNull(it) }?.trim().orEmpty()

        var added = 0
        var dups = 0
        var invalid = 0
        db.withTransaction {
            for (r in data) {
                val name = cell(r, "name")
                val mobile = PhoneUtils.normalize(cell(r, "mobile"))
                val alt = PhoneUtils.normalize(cell(r, "alt_mobile"))
                val villageName = cell(r, "village")
                val cityName = cell(r, "city")
                if (name.isEmpty() || !PhoneUtils.isValid(mobile) ||
                    (alt.isNotEmpty() && !PhoneUtils.isValid(alt)) ||
                    (villageName.isEmpty() && cityName.isEmpty())
                ) {
                    invalid++
                    continue
                }
                if (customerDao.findByMobile(mobile) != null) {
                    dups++
                    continue
                }
                val villageId: Long? = if (villageName.isEmpty()) null else
                    (villageDao.findByName(villageName)?.id
                        ?: villageDao.insert(VillageEntity(name = villageName)))
                val cityId: Long? = if (cityName.isEmpty()) null else
                    (cityDao.findByName(cityName)?.id
                        ?: cityDao.insert(CityEntity(name = cityName)))
                val consent = cell(r, "consent").lowercase() in CONSENT_YES
                val newCid = customerDao.insert(
                    CustomerEntity(
                        name = name, mobile = mobile, altMobile = alt,
                        villageId = villageId, cityId = cityId,
                        address = cell(r, "address"),
                        category = cell(r, "category").ifEmpty { "सामान्य" },
                        language = cell(r, "language").ifEmpty { "हिंदी" },
                        callConsent = consent,
                        consentDate = if (consent) System.currentTimeMillis() else null,
                        preferredTime = cell(r, "preferred_time"),
                        notes = cell(r, "notes")
                    )
                )
                if (consent) {
                    consentDao.insert(ConsentLogEntity(customerId = newCid, customerName = name, granted = true, source = "CSV_IMPORT"))
                }
                added++
            }
        }
        return ImportSummary(added, dups, invalid)
    }

    companion object {
        val CSV_COLUMNS = listOf(
            "name", "mobile", "alt_mobile", "village", "city", "address",
            "category", "language", "consent", "preferred_time", "notes"
        )
        private val CONSENT_YES = setOf("yes", "y", "true", "1", "हाँ", "हां", "ha", "haan")
    }
}
