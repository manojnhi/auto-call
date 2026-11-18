package com.gramcall.ai.features.calling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.calling.CallingRules
import com.gramcall.ai.repository.EligibilityPreview
import com.gramcall.ai.ui.*

@Composable
fun VillageCallingScreen(
    appVm: AppViewModel,
    vm: CallingViewModel,
    onStarted: () -> Unit,
    onOpenActive: () -> Unit
) {
    val villages by appVm.allVillages.collectAsStateWithLifecycle()
    val cities by appVm.allCities.collectAsStateWithLifecycle()
    val active by vm.activeCampaign.collectAsStateWithLifecycle()
    var villageId by remember { mutableStateOf<Long?>(null) }
    var cityId by remember { mutableStateOf<Long?>(null) }
    var preview by remember { mutableStateOf<EligibilityPreview?>(null) }

    LaunchedEffect(villageId, cityId, active?.id) {
        preview = if (villageId == null && cityId == null) null else vm.preview(villageId, cityId)
    }

    val villageOptions: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to "— चुनें —") + villages.map { it.id to it.name }
    val cityOptions: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to "— चुनें —") + cities.map { it.id to it.name }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownField("गाँव", villageOptions, villageId, { villageId = it; if (it != null) cityId = null })
        DropdownField("शहर", cityOptions, cityId, { cityId = it; if (it != null) villageId = null })

        val p = preview
        if (p != null) {
            InfoCard("कॉल सूची का पूर्वावलोकन") {
                KV("कुल ग्राहक", p.totalCustomers.toString())
                KV("सहमति नहीं (छोड़े जाएँगे)", p.noConsent.toString())
                KV("पिछले ${CallingRules.RECENT_UNANSWERED_HOURS} घंटे में नो-आंसर/रिजेक्ट (छोड़े जाएँगे)", p.recentlyUnanswered.toString())
                KV("कॉल किए जाएँगे", p.eligible.size.toString())
                p.eligible.take(8).forEach { Text("• ${it.name} (${it.mobile})", style = MaterialTheme.typography.bodySmall) }
                if (p.eligible.size > 8) Text("… और ${p.eligible.size - 8}", style = MaterialTheme.typography.bodySmall)
            }
        }

        InfoCard("नियम") {
            Text("• एक समय में सिर्फ़ एक कॉल, हर कॉल आपके एक टैप से", style = MaterialTheme.typography.bodySmall)
            Text("• कॉल का समय: सुबह ${CallingRules.START_HOUR} से रात ${CallingRules.END_HOUR - 12} बजे", style = MaterialTheme.typography.bodySmall)
            Text("• सिर्फ़ सहमति वाले ग्राहकों को कॉल", style = MaterialTheme.typography.bodySmall)
            Text("• एक अभियान में हर ग्राहक को एक ही बार", style = MaterialTheme.typography.bodySmall)
        }

        if (active != null) {
            Text("एक अभियान पहले से चालू है — नया शुरू करने से पहले उसे पूरा या बंद करें।", color = Danger)
            OutlinedButton(onClick = { vm.focusActive(); onOpenActive() }, modifier = Modifier.fillMaxWidth()) {
                Text("चालू अभियान खोलें")
            }
        } else {
            Button(
                enabled = p != null && p.eligible.isNotEmpty(),
                onClick = { vm.startCampaign(villageId, cityId, onStarted) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
            ) { Text("START VILLAGE CALL") }
        }
    }
}
