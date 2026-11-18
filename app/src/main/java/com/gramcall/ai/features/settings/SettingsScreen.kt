package com.gramcall.ai.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.features.ai.AI_LANGUAGES
import com.gramcall.ai.features.calling.InfoCard
import com.gramcall.ai.features.calling.KV
import com.gramcall.ai.repository.Keys
import com.gramcall.ai.ui.*
import com.gramcall.ai.utils.Fmt

@Composable
fun SettingsScreen(
    vm: BizViewModel, appVm: AppViewModel, session: SessionViewModel,
    onBackup: () -> Unit, onSim: () -> Unit
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val consent by appVm.consentLog.collectAsStateWithLifecycle()
    val pinVersion by session.pinVersion.collectAsStateWithLifecycle()
    var name by remember(s) { mutableStateOf(s[Keys.SHOP_NAME] ?: "") }
    var address by remember(s) { mutableStateOf(s[Keys.SHOP_ADDRESS] ?: "") }
    var mobile by remember(s) { mutableStateOf(s[Keys.SHOP_MOBILE] ?: "") }
    var lang by remember(s) { mutableStateOf(s[Keys.LANGUAGE] ?: AI_LANGUAGES[0]) }
    val hasPin = remember(pinVersion) { session.hasPin() }
    var pinDialog by remember { mutableStateOf<String?>(null) } // "set" | "change" | "remove"
    var confirmLogout by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoCard("दुकान की जानकारी") {
            FormField(name, { name = it }, "दुकान का नाम")
            FormField(address, { address = it }, "पता", singleLine = false)
            FormField(mobile, { mobile = it }, "मोबाइल", KeyboardType.Phone)
            DropdownField("डिफ़ॉल्ट कॉलिंग भाषा", AI_LANGUAGES.map { it to it }, lang, { lang = it })
            Button(
                onClick = { vm.saveSettings(mapOf(Keys.SHOP_NAME to name, Keys.SHOP_ADDRESS to address, Keys.SHOP_MOBILE to mobile, Keys.LANGUAGE to lang)) },
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
            ) { Text("सेव करें") }
        }

        InfoCard("SIM") {
            Text("कॉल हमेशा SIM 1 से लगती है।", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onSim) { Text("SIM की स्थिति देखें") }
        }

        InfoCard("ऐप लॉक (PIN)") {
            Text(if (hasPin) "PIN चालू है" else "PIN बंद है", color = if (hasPin) Green else MaterialTheme.colorScheme.onSurfaceVariant)
            if (!hasPin) {
                Button(onClick = { pinDialog = "set" }) { Text("PIN सेट करें") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pinDialog = "change" }) { Text("PIN बदलें") }
                    OutlinedButton(onClick = { pinDialog = "remove" }) { Text("PIN हटाएँ") }
                }
                OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) { Text("लॉग आउट / ऐप लॉक करें") }
            }
        }

        InfoCard("बैकअप और डेटा") {
            Text("पूरे डेटा का बैकअप / रिस्टोर, और क्लाउड बैकअप।", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) { Text("बैकअप स्क्रीन खोलें") }
            Text("ग्राहक व ऑर्डर का CSV एक्सपोर्ट उन्हीं स्क्रीन पर है।", style = MaterialTheme.typography.bodySmall)
        }

        InfoCard("गोपनीयता और सहमति") {
            Text("• सिर्फ़ सहमति वाले ग्राहकों को कॉल होती है; सहमति हटाने (opt-out) पर कॉल बंद।", style = MaterialTheme.typography.bodySmall)
            Text("• कॉल रिकॉर्डिंग नहीं होती; माइक्रोफ़ोन की अनुमति नहीं माँगी जाती।", style = MaterialTheme.typography.bodySmall)
            Text("• ग्राहक नंबर DND/NCPR सूची से मिलाना आपकी ज़िम्मेदारी है — ऐप यह जाँच नहीं करता।", style = MaterialTheme.typography.bodySmall)
            Text("हाल के सहमति रिकॉर्ड", style = MaterialTheme.typography.titleSmall)
            if (consent.isEmpty()) Text("अभी कोई रिकॉर्ड नहीं", style = MaterialTheme.typography.bodySmall)
            consent.take(8).forEach {
                Text("${Fmt.dateTime(it.at)} • ${it.customerName} • ${if (it.granted) "सहमति दी" else "सहमति हटी"} (${it.source})", style = MaterialTheme.typography.bodySmall)
            }
        }

        InfoCard("ऐप के बारे में") {
            KV("नाम", "GramCall AI")
            KV("वर्ज़न", "0.1.0")
            Text("पहला रिलीज़: एक दुकान-मालिक अकाउंट।", style = MaterialTheme.typography.bodySmall)
        }
    }

    pinDialog?.let { mode -> PinDialog(mode, session) { pinDialog = null } }
    if (confirmLogout) {
        ConfirmDialog("लॉग आउट?", "ऐप लॉक हो जाएगा और क्लाउड लॉगिन हट जाएगा।", "लॉग आउट",
            onConfirm = { confirmLogout = false; session.logout() }, onDismiss = { confirmLogout = false })
    }
}

@Composable
private fun PinDialog(mode: String, session: SessionViewModel, onClose: () -> Unit) {
    var old by remember { mutableStateOf("") }
    var new1 by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val needOld = mode != "set"
    val needNew = mode != "remove"

    @Composable
    fun pinField(v: String, label: String, set: (String) -> Unit) = OutlinedTextField(
        value = v, onValueChange = { set(it.filter(Char::isDigit).take(6)) }, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(when (mode) { "set" -> "PIN सेट करें"; "change" -> "PIN बदलें"; else -> "PIN हटाएँ" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needOld) pinField(old, "मौजूदा PIN") { old = it }
                if (needNew) {
                    pinField(new1, "नया PIN (4-6 अंक)") { new1 = it }
                    pinField(new2, "नया PIN दोबारा") { new2 = it }
                }
                error?.let { Text(it, color = Danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (needNew && (new1.length < 4 || new1 != new2)) { error = "PIN 4-6 अंक का हो और दोनों बार एक जैसा"; return@TextButton }
                val ok = when (mode) {
                    "set" -> { session.setPin(new1); true }
                    "change" -> session.changePin(old, new1)
                    else -> session.removePin(old)
                }
                if (ok) onClose() else error = "मौजूदा PIN गलत है (या कुछ देर रुकें)"
            }) { Text("ठीक है") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("रद्द करें") } }
    )
}
