package com.gramcall.ai

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gramcall.ai.calling.CallStatus
import com.gramcall.ai.calling.CampaignStatus
import com.gramcall.ai.database.AppDatabase
import com.gramcall.ai.database.CustomerEntity
import com.gramcall.ai.database.VillageEntity
import com.gramcall.ai.repository.GramRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampaignRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: GramRepository
    private var villageId = 0L

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = GramRepository(db)
        repo.saveVillage(VillageEntity(name = "इटारौरा"))
        villageId = repo.villages("").first().first().village.id
        // 3 sahmati wale, 1 bina sahmati
        repo.saveCustomer(CustomerEntity(name = "A", mobile = "9876543210", villageId = villageId, callConsent = true))
        repo.saveCustomer(CustomerEntity(name = "B", mobile = "9876543211", villageId = villageId, callConsent = true))
        repo.saveCustomer(CustomerEntity(name = "C", mobile = "9876543212", villageId = villageId, callConsent = true))
        repo.saveCustomer(CustomerEntity(name = "D", mobile = "9876543213", villageId = villageId, callConsent = false))
        Unit
    }

    @After fun tearDown() = db.close()

    @Test fun onlyConsentedCustomersAreQueued() = runBlocking {
        val p = repo.previewEligible(villageId, null)
        assertEquals(4, p.totalCustomers); assertEquals(1, p.noConsent); assertEquals(3, p.eligible.size)
        val r = repo.startCampaign(villageId, null)
        assertNotNull(r.campaignId)
        assertEquals(3, repo.callsForCampaign(r.campaignId!!).first().size)
    }

    @Test fun secondCampaignRejectedWhileOneActive() = runBlocking {
        assertNotNull(repo.startCampaign(villageId, null).campaignId)
        assertNull(repo.startCampaign(villageId, null).campaignId)
    }

    @Test fun sequentialFlowCompletesCampaign() = runBlocking {
        val cid = repo.startCampaign(villageId, null).campaignId!!
        repeat(3) {
            val next = repo.nextPending(cid)!!
            repo.markCalling(next.id, 1000L)
            assertTrue(repo.hasCallingRow(cid))
            repo.finishCall(next.id, 2000L, 5)
            repo.setResult(next.id, CallStatus.COMPLETED)
            repo.finishIfDone(cid)
        }
        assertNull(repo.nextPending(cid))
        assertEquals(CampaignStatus.COMPLETED, repo.campaign(cid).first()!!.status)
    }

    @Test fun skipAndStop() = runBlocking {
        val cid = repo.startCampaign(villageId, null).campaignId!!
        repo.skipNext(cid)
        assertEquals(CallStatus.SKIPPED, repo.callsForCampaign(cid).first()[0].status)
        repo.stopCampaign(cid)
        assertEquals(CampaignStatus.STOPPED, repo.campaign(cid).first()!!.status)
        assertNull(repo.nextPending(cid))
    }

    @Test fun recentNoAnswerIsExcludedFromNextCampaign() = runBlocking {
        val cid = repo.startCampaign(villageId, null).campaignId!!
        val first = repo.nextPending(cid)!!
        repo.markCalling(first.id, System.currentTimeMillis())
        repo.setResult(first.id, CallStatus.NO_ANSWER)
        repo.stopCampaign(cid)
        val p = repo.previewEligible(villageId, null)
        assertEquals(1, p.recentlyUnanswered); assertEquals(2, p.eligible.size)
    }

    @Test fun interruptedCallBecomesAwaitingResult() = runBlocking {
        val cid = repo.startCampaign(villageId, null).campaignId!!
        val n = repo.nextPending(cid)!!
        repo.markCalling(n.id, 1000L)
        repo.recoverInterruptedCalls()
        val row = repo.callsForCampaign(cid).first().first { it.id == n.id }
        assertEquals(CallStatus.CALLING, row.status); assertNotNull(row.endedAt)
    }
}
