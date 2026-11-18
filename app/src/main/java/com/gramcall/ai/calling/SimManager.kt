package com.gramcall.ai.calling

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/** SIM 1 detect karta hai aur sirf SIM 1 ke PhoneAccount se call lagata hai. */
class SimManager(private val ctx: Context) {
    private fun granted(p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun hasPhoneState() = granted(Manifest.permission.READ_PHONE_STATE)
    fun hasCallPermission() = granted(Manifest.permission.CALL_PHONE)
    fun hasNumberPermission() = granted(Manifest.permission.READ_PHONE_NUMBERS)

    @SuppressLint("MissingPermission")
    fun inspect(): SimInfo {
        val base = SimInfo(hasPhoneState(), hasCallPermission(), hasNumberPermission())
        if (!base.hasPhoneStatePermission) return base.copy(problem = "फ़ोन स्थिति की अनुमति चाहिए")
        val airplane = Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        return try {
            val sm = ctx.getSystemService(SubscriptionManager::class.java)
            val subs: List<SubscriptionInfo> = sm.activeSubscriptionInfoList.orEmpty()
            val sim1 = subs.firstOrNull { it.simSlotIndex == 0 }
                ?: return base.copy(
                    activeSimCount = subs.size, airplane = airplane,
                    problem = if (subs.isEmpty()) "कोई सक्रिय SIM नहीं मिला" else "SIM 1 (स्लॉट 1) में सक्रिय SIM नहीं है"
                )
            val subId = sim1.subscriptionId
            val tm = ctx.getSystemService(TelephonyManager::class.java).createForSubscriptionId(subId)
            val ready = tm.simState == TelephonyManager.SIM_STATE_READY
            val carrier = sim1.carrierName?.toString()?.takeIf { it.isNotBlank() }
                ?: tm.simOperatorName.takeIf { it.isNotBlank() }
            val networkOk = tm.networkOperator.isNotEmpty()
            val handle = findHandle(subId, subs.size)
            val problem = when {
                airplane -> "एयरप्लेन मोड चालू है"
                !ready -> "SIM 1 तैयार नहीं है"
                !networkOk -> "नेटवर्क नहीं मिल रहा"
                handle == null -> "SIM 1 का कॉल अकाउंट नहीं मिला (इस फ़ोन पर ऐप से कॉल नहीं लगेगी)"
                !base.hasCallPermission -> "कॉल करने की अनुमति चाहिए"
                else -> null
            }
            base.copy(
                sim1Found = true, simReady = ready, carrier = carrier,
                number = readNumber(sm, subId, sim1), networkOk = networkOk, airplane = airplane,
                accountHandle = handle, activeSimCount = subs.size, problem = problem
            )
        } catch (e: SecurityException) {
            base.copy(problem = "अनुमति नहीं मिली: ${e.message}")
        }
    }

    @SuppressLint("NewApi")
    private fun readNumber(sm: SubscriptionManager, subId: Int, info: SubscriptionInfo): String? {
        if (!hasNumberPermission()) return null
        return try {
            val n: String? = if (Build.VERSION.SDK_INT >= 33) sm.getPhoneNumber(subId) else legacyNumber(info)
            n?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyNumber(info: SubscriptionInfo): String? = info.number

    /**
     * SIM 1 ka PhoneAccountHandle. Pakka match na mile to (sirf ek SIM + ek account wale phone par) wahi account;
     * warna null - tab galat SIM se call lagane se bachne ke liye ऐप call nahi lagata.
     */
    @SuppressLint("MissingPermission", "NewApi")
    private fun findHandle(subId: Int, activeSimCount: Int): PhoneAccountHandle? {
        val telecom = ctx.getSystemService(TelecomManager::class.java)
        val accounts: List<PhoneAccountHandle> = try {
            telecom.callCapablePhoneAccounts
        } catch (e: SecurityException) {
            return null
        }
        val tm = ctx.getSystemService(TelephonyManager::class.java)
        val matched = accounts.firstOrNull { h ->
            h.id == subId.toString() || (Build.VERSION.SDK_INT >= 30 && try {
                tm.createForPhoneAccountHandle(h)?.subscriptionId == subId
            } catch (e: Exception) {
                false
            })
        }
        if (matched != null) return matched
        return if (accounts.size == 1 && activeSimCount == 1) accounts[0] else null
    }

    @SuppressLint("MissingPermission")
    fun placeCall(number: String, handle: PhoneAccountHandle): Result<Unit> = runCatching {
        val extras = Bundle().apply { putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle) }
        ctx.getSystemService(TelecomManager::class.java)
            .placeCall(Uri.fromParts("tel", number, null), extras)
    }
}
