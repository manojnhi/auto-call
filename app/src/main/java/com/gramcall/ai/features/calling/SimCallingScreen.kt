package com.gramcall.ai.features.calling

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.calling.CampaignStatus
import com.gramcall.ai.calling.PHONE_PERMISSIONS
import com.gramcall.ai.ui.*

@Composable
fun SimCallingScreen(
    vm: CallingViewModel,
    onVillageCalling: () -> Unit,
    onHistory: () -> Unit,
    onOpenCampaign: () -> Unit
) {
    val sim by vm.simInfo.collectAsStateWithLifecycle()
    val active by vm.activeCampaign.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.refreshSim()
    }
    LaunchedEffect(Unit) { vm.refreshSim() }

    fun yn(b: Boolean) = if (b) "✓ हाँ" else "✗ नहीं"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        active?.let { c ->
            InfoCard("अधूरा कॉल अभियान") {
                Text("${c.title} — ${CampaignStatus.label(c.status)}")
                Button(
                    onClick = { vm.focusActive(); onOpenCampaign() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
                ) { Text("अभियान खोलें") }
            }
        }

        InfoCard("SIM 1 की स्थिति") {
            KV("SIM 1 मिला", yn(sim.sim1Found))
            KV("SIM तैयार", yn(sim.simReady))
            KV("ऑपरेटर", sim.carrier ?: "—")
            KV("नंबर", sim.number ?: "— (अनुमति/SIM पर निर्भर)")
            KV("नेटवर्क (अनुमानित)", yn(sim.networkOk))
            KV("एयरप्लेन मोड", if (sim.airplane) "चालू" else "बंद")
            KV("कॉल अकाउंट (SIM 1)", yn(sim.accountHandle != null))
            KV("सक्रिय SIM की संख्या", sim.activeSimCount.toString())
            sim.problem?.let { Text(it, color = Danger) }
            if (sim.canCall) Text("SIM 1 से कॉल लगाने के लिए तैयार", color = Green)
            OutlinedButton(onClick = { vm.refreshSim() }) { Text("दोबारा जाँचें") }
        }

        InfoCard("अनुमतियाँ (क्यों चाहिए)") {
            KV("फ़ोन स्थिति", yn(sim.hasPhoneStatePermission))
            Text("SIM 1 पहचानने और कॉल चालू/खत्म होने का पता लगाने के लिए।", style = MaterialTheme.typography.bodySmall)
            KV("कॉल करना", yn(sim.hasCallPermission))
            Text("SIM 1 से सीधे कॉल लगाने के लिए।", style = MaterialTheme.typography.bodySmall)
            KV("फ़ोन नंबर", yn(sim.hasNumberPermission))
            Text("सिर्फ़ SIM का अपना नंबर दिखाने के लिए (वैकल्पिक)।", style = MaterialTheme.typography.bodySmall)
            if (!(sim.hasPhoneStatePermission && sim.hasCallPermission && sim.hasNumberPermission)) {
                Button(
                    onClick = { launcher.launch(PHONE_PERMISSIONS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
                ) { Text("अनुमति दें") }
            }
            Text(
                "कॉल लॉग, माइक्रोफ़ोन या SMS की अनुमति नहीं माँगी जाती।",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        InfoCard("कॉलिंग मोड") {
            Text("अभी चालू मोड: SIM 1 कॉल + मैनुअल नतीजा (आपके फ़ोन से असली कॉल)", color = Green)
            Text(
                "AI दो-तरफ़ा आवाज़: सत्यापित नहीं। सामान्य SIM कॉल में Android ऐप को सामने वाले की आवाज़ सुनने " +
                    "या आवाज़ डालने की अनुमति नहीं देता। यह फ़ेज़ 5 में असली फ़ोन पर जाँच के बाद ही चालू होगा।",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "कॉल का नतीजा (बात हुई / नो आंसर / व्यस्त...) Android खुद नहीं बताता, इसलिए हर कॉल के बाद आप चुनते हैं।",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onVillageCalling, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
        ) { Text("गाँव / शहर कॉल अभियान") }
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("कॉल हिस्ट्री") }
    }
}

@Composable
fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(k, Modifier.weight(0.5f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(v, Modifier.weight(0.5f))
    }
}
