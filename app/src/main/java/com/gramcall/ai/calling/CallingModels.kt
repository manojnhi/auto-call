package com.gramcall.ai.calling

import android.Manifest
import android.telecom.PhoneAccountHandle
import com.gramcall.ai.database.CallHistoryEntity
import java.util.Calendar

object CallStatus {
    const val PENDING = "PENDING"
    const val CALLING = "CALLING"
    const val COMPLETED = "COMPLETED"
    const val BUSY = "BUSY"
    const val NO_ANSWER = "NO_ANSWER"
    const val REJECTED = "REJECTED"
    const val FAILED = "FAILED"
    const val SKIPPED = "SKIPPED"

    /** Owner call ke baad inme se natija chunta hai (Android SIM call ka natija khud nahi batata). */
    val RESULT_OPTIONS = listOf(COMPLETED, NO_ANSWER, BUSY, REJECTED, FAILED)

    fun label(s: String) = when (s) {
        PENDING -> "बाकी"
        CALLING -> "कॉल चल रही है"
        COMPLETED -> "बात हुई"
        BUSY -> "व्यस्त"
        NO_ANSWER -> "नो आंसर"
        REJECTED -> "रिजेक्ट"
        FAILED -> "फेल"
        SKIPPED -> "छोड़ा गया"
        else -> s
    }
}

object CampaignStatus {
    const val RUNNING = "RUNNING"
    const val PAUSED = "PAUSED"
    const val COMPLETED = "COMPLETED"
    const val STOPPED = "STOPPED"

    fun label(s: String) = when (s) {
        RUNNING -> "चालू"
        PAUSED -> "रुका हुआ"
        COMPLETED -> "पूरा हुआ"
        STOPPED -> "रोका गया"
        else -> s
    }
}

object CallingRules {
    /** TRAI ke hisaab se promotional/commercial calls ke liye safe window: 9:00 - 21:00 */
    const val START_HOUR = 9
    const val END_HOUR = 21
    /** Itne ghante tak NO_ANSWER / REJECTED customer ko dobara nahi chuna jaata. */
    const val RECENT_UNANSWERED_HOURS = 24L
    const val NO_OFFHOOK_TIMEOUT_MS = 30_000L

    fun isAllowedHour(hour: Int) = hour in START_HOUR until END_HOUR
    fun isAllowedNow(cal: Calendar = Calendar.getInstance()) = isAllowedHour(cal.get(Calendar.HOUR_OF_DAY))
}

data class CampaignSummary(
    val total: Int, val pending: Int, val calling: Int, val completed: Int,
    val noAnswer: Int, val busy: Int, val rejected: Int, val skipped: Int, val failed: Int
) {
    val done: Int get() = total - pending - calling
}

object CampaignLogic {
    /** Agla customer: sabse chhota position wala PENDING. Ek time par sirf ek call. */
    fun next(rows: List<CallHistoryEntity>): CallHistoryEntity? =
        rows.filter { it.status == CallStatus.PENDING }.minByOrNull { it.position }

    fun isFinished(rows: List<CallHistoryEntity>): Boolean =
        rows.isNotEmpty() && rows.none { it.status == CallStatus.PENDING || it.status == CallStatus.CALLING }

    fun summarize(rows: List<CallHistoryEntity>): CampaignSummary {
        fun n(s: String) = rows.count { it.status == s }
        return CampaignSummary(
            total = rows.size, pending = n(CallStatus.PENDING), calling = n(CallStatus.CALLING),
            completed = n(CallStatus.COMPLETED), noAnswer = n(CallStatus.NO_ANSWER),
            busy = n(CallStatus.BUSY), rejected = n(CallStatus.REJECTED),
            skipped = n(CallStatus.SKIPPED), failed = n(CallStatus.FAILED)
        )
    }
}

val PHONE_PERMISSIONS = arrayOf(
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.CALL_PHONE,
    Manifest.permission.READ_PHONE_NUMBERS
)

data class SimInfo(
    val hasPhoneStatePermission: Boolean = false,
    val hasCallPermission: Boolean = false,
    val hasNumberPermission: Boolean = false,
    val sim1Found: Boolean = false,
    val simReady: Boolean = false,
    val carrier: String? = null,
    val number: String? = null,
    val networkOk: Boolean = false,
    val airplane: Boolean = false,
    val accountHandle: PhoneAccountHandle? = null,
    val activeSimCount: Int = 0,
    val problem: String? = null
) {
    val canCall: Boolean
        get() = hasPhoneStatePermission && hasCallPermission && sim1Found && simReady &&
            !airplane && accountHandle != null
}
