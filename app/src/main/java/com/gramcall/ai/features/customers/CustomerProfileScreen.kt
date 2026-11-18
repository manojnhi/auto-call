package com.gramcall.ai.features.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.biz.Money
import com.gramcall.ai.biz.OrderStatus
import com.gramcall.ai.calling.CallStatus
import com.gramcall.ai.database.CustomerWithPlace
import com.gramcall.ai.utils.Fmt
import com.gramcall.ai.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface ProfileState {
    data object Loading : ProfileState
    data object Missing : ProfileState
    data class Found(val data: CustomerWithPlace) : ProfileState
}

private fun fmt(ts: Long?): String =
    if (ts == null) "—" else SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN")).format(Date(ts))

@Composable
fun CustomerProfileScreen(
    vm: AppViewModel, bvm: BizViewModel, id: Long,
    onEdit: () -> Unit, onDeleted: () -> Unit, onNewOrder: () -> Unit, onOpenOrder: (Long) -> Unit
) {
    val state by produceState<ProfileState>(ProfileState.Loading, id) {
        vm.customer(id).collect { value = if (it == null) ProfileState.Missing else ProfileState.Found(it) }
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val orders by remember(id) { bvm.customerOrders(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val calls by remember(id) { vm.callsForCustomer(id) }.collectAsStateWithLifecycle(initialValue = emptyList())

    when (val s = state) {
        ProfileState.Loading -> LoadingBox()
        ProfileState.Missing -> EmptyState("ग्राहक नहीं मिला")
        is ProfileState.Found -> {
            val c = s.data.customer
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(c.name, style = MaterialTheme.typography.headlineSmall)
                        Row2("मोबाइल", c.mobile)
                        if (c.altMobile.isNotEmpty()) Row2("दूसरा मोबाइल", c.altMobile)
                        Row2("गाँव", s.data.villageName ?: "—")
                        Row2("शहर", s.data.cityName ?: "—")
                        Row2("पता", c.address.ifEmpty { "—" })
                        Row2("श्रेणी", c.category)
                        Row2("भाषा", c.language)
                        Row2("कॉल की सहमति", if (c.callConsent) "हाँ (${fmt(c.consentDate)})" else "नहीं")
                        Row2("पसंदीदा समय", c.preferredTime.ifEmpty { "—" })
                        Row2("नोट्स", c.notes.ifEmpty { "—" })
                        Row2("रजिस्ट्रेशन", fmt(c.createdAt))
                    }
                }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("कॉल इतिहास", style = MaterialTheme.typography.titleMedium)
                        if (calls.isEmpty()) {
                            Text("अभी कोई कॉल नहीं", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        calls.take(10).forEach { h ->
                            Text(
                                "${Fmt.dateTime(h.startedAt)} • ${CallStatus.label(h.status)}" +
                                    if (h.durationSec > 0) " • ${Fmt.duration(h.durationSec)}" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                val live = orders.filter { it.status != OrderStatus.CANCELLED }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ऑर्डर इतिहास", style = MaterialTheme.typography.titleMedium)
                        Row2("कुल ऑर्डर", live.size.toString())
                        Row2("कुल खरीद", Money.fmt(live.sumOf { it.total }))
                        Row2("आख़िरी ऑर्डर", fmt(live.maxOfOrNull { it.orderAt }))
                        orders.take(10).forEach { o ->
                            TextButton(onClick = { onOpenOrder(o.id) }) {
                                Text("#${o.id} • ${fmt(o.orderAt)} • ${Money.fmt(o.total)} • ${OrderStatus.label(o.status)}")
                            }
                        }
                    }
                }
                Button(
                    onClick = onNewOrder, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
                ) { Text("नया ऑर्डर बनाएँ") }
                if (c.callConsent) {
                    OutlinedButton(onClick = { vm.optOut(c) }, modifier = Modifier.fillMaxWidth()) {
                        Text("कॉल बंद करें (opt-out)")
                    }
                }
                OutlinedButton(
                    onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("संपादित करें") }
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("ग्राहक हटाएँ", color = Danger)
                }
            }
            if (confirmDelete) {
                ConfirmDialog(
                    title = "ग्राहक हटाएँ?",
                    text = "\"${c.name}\" की जानकारी हमेशा के लिए हट जाएगी।",
                    onConfirm = {
                        confirmDelete = false
                        vm.deleteCustomer(c) { if (it.ok) onDeleted() }
                    },
                    onDismiss = { confirmDelete = false }
                )
            }
        }
    }
}

@Composable
private fun Row2(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(0.4f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(0.6f))
    }
}
