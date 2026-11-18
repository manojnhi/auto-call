package com.gramcall.ai

import com.gramcall.ai.calling.*
import com.gramcall.ai.database.CallHistoryEntity
import org.junit.Assert.*
import org.junit.Test

class CampaignLogicTest {
    private fun row(pos: Int, status: String) = CallHistoryEntity(
        id = pos.toLong(), campaignId = 1, customerId = pos.toLong(),
        customerName = "C$pos", mobile = "98765432${10 + pos}", status = status, position = pos
    )

    @Test fun nextIsLowestPositionPending() {
        val rows = listOf(row(2, CallStatus.PENDING), row(0, CallStatus.COMPLETED), row(1, CallStatus.PENDING))
        assertEquals(1, CampaignLogic.next(rows)?.position)
    }

    @Test fun nextIsNullWhenNothingPending() {
        assertNull(CampaignLogic.next(listOf(row(0, CallStatus.COMPLETED), row(1, CallStatus.SKIPPED))))
    }

    @Test fun notFinishedWhileCallingOrPending() {
        assertFalse(CampaignLogic.isFinished(listOf(row(0, CallStatus.CALLING))))
        assertFalse(CampaignLogic.isFinished(listOf(row(0, CallStatus.PENDING))))
        assertTrue(CampaignLogic.isFinished(listOf(row(0, CallStatus.NO_ANSWER), row(1, CallStatus.BUSY))))
        assertFalse(CampaignLogic.isFinished(emptyList()))
    }

    @Test fun summaryCountsEveryStatus() {
        val rows = listOf(
            row(0, CallStatus.COMPLETED), row(1, CallStatus.NO_ANSWER), row(2, CallStatus.BUSY),
            row(3, CallStatus.REJECTED), row(4, CallStatus.SKIPPED), row(5, CallStatus.FAILED),
            row(6, CallStatus.PENDING), row(7, CallStatus.CALLING)
        )
        val s = CampaignLogic.summarize(rows)
        assertEquals(8, s.total)
        assertEquals(1, s.completed); assertEquals(1, s.noAnswer); assertEquals(1, s.busy)
        assertEquals(1, s.rejected); assertEquals(1, s.skipped); assertEquals(1, s.failed)
        assertEquals(6, s.done)
    }
}
