package com.gramcall.ai.features.orders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.biz.DatePreset
import com.gramcall.ai.biz.Money
import com.gramcall.ai.biz.OrderStatus
import com.gramcall.ai.ui.*
import com.gramcall.ai.utils.Fmt

@Composable
fun OrdersScreen(vm: BizViewModel, onOpen: (Long) -> Unit, onNew: () -> Unit) {
    val q by vm.orderQuery.collectAsStateWithLifecycle()
    val status by vm.orderStatus.collectAsStateWithLifecycle()
    val preset by vm.orderPreset.collectAsStateWithLifecycle()
    val list by vm.orders.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) list?.let { vm.exportOrders(uri, it) }
    }
    val statusOptions: List<Pair<String?, String>> =
        listOf<Pair<String?, String>>(null to "सभी स्थितियाँ") + OrderStatus.ALL.map { it to OrderStatus.label(it) }
    val presetOptions: List<Pair<DatePreset?, String>> =
        listOf<Pair<DatePreset?, String>>(null to "सभी समय") +
            DatePreset.values().filter { it != DatePreset.CUSTOM }.map { it to it.label }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SearchField(q, { vm.orderQuery.value = it }, "नाम / मोबाइल / ऑर्डर नंबर") }
                IconButton(onClick = { exportLauncher.launch("orders.csv") }) { Icon(Icons.Filled.Download, "CSV एक्सपोर्ट") }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField("स्थिति", statusOptions, status, { vm.orderStatus.value = it }, Modifier.weight(1f))
                DropdownField("समय", presetOptions, preset, { vm.orderPreset.value = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            val l = list
            when {
                l == null -> LoadingBox()
                l.isEmpty() -> EmptyState("कोई ऑर्डर नहीं", "+ बटन से नया ऑर्डर बनाएँ, या कॉल अभियान / AI सहायक से ऑर्डर लें।")
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(l, key = { it.id }) { o ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(o.id) }
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("#${o.id} • ${o.customerName}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        Fmt.dateTime(o.orderAt) + listOf(o.villageName, o.cityName).filter { it.isNotBlank() }
                                            .joinToString(", ").let { if (it.isEmpty()) "" else " • $it" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Money.fmt(o.total), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        OrderStatus.label(o.status), style = MaterialTheme.typography.labelMedium,
                                        color = if (o.status == OrderStatus.CANCELLED) Danger else Green
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onNew, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Green, contentColor = Navy
        ) { Icon(Icons.Filled.Add, "नया ऑर्डर") }
    }
}
