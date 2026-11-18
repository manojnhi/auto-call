package com.gramcall.ai.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.biz.Money
import com.gramcall.ai.ui.AppViewModel
import com.gramcall.ai.ui.BizViewModel
import com.gramcall.ai.ui.Green

private data class Stat(val label: String, val value: String, val icon: ImageVector, val live: Boolean)

@Composable
fun DashboardScreen(vm: AppViewModel, biz: BizViewModel) {
    val customers by vm.customerCount.collectAsStateWithLifecycle()
    val villages by vm.villageCount.collectAsStateWithLifecycle()
    val cities by vm.cityCount.collectAsStateWithLifecycle()
    val callsToday by vm.callsToday.collectAsStateWithLifecycle()
    val successToday by vm.successToday.collectAsStateWithLifecycle()
    val noAnswerToday by vm.noAnswerToday.collectAsStateWithLifecycle()
    val ordersToday by biz.ordersToday.collectAsStateWithLifecycle()
    val salesToday by biz.salesToday.collectAsStateWithLifecycle()
    val openOrders by biz.openOrders.collectAsStateWithLifecycle()
    val delivered by biz.deliveredOrders.collectAsStateWithLifecycle()
    val low by biz.lowStockCount.collectAsStateWithLifecycle()

    fun v(n: Int?) = n?.toString() ?: "…"
    val cards = listOf(
        Stat("कुल ग्राहक", v(customers), Icons.Filled.People, true),
        Stat("कुल गाँव", v(villages), Icons.Filled.Place, true),
        Stat("कुल शहर", v(cities), Icons.Filled.LocationCity, true),
        Stat("आज की कॉल", v(callsToday), Icons.Filled.Call, true),
        Stat("सफल कॉल", v(successToday), Icons.Filled.CheckCircle, true),
        Stat("नो आंसर कॉल", v(noAnswerToday), Icons.Filled.PhoneMissed, true),
        Stat("आज के ऑर्डर", v(ordersToday), Icons.Filled.ShoppingCart, true),
        Stat("आज की बिक्री", salesToday?.let { Money.fmt(it) } ?: "…", Icons.Filled.CurrencyRupee, true),
        Stat("पेंडिंग ऑर्डर", v(openOrders), Icons.Filled.HourglassTop, true),
        Stat("पूरे हुए ऑर्डर", v(delivered), Icons.Filled.TaskAlt, true)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Text("डैशबोर्ड", style = MaterialTheme.typography.headlineSmall)
                if (low > 0) {
                    Text("⚠ $low प्रोडक्ट का स्टॉक कम है", style = MaterialTheme.typography.bodySmall, color = com.gramcall.ai.ui.Danger)
                }
            }
        }
        items(cards) { StatCard(it) }
    }
}

@Composable
private fun StatCard(s: Stat) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Icon(
                s.icon, null,
                tint = if (s.live) Green else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))
            Text(s.value, style = MaterialTheme.typography.headlineMedium)
            Text(
                s.label, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
