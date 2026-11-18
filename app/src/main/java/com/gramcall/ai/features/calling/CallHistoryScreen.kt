package com.gramcall.ai.features.calling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.calling.CallStatus
import com.gramcall.ai.ui.*
import com.gramcall.ai.utils.Fmt

@Composable
fun CallHistoryScreen(vm: CallingViewModel) {
    val list by vm.history.collectAsStateWithLifecycle()
    val l = list
    when {
        l == null -> LoadingBox()
        l.isEmpty() -> EmptyState("अभी कोई कॉल हिस्ट्री नहीं", "कॉल अभियान चलाने के बाद यहाँ हर कॉल दिखेगी।")
        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(l, key = { it.id }) { r ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.customerName, style = MaterialTheme.typography.titleSmall)
                            Text(r.mobile, style = MaterialTheme.typography.bodySmall)
                            Text(
                                Fmt.dateTime(r.startedAt), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(CallStatus.label(r.status), color = statusColor(r.status), style = MaterialTheme.typography.labelLarge)
                            if (r.durationSec > 0) Text(Fmt.duration(r.durationSec), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
