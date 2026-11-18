package com.gramcall.ai.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.gramcall.ai.features.calling.InfoCard
import com.gramcall.ai.features.calling.KV
import com.gramcall.ai.ui.*

@Composable
fun BackupScreen(vm: BackupViewModel) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val cloud by vm.cloud.collectAsStateWithLifecycle()
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    var url by remember(cloud.url) { mutableStateOf(cloud.url) }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var shop by remember { mutableStateOf("") }
    var confirmCloudDownload by remember { mutableStateOf(false) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) vm.exportTo(uri) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) pendingRestore = uri }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        InfoCard("फ़ोन में बैकअप") {
            Text("सारा डेटा (ग्राहक, गाँव, ऑर्डर, प्रोडक्ट, कॉल हिस्ट्री, सेटिंग्स) एक फ़ाइल में। PIN इसमें नहीं जाता।", style = MaterialTheme.typography.bodySmall)
            Text("⚠ फ़ाइल में ग्राहकों के नंबर होते हैं — उसे सुरक्षित रखें।", style = MaterialTheme.typography.bodySmall, color = Danger)
            Button(
                enabled = !busy, onClick = { create.launch("gramcall-backup.json") }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
            ) { Text("बैकअप फ़ाइल बनाएँ") }
            OutlinedButton(enabled = !busy, onClick = { open.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("बैकअप से रिस्टोर करें") }
        }

        InfoCard("क्लाउड बैकअप (आपका बैकएंड सर्वर)") {
            Text("सर्वर चलाने की गाइड: docs/INSTALLATION.md। सिर्फ़ https:// URL चलेगा।", style = MaterialTheme.typography.bodySmall)
            FormField(url, { url = it }, "सर्वर URL (https://...)", KeyboardType.Uri)
            if (!cloud.loggedIn) {
                FormField(mobile, { mobile = it }, "मोबाइल", KeyboardType.Phone)
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("पासवर्ड (कम से कम 8 अक्षर)") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
                )
                FormField(shop, { shop = it }, "दुकान का नाम (सिर्फ़ रजिस्टर के लिए)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !busy, onClick = { vm.auth(url, mobile, password, shop, false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)) { Text("लॉगिन") }
                    OutlinedButton(enabled = !busy, onClick = { vm.auth(url, mobile, password, shop, true) }) { Text("रजिस्टर") }
                }
            } else {
                KV("स्थिति", "लॉगिन है")
                KV("सर्वर वर्ज़न", cloud.version.toString())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !busy, onClick = { vm.upload() },
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)) { Text("अपलोड") }
                    OutlinedButton(enabled = !busy, onClick = { confirmCloudDownload = true }) { Text("डाउनलोड/रिस्टोर") }
                }
                OutlinedButton(onClick = { vm.cloudLogout() }) { Text("क्लाउड लॉगआउट") }
            }
            Text("यह पूरा-स्नैपशॉट बैकअप है, लाइव सिंक नहीं। दो फ़ोन पर एक साथ बदलाव करने पर सर्वर टकराव (409) बताता है।", style = MaterialTheme.typography.bodySmall)
        }
    }

    pendingRestore?.let { uri ->
        ConfirmDialog("रिस्टोर करें?", "अभी का सारा डेटा बैकअप वाले डेटा से बदल जाएगा।", "रिस्टोर",
            onConfirm = { pendingRestore = null; vm.restoreFrom(uri) }, onDismiss = { pendingRestore = null })
    }
    if (confirmCloudDownload) {
        ConfirmDialog("क्लाउड से रिस्टोर?", "अभी का सारा डेटा सर्वर वाले डेटा से बदल जाएगा।", "रिस्टोर",
            onConfirm = { confirmCloudDownload = false; vm.download() }, onDismiss = { confirmCloudDownload = false })
    }
}
