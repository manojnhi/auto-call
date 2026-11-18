package com.gramcall.ai.features.customers

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
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.database.CustomerWithPlace
import com.gramcall.ai.ui.*

@Composable
fun CustomersScreen(vm: AppViewModel, onOpen: (Long) -> Unit, onAdd: () -> Unit) {
    val query by vm.customerQuery.collectAsStateWithLifecycle()
    val villageFilter by vm.customerVillageFilter.collectAsStateWithLifecycle()
    val cityFilter by vm.customerCityFilter.collectAsStateWithLifecycle()
    val list by vm.customers.collectAsStateWithLifecycle()
    val villages by vm.allVillages.collectAsStateWithLifecycle()
    val cities by vm.allCities.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCsv(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri)
    }

    val villageOptions: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to "सभी गाँव") + villages.map { it.id to it.name }
    val cityOptions: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to "सभी शहर") + cities.map { it.id to it.name }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SearchField(query, { vm.customerQuery.value = it }, "नाम या मोबाइल खोजें") }
                IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Icon(Icons.Filled.Upload, "CSV इम्पोर्ट") }
                IconButton(onClick = { exportLauncher.launch("grahak_list.csv") }) { Icon(Icons.Filled.Download, "CSV एक्सपोर्ट") }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DropdownField("गाँव", villageOptions, villageFilter, { vm.customerVillageFilter.value = it }, Modifier.weight(1f))
                DropdownField("शहर", cityOptions, cityFilter, { vm.customerCityFilter.value = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            val l = list
            when {
                l == null -> LoadingBox()
                l.isEmpty() -> EmptyState(
                    "कोई ग्राहक नहीं मिला",
                    "+ बटन से ग्राहक जोड़ें, या ऊपर के तीर से CSV इम्पोर्ट करें।\nCSV कॉलम: name, mobile, alt_mobile, village, city, address, category, language, consent, preferred_time, notes"
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(l, key = { it.customer.id }) { cw -> CustomerCard(cw) { onOpen(cw.customer.id) } }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Green, contentColor = Navy
        ) { Icon(Icons.Filled.Add, "ग्राहक जोड़ें") }
    }
}

@Composable
private fun CustomerCard(cw: CustomerWithPlace, onClick: () -> Unit) {
    val c = cw.customer
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Text(c.mobile, style = MaterialTheme.typography.bodyMedium)
                val place = listOfNotNull(cw.villageName, cw.cityName).joinToString(" • ")
                if (place.isNotEmpty()) {
                    Text(place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                if (c.callConsent) "सहमति ✓" else "सहमति नहीं",
                style = MaterialTheme.typography.labelMedium,
                color = if (c.callConsent) Green else Danger
            )
        }
    }
}
