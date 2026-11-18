package com.gramcall.ai.features.calling

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.calling.*
import com.gramcall.ai.database.CallHistoryEntity
import com.gramcall.ai.ui.*
import com.gramcall.ai.utils.Fmt

@Composable
fun CampaignScreen(vm: CallingViewModel, onClose: () -> Unit, onTakeOrder: (Long, Long) -> Unit) {
    val camp by vm.campaign.collectAsStateWithLifecycle()
    val rows by vm.rows.collectAsStateWithLifecycle()
    val sim by vm.simInfo.collectAsStateWithLifecycle()
    val orderCount by vm.orderCount.collectAsStateWithLifecycle()
    var confirmStop by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.refreshSim()
    }
    LaunchedEffect(Unit) { vm.refreshSim() }

    val c = camp
    if (c == null) {
        EmptyState("कोई कॉल अभियान नहीं चल रहा", "SIM कॉलिंग > गाँव / शहर कॉल अभियान से नया अभियान शुरू करें।")
        return
    }

    val summary = CampaignLogic.summarize(rows)
    val calling = rows.firstOrNull { it.status == CallStatus.CALLING }
    val awaiting = calling != null && calling.endedAt != null
    val next = CampaignLogic.next(rows)
    val finished = c.status == CampaignStatus.COMPLETED || c.status == CampaignStatus.STOPPED
    val busy = calling != null

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard(c.title) {
                Text("स्थिति: ${CampaignStatus.label(c.status)}", color = if (finished) MaterialTheme.colorScheme.onSurfaceVariant else Green)
                LinearProgressIndicator(
                    progress = { if (summary.total == 0) 0f else summary.done.toFloat() / summary.total },
                    modifier = Modifier.fillMaxWidth(), color = Green
                )
                Text("${summary.done} / ${summary.total} ग्राहक हो चुके")
            }
        }

        if (!finished) {
            item {
                InfoCard("अगला कदम") {
                    when {
                        calling != null && !awaiting -> {
                            Text("कॉल चल रही है: ${calling.customerName} (${calling.mobile})")
                            Text("कॉल खत्म होने पर नतीजा पूछा जाएगा।", style = MaterialTheme.typography.bodySmall)
                        }
                        c.status == CampaignStatus.PAUSED ->
                            Text("अभियान रुका हुआ है। आगे बढ़ने के लिए “जारी रखें” दबाएँ।")
                        next != null -> {
                            Text("अगला ग्राहक: ${next.customerName}")
                            Text(next.mobile, style = MaterialTheme.typography.bodyMedium)
                            if (!sim.canCall && sim.problem != null) Text(sim.problem!!, color = Danger)
                            Button(
                                onClick = {
                                    if (vm.hasPhonePermissions()) vm.dialNext() else launcher.launch(PHONE_PERMISSIONS)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
                            ) { Text("कॉल लगाएँ (SIM 1)") }
                        }
                        else -> Text("कोई ग्राहक बाकी नहीं।")
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (c.status == CampaignStatus.PAUSED) {
                        OutlinedButton(onClick = { vm.resume() }, modifier = Modifier.weight(1f)) { Text("जारी रखें") }
                    } else {
                        OutlinedButton(onClick = { vm.pause() }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("रोकें") }
                    }
                    OutlinedButton(onClick = { vm.skipNext() }, enabled = !busy && next != null, modifier = Modifier.weight(1f)) { Text("छोड़ें") }
                    OutlinedButton(onClick = { confirmStop = true }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text("बंद", color = Danger)
                    }
                }
            }
        }

        item {
            InfoCard(if (finished) "अभियान सारांश" else "अब तक का सारांश") {
                KV("कुल ग्राहक", summary.total.toString())
                KV("कॉल पूरी (बात हुई)", summary.completed.toString())
                KV("ऑर्डर मिले", orderCount.toString())
                KV("नो आंसर", summary.noAnswer.toString())
                KV("व्यस्त", summary.busy.toString())
                KV("रिजेक्ट", summary.rejected.toString())
                KV("छोड़े गए", summary.skipped.toString())
                KV("फेल", summary.failed.toString())
                if (finished) {
                    Button(
                        onClick = { vm.dismissCampaign(); onClose() }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
                    ) { Text("बंद करें") }
                }
            }
        }

        items(rows, key = { it.id }) { RowItem(it) }
    }

    if (awaiting && calling != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("कॉल का नतीजा चुनें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${calling.customerName} • ${calling.mobile}")
                    Text("कॉल का समय (रिंग समेत): ${Fmt.duration(calling.durationSec)}", style = MaterialTheme.typography.bodySmall)
                    if (calling.durationSec == 0) {
                        Text("कॉल कनेक्ट होने का संकेत नहीं मिला।", color = Danger, style = MaterialTheme.typography.bodySmall)
                    }
                    if (calling.customerId != null) {
                        Button(
                            onClick = { vm.setResult(calling, CallStatus.COMPLETED); onTakeOrder(calling.customerId, calling.campaignId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
                        ) { Text("बात हुई + ऑर्डर लें") }
                    }
                    CallStatus.RESULT_OPTIONS.forEach { st ->
                        OutlinedButton(onClick = { vm.setResult(calling, st) }, modifier = Modifier.fillMaxWidth()) {
                            Text(CallStatus.label(st))
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (confirmStop) {
        ConfirmDialog(
            title = "अभियान बंद करें?",
            text = "बाकी सभी ग्राहक “छोड़े गए” हो जाएँगे।",
            confirmLabel = "बंद करें",
            onConfirm = { confirmStop = false; vm.stop() },
            onDismiss = { confirmStop = false }
        )
    }
}

@Composable
fun statusColor(s: String): Color = when (s) {
    CallStatus.COMPLETED -> Green
    CallStatus.FAILED, CallStatus.REJECTED -> Danger
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun RowItem(r: CallHistoryEntity) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${r.position + 1}. ${r.customerName}")
                Text(r.mobile, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (r.note.isNotEmpty()) Text(r.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(CallStatus.label(r.status), color = statusColor(r.status), style = MaterialTheme.typography.labelLarge)
                if (r.durationSec > 0) Text(Fmt.duration(r.durationSec), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
