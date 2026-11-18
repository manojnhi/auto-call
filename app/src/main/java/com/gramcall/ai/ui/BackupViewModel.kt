package com.gramcall.ai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gramcall.ai.GramCallApp
import com.gramcall.ai.network.CloudClient
import com.gramcall.ai.repository.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CloudUi(val url: String, val loggedIn: Boolean, val version: Int, val busy: Boolean = false)

class BackupViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as GramCallApp).database
    private val store = (app as GramCallApp).secure
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _cloud = MutableStateFlow(CloudUi(store.serverUrl, store.token.isNotEmpty(), store.cloudVersion))
    val cloud: StateFlow<CloudUi> = _cloud

    private fun refreshCloud() { _cloud.value = CloudUi(store.serverUrl, store.token.isNotEmpty(), store.cloudVersion) }

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try { block() } catch (e: Exception) { messages.tryEmit("त्रुटि: ${e.message}") } finally { _busy.value = false }
        }
    }

    fun exportTo(uri: Uri) = launchBusy {
        val json = BackupManager.export(db)
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("फ़ाइल नहीं खुल सकी")
        }
        messages.tryEmit("बैकअप फ़ाइल बन गई")
    }

    fun restoreFrom(uri: Uri) = launchBusy {
        val json = withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw IllegalStateException("फ़ाइल नहीं खुल सकी")
        }
        BackupManager.restore(db, json)
        messages.tryEmit("रिस्टोर पूरा हुआ")
    }

    private fun serverError(code: Int, body: JSONObject?) =
        "सर्वर त्रुटि ($code): ${body?.optString("error").orEmpty()}"

    fun auth(url: String, mobile: String, password: String, shopName: String, register: Boolean) = launchBusy {
        val base = url.trim().trimEnd('/')
        if (!base.startsWith("https://")) { messages.tryEmit("सर्वर URL https:// से शुरू होना चाहिए"); return@launchBusy }
        val body = JSONObject().put("mobile", mobile.trim()).put("password", password)
        if (register) body.put("shopName", shopName.trim())
        val r = withContext(Dispatchers.IO) {
            CloudClient.call("POST", "$base/api/auth/" + if (register) "register" else "login", null, body)
        }
        val token = r.body?.optString("token").orEmpty()
        if (r.code in 200..299 && token.isNotEmpty()) {
            store.serverUrl = base; store.token = token; store.cloudVersion = r.body?.optInt("version", 0) ?: 0
            refreshCloud()
            messages.tryEmit(if (register) "रजिस्टर हो गया" else "लॉगिन हो गया")
        } else messages.tryEmit(serverError(r.code, r.body))
    }

    fun cloudLogout() { store.clearCloudSession(); refreshCloud() }

    fun upload() = launchBusy {
        val payload = BackupManager.export(db)
        val body = JSONObject().put("baseVersion", store.cloudVersion).put("payload", payload)
        val r = withContext(Dispatchers.IO) { CloudClient.call("PUT", store.serverUrl + "/api/backup", store.token, body) }
        when {
            r.code in 200..299 -> { store.cloudVersion = r.body?.optInt("version", 0) ?: 0; refreshCloud(); messages.tryEmit("क्लाउड पर बैकअप हो गया") }
            r.code == 409 -> messages.tryEmit("सर्वर पर नया बैकअप है (किसी और फ़ोन से) — पहले डाउनलोड करें")
            else -> messages.tryEmit(serverError(r.code, r.body))
        }
    }

    fun download() = launchBusy {
        val r = withContext(Dispatchers.IO) { CloudClient.call("GET", store.serverUrl + "/api/backup", store.token, null) }
        when {
            r.code == 404 -> messages.tryEmit("सर्वर पर अभी कोई बैकअप नहीं है")
            r.code in 200..299 && r.body != null -> {
                BackupManager.restore(db, r.body.getString("payload"))
                store.cloudVersion = r.body.optInt("version", 0); refreshCloud()
                messages.tryEmit("क्लाउड से रिस्टोर हो गया")
            }
            else -> messages.tryEmit(serverError(r.code, r.body))
        }
    }
}
