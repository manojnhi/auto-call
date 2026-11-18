package com.gramcall.ai.features.cities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.database.AreaEntity
import com.gramcall.ai.database.CityEntity
import com.gramcall.ai.database.CityWithCount
import com.gramcall.ai.ui.*

@Composable
fun CitiesScreen(vm: AppViewModel, onShowCustomers: (Long) -> Unit) {
    val query by vm.cityQuery.collectAsStateWithLifecycle()
    val list by vm.cities.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CityEntity?>(null) }
    var deleting by remember { mutableStateOf<CityEntity?>(null) }
    var areasFor by remember { mutableStateOf<CityEntity?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SearchField(query, { vm.cityQuery.value = it }, "शहर खोजें")
            val l = list
            when {
                l == null -> LoadingBox()
                l.isEmpty() -> EmptyState(
                    if (query.isBlank()) "अभी कोई शहर नहीं जुड़ा" else "कोई शहर नहीं मिला",
                    if (query.isBlank()) "नीचे + बटन दबाकर पहला शहर जोड़ें" else ""
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(l, key = { it.city.id }) { cc ->
                        CityCard(
                            cc,
                            onClick = { onShowCustomers(cc.city.id) },
                            onAreas = { areasFor = cc.city },
                            onEdit = { editing = cc.city; showDialog = true },
                            onDelete = { deleting = cc.city }
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = null; showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Green, contentColor = Navy
        ) { Icon(Icons.Filled.Add, "शहर जोड़ें") }
    }

    if (showDialog) {
        CityDialog(
            initial = editing,
            onSave = { c -> vm.saveCity(c) { if (it.ok) showDialog = false } },
            onDismiss = { showDialog = false }
        )
    }
    deleting?.let { c ->
        ConfirmDialog(
            title = "शहर हटाएँ?",
            text = "\"${c.name}\" और उसके इलाके हटाए जाएँगे। इस शहर के ग्राहक सूची में रहेंगे, पर उनका शहर खाली हो जाएगा।",
            onConfirm = { vm.deleteCity(c); deleting = null },
            onDismiss = { deleting = null }
        )
    }
    areasFor?.let { c -> AreasDialog(vm, c, onDismiss = { areasFor = null }) }
}

@Composable
private fun CityCard(
    cc: CityWithCount, onClick: () -> Unit, onAreas: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val c = cc.city
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                val place = listOf(c.district, c.state).filter { it.isNotBlank() }.joinToString(", ")
                if (place.isNotEmpty()) {
                    Text(place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text("ग्राहक: ${cc.customerCount}", style = MaterialTheme.typography.bodyMedium, color = Green)
            }
            IconButton(onClick = onAreas) { Icon(Icons.Filled.Map, "इलाके") }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "संपादित करें") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "हटाएँ", tint = Danger) }
        }
    }
}

@Composable
private fun CityDialog(initial: CityEntity?, onSave: (CityEntity) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var district by remember { mutableStateOf(initial?.district ?: "") }
    var state by remember { mutableStateOf(initial?.state ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "नया शहर" else "शहर संपादित करें") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(name, { name = it }, "शहर का नाम *")
                FormField(district, { district = it }, "ज़िला")
                FormField(state, { state = it }, "राज्य")
                FormField(notes, { notes = it }, "नोट्स", singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    CityEntity(
                        id = initial?.id ?: 0, name = name, district = district, state = state, notes = notes,
                        createdAt = initial?.createdAt ?: System.currentTimeMillis()
                    )
                )
            }) { Text("सेव करें") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("रद्द करें") } }
    )
}

@Composable
private fun AreasDialog(vm: AppViewModel, city: CityEntity, onDismiss: () -> Unit) {
    val areas by remember(city.id) { vm.areas(city.id) }.collectAsStateWithLifecycle(initialValue = emptyList<AreaEntity>())
    var newArea by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${city.name} के इलाके") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newArea, onValueChange = { newArea = it }, singleLine = true,
                        label = { Text("नया इलाका / मोहल्ला") }, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.addArea(city.id, newArea) { if (it.ok) newArea = "" } }) {
                        Icon(Icons.Filled.Add, "जोड़ें", tint = Green)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (areas.isEmpty()) {
                    Text("अभी कोई इलाका नहीं", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                areas.forEach { a ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(a.name, Modifier.weight(1f))
                        IconButton(onClick = { vm.deleteArea(a) }) { Icon(Icons.Filled.Close, "हटाएँ") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद करें") } }
    )
}
