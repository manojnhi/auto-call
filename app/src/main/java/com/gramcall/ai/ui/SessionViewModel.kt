package com.gramcall.ai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gramcall.ai.GramCallApp
import com.gramcall.ai.repository.Keys
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Gate { LOADING, ONBOARDING, LOCKED, OPEN }

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val biz = (app as GramCallApp).biz
    private val store = (app as GramCallApp).secure

    private val _gate = MutableStateFlow(Gate.LOADING)
    val gate: StateFlow<Gate> = _gate
    private val _pinVersion = MutableStateFlow(0)
    val pinVersion: StateFlow<Int> = _pinVersion

    init {
        viewModelScope.launch {
            delay(900)
            _gate.value = when {
                biz.setting(Keys.ONBOARDED) != "1" -> Gate.ONBOARDING
                store.hasPin() -> Gate.LOCKED
                else -> Gate.OPEN
            }
        }
    }

    fun hasPin() = store.hasPin()
    fun lockoutSeconds() = store.lockoutSecondsLeft()

    fun completeOnboarding(shopName: String, mobile: String, pin: String) {
        viewModelScope.launch {
            biz.putSettings(mapOf(Keys.ONBOARDED to "1", Keys.SHOP_NAME to shopName.trim(), Keys.SHOP_MOBILE to mobile.trim()))
            if (pin.isNotEmpty()) store.setPin(pin)
            _gate.value = Gate.OPEN
        }
    }

    fun unlock(pin: String): Boolean {
        val ok = store.verifyPin(pin)
        if (ok) _gate.value = Gate.OPEN
        return ok
    }

    /** Logout: PIN laga ho to app lock ho jaata hai, aur cloud login hat jaata hai. */
    fun logout() {
        store.clearCloudSession()
        if (store.hasPin()) _gate.value = Gate.LOCKED
    }

    fun setPin(pin: String) { store.setPin(pin); _pinVersion.value++ }
    fun changePin(old: String, new: String): Boolean {
        if (!store.verifyPin(old)) return false
        store.setPin(new); _pinVersion.value++
        return true
    }
    fun removePin(current: String): Boolean {
        if (!store.verifyPin(current)) return false
        store.clearPin(); _pinVersion.value++
        return true
    }
}
