package com.gramcall.ai.ui

import android.app.Application
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gramcall.ai.GramCallApp
import com.gramcall.ai.calling.*
import com.gramcall.ai.database.CallHistoryEntity
import com.gramcall.ai.database.CampaignEntity
import com.gramcall.ai.repository.EligibilityPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CallingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as GramCallApp).repository
    private val bizRepo = (app as GramCallApp).biz
    private val sim = SimManager(app)
    private val monitor = CallStateMonitor(app)
    private val started = SharingStarted.WhileSubscribed(5000)

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    // ---- SIM ----
    private val _simInfo = MutableStateFlow(sim.inspect())
    val simInfo: StateFlow<SimInfo> = _simInfo
    fun refreshSim() { _simInfo.value = sim.inspect() }
    fun hasPhonePermissions() = sim.hasPhoneState() && sim.hasCallPermission()

    // ---- campaign state ----
    private val focusId = MutableStateFlow<Long?>(null)
    val activeCampaign: StateFlow<CampaignEntity?> = repo.activeCampaign().stateIn(viewModelScope, started, null)
    val campaign: StateFlow<CampaignEntity?> = focusId
        .flatMapLatest { id -> if (id == null) flowOf<CampaignEntity?>(null) else repo.campaign(id) }
        .stateIn(viewModelScope, started, null)
    val rows: StateFlow<List<CallHistoryEntity>> = focusId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList<CallHistoryEntity>()) else repo.callsForCampaign(id) }
        .stateIn(viewModelScope, started, emptyList())
    val orderCount: StateFlow<Int> = focusId
        .flatMapLatest { id -> if (id == null) flowOf(0) else bizRepo.orderCountForCampaign(id) }
        .stateIn(viewModelScope, started, 0)
    val history: StateFlow<List<CallHistoryEntity>?> = repo.callHistory().stateIn(viewModelScope, started, null)

    // ---- live call tracking (sirf is process mein) ----
    private var dialing = false
    private var dialRowId: Long? = null
    private var offhookAt: Long? = null
    private var timeoutJob: Job? = null

    init {
        viewModelScope.launch {
            repo.recoverInterruptedCalls()
            focusId.value = repo.activeCampaignId()
        }
    }

    suspend fun preview(villageId: Long?, cityId: Long?): EligibilityPreview =
        repo.previewEligible(villageId, cityId)

    fun focusActive() {
        viewModelScope.launch { focusId.value = repo.activeCampaignId() }
    }

    fun dismissCampaign() { focusId.value = null }

    fun startCampaign(villageId: Long?, cityId: Long?, onStarted: () -> Unit) {
        viewModelScope.launch {
            if (!CallingRules.isAllowedNow()) {
                messages.tryEmit("कॉलिंग सिर्फ़ सुबह ${CallingRules.START_HOUR} से रात ${CallingRules.END_HOUR - 12} बजे के बीच")
                return@launch
            }
            val r = repo.startCampaign(villageId, cityId)
            if (r.campaignId == null) {
                messages.tryEmit(r.message)
            } else {
                focusId.value = r.campaignId
                onStarted()
            }
        }
    }

    /** Ek tap = ek call. Kabhi do call ek saath nahi. */
    fun dialNext() {
        if (dialing) return
        val c = campaign.value ?: return
        if (c.status != CampaignStatus.RUNNING) return
        if (!CallingRules.isAllowedNow()) {
            messages.tryEmit("कॉलिंग का समय सुबह ${CallingRules.START_HOUR} से रात ${CallingRules.END_HOUR - 12} बजे तक है")
            return
        }
        val info = sim.inspect()
        _simInfo.value = info
        val handle = info.accountHandle
        if (!info.canCall || handle == null) {
            messages.tryEmit(info.problem ?: "SIM 1 तैयार नहीं है")
            return
        }
        dialing = true
        viewModelScope.launch {
            if (repo.hasCallingRow(c.id)) { dialing = false; return@launch }
            val next = repo.nextPending(c.id)
            if (next == null) {
                dialing = false
                repo.finishIfDone(c.id)
                return@launch
            }
            // Dial se theek pehle sahmati dobara jaanch (opt-out ho gaya ho to call nahi)
            val cust = next.customerId?.let { repo.customerById(it) }
            if (cust == null || !cust.callConsent) {
                dialing = false
                repo.setResult(next.id, CallStatus.SKIPPED, "सहमति नहीं / ग्राहक हटा दिया गया")
                repo.finishIfDone(c.id)
                messages.tryEmit("${next.customerName}: सहमति नहीं है, कॉल नहीं की गई")
                return@launch
            }
            repo.markCalling(next.id, System.currentTimeMillis())
            dialRowId = next.id
            offhookAt = null
            monitor.start { onCallState(it) }
            timeoutJob = launch {
                delay(CallingRules.NO_OFFHOOK_TIMEOUT_MS)
                if (offhookAt == null) endCall(0)
            }
            sim.placeCall(next.mobile, handle).onFailure { e ->
                timeoutJob?.cancel()
                monitor.stop()
                dialing = false
                dialRowId = null
                repo.setResult(next.id, CallStatus.FAILED, "कॉल शुरू नहीं हो सकी: ${e.message}")
                repo.finishIfDone(c.id)
            }
        }
    }

    private fun onCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> if (offhookAt == null) offhookAt = System.currentTimeMillis()
            TelephonyManager.CALL_STATE_IDLE -> offhookAt?.let { endCall(((System.currentTimeMillis() - it) / 1000).toInt()) }
        }
    }

    private fun endCall(durationSec: Int) {
        timeoutJob?.cancel()
        timeoutJob = null
        monitor.stop()
        dialing = false
        val id = dialRowId ?: return
        dialRowId = null
        offhookAt = null
        viewModelScope.launch { repo.finishCall(id, System.currentTimeMillis(), durationSec) }
    }

    fun setResult(row: CallHistoryEntity, status: String) {
        viewModelScope.launch {
            repo.setResult(row.id, status)
            repo.finishIfDone(row.campaignId)
        }
    }

    private fun whenIdle(block: suspend (CampaignEntity) -> Unit) {
        val c = campaign.value ?: return
        viewModelScope.launch {
            if (repo.hasCallingRow(c.id)) {
                messages.tryEmit("कॉल/नतीजा बाकी है — पहले वह पूरा करें")
            } else {
                block(c)
            }
        }
    }

    fun pause() = whenIdle { repo.setCampaignStatus(it.id, CampaignStatus.PAUSED) }
    fun resume() = whenIdle { repo.setCampaignStatus(it.id, CampaignStatus.RUNNING) }
    fun skipNext() = whenIdle { repo.skipNext(it.id) }
    fun stop() = whenIdle { repo.stopCampaign(it.id) }

    override fun onCleared() {
        monitor.stop()
        super.onCleared()
    }
}
