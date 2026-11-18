package com.gramcall.ai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gramcall.ai.GramCallApp
import com.gramcall.ai.calling.CallStatus
import com.gramcall.ai.database.*
import com.gramcall.ai.repository.OpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as GramCallApp).repository
    private val biz = (app as GramCallApp).biz
    private val started = SharingStarted.WhileSubscribed(5000)

    /** Snackbar messages */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    // ---- counts (null = loading) ----
    val customerCount: StateFlow<Int?> = repo.customerCount().stateIn(viewModelScope, started, null)
    val villageCount: StateFlow<Int?> = repo.villageCount().stateIn(viewModelScope, started, null)
    val cityCount: StateFlow<Int?> = repo.cityCount().stateIn(viewModelScope, started, null)

    // ---- aaj ke call counts ----
    private val dayStart: Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val callsToday: StateFlow<Int?> = repo.callsSince(dayStart).stateIn(viewModelScope, started, null)
    val successToday: StateFlow<Int?> =
        repo.callsWithStatusSince(dayStart, CallStatus.COMPLETED).stateIn(viewModelScope, started, null)
    val noAnswerToday: StateFlow<Int?> =
        repo.callsWithStatusSince(dayStart, CallStatus.NO_ANSWER).stateIn(viewModelScope, started, null)
    fun callsForCustomer(id: Long) = repo.callsForCustomer(id)
    fun ordersForCustomer(id: Long) = biz.orders(0L, Long.MAX_VALUE, null, null, null, id, "")
    val consentLog: StateFlow<List<ConsentLogEntity>> =
        repo.consentLog().stateIn(viewModelScope, started, emptyList())
    fun optOut(c: CustomerEntity) = launchOp({ repo.optOutCustomer(c) }, {})

    // ---- villages ----
    val villageQuery = MutableStateFlow("")
    val villages: StateFlow<List<VillageWithCount>?> =
        villageQuery.flatMapLatest { repo.villages(it.trim()) }.stateIn(viewModelScope, started, null)
    val allVillages: StateFlow<List<VillageEntity>> =
        repo.allVillages().stateIn(viewModelScope, started, emptyList())

    // ---- cities ----
    val cityQuery = MutableStateFlow("")
    val cities: StateFlow<List<CityWithCount>?> =
        cityQuery.flatMapLatest { repo.cities(it.trim()) }.stateIn(viewModelScope, started, null)
    val allCities: StateFlow<List<CityEntity>> =
        repo.allCities().stateIn(viewModelScope, started, emptyList())
    fun areas(cityId: Long): Flow<List<AreaEntity>> = repo.areas(cityId)

    // ---- customers ----
    val customerQuery = MutableStateFlow("")
    val customerVillageFilter = MutableStateFlow<Long?>(null)
    val customerCityFilter = MutableStateFlow<Long?>(null)
    val customers: StateFlow<List<CustomerWithPlace>?> =
        combine(customerQuery, customerVillageFilter, customerCityFilter) { q, v, c -> Triple(q, v, c) }
            .flatMapLatest { (q, v, c) -> repo.customers(q.trim(), v, c) }
            .stateIn(viewModelScope, started, null)
    fun customer(id: Long): Flow<CustomerWithPlace?> = repo.customer(id)
    suspend fun loadCustomer(id: Long): CustomerEntity? = repo.customerById(id)

    // ---- actions ----
    private fun launchOp(block: suspend () -> OpResult, onDone: (OpResult) -> Unit) {
        viewModelScope.launch {
            val r = block()
            if (r.message.isNotEmpty()) messages.tryEmit(r.message)
            onDone(r)
        }
    }

    fun saveVillage(v: VillageEntity, onDone: (OpResult) -> Unit = {}) = launchOp({ repo.saveVillage(v) }, onDone)
    fun deleteVillage(v: VillageEntity) = launchOp({ repo.deleteVillage(v) }, {})
    fun saveCity(c: CityEntity, onDone: (OpResult) -> Unit = {}) = launchOp({ repo.saveCity(c) }, onDone)
    fun deleteCity(c: CityEntity) = launchOp({ repo.deleteCity(c) }, {})
    fun addArea(cityId: Long, name: String, onDone: (OpResult) -> Unit = {}) =
        launchOp({ repo.addArea(cityId, name) }, onDone)
    fun deleteArea(a: AreaEntity) { viewModelScope.launch { repo.deleteArea(a) } }
    fun saveCustomer(c: CustomerEntity, onDone: (OpResult) -> Unit = {}) = launchOp({ repo.saveCustomer(c) }, onDone)
    fun deleteCustomer(c: CustomerEntity, onDone: (OpResult) -> Unit = {}) = launchOp({ repo.deleteCustomer(c) }, onDone)

    // ---- CSV ----
    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: throw IllegalStateException("फ़ाइल नहीं खुल सकी")
                }
                messages.tryEmit(repo.importCsv(text).message())
            } catch (e: Exception) {
                messages.tryEmit("इम्पोर्ट विफल: ${e.message}")
            }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val csv = repo.exportCsv()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                        // BOM taaki Excel mein Hindi sahi dikhe
                        it.write(("\uFEFF" + csv).toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("फ़ाइल नहीं खुल सकी")
                }
                messages.tryEmit("ग्राहक सूची एक्सपोर्ट हो गई")
            } catch (e: Exception) {
                messages.tryEmit("एक्सपोर्ट विफल: ${e.message}")
            }
        }
    }
}
