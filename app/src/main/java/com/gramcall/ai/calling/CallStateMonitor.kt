package com.gramcall.ai.calling

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi

/** Android ke supported call states (IDLE / RINGING / OFFHOOK) sunta hai. READ_PHONE_STATE chahiye. */
class CallStateMonitor(private val ctx: Context) {
    private val tm: TelephonyManager = ctx.getSystemService(TelephonyManager::class.java)
    private var handle: Any? = null

    fun start(onState: (Int) -> Unit) {
        stop()
        handle = if (Build.VERSION.SDK_INT >= 31) registerNew(onState) else registerLegacy(onState)
    }

    @RequiresApi(31)
    private fun registerNew(onState: (Int) -> Unit): Any {
        val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) = onState(state)
        }
        tm.registerTelephonyCallback(ctx.mainExecutor, cb)
        return cb
    }

    @Suppress("DEPRECATION")
    private fun registerLegacy(onState: (Int) -> Unit): Any {
        val l = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) = onState(state)
        }
        tm.listen(l, PhoneStateListener.LISTEN_CALL_STATE)
        return l
    }

    @Suppress("DEPRECATION")
    fun stop() {
        val h = handle ?: return
        handle = null
        if (Build.VERSION.SDK_INT >= 31) {
            if (h is TelephonyCallback) tm.unregisterTelephonyCallback(h)
        } else if (h is PhoneStateListener) {
            tm.listen(h, PhoneStateListener.LISTEN_NONE)
        }
    }
}
