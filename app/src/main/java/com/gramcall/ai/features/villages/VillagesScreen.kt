package com.gramcall.ai.features.villages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.database.VillageEntity
import com.gramcall.ai.database.VillageWithCount
import com.gramcall.ai.ui.*

@Composable
fun VillagesScreen(vm: AppViewModel, onShowCustomers: (Long) -> Unit) {
    val query by vm.villageQuery.collectAsStateWithLifecycle()
    val list by vm.villages.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VillageEntity?>(null) }
    var deleting by remember { mutableStateOf<VillageEntity?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SearchField(query, { vm.villageQuery.value = it }, "गाँव खोजें")
            val l = list
            when {
                l == null -> LoadingBox()
                l.isEmpty() -> EmptyState(
                    if (query.isBlank()) "अभी कोई गाँव नहीं जुड़ा" else "कोई गाँव नहीं मिला",
                    if (query.isBlank()) "नीचे + बटन दबाकर पहला गाँव जोड़ें" else ""
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(l, key = { it.village.id }) { vc ->
                        VillageCard(
                            vc,
                            onClick = { onShowCustomers(vc.village.id) },
                            onEdit = { editing = vc.village; showDialog = true },
                            onDelete = { deleting = vc.village }
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = null; showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Green, contentColor = Navy
        ) { Icon(Icons.Filled.Add, "गाँव जोड़ें") }
    }

    if (showDialog) {
        VillageDialog(
            initial = editing,
            onSave = { v -> vm.saveVillage(v) { if (it.ok) showDialog = false } },
            onDismiss = { showDialog = false }
        )
    }
    deleting?.let { v ->
        ConfirmDialog(
            title = "गाँव हटाएँ?",
            text = "\"${v.name}\" हटाया जाएगा। इस गाँव के ग्राहक सूची में रहेंगे, पर उनका गाँव खाली हो जाएगा।",
            onConfirm = { vm.deleteVillage(v); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun VillageCard(vc: VillageWithCount, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val v = vc.village
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(v.name, style = MaterialTheme.typography.titleMedium)
                val place = listOf(v.district, v.state, v.pinCode).filter { it.isNotBlank() }.joinToString(", ")
                if (place.isNotEmpty()) {
                    Text(place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text("ग्राहक: ${vc.customerCount}", style = MaterialTheme.typography.bodyMedium, color = Green)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "संपादित करें") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "हटाएँ", tint = Danger) }
        }
    }
}

@Composable
private fun VillageDialog(initial: VillageEntity?, onSave: (VillageEntity) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var district by remember { mutableStateOf(initial?.district ?: "") }
    var state by remember { mutableStateOf(initial?.state ?: "") }
    var pin by remember { mutableStateOf(initial?.pinCode ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "नया गाँव" else "गाँव संपादित करें") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(name, { name = it }, "गाँव का नाम *")
                FormField(district, { district = it }, "ज़िला")
                FormField(state, { state = it }, "राज्य")
                FormField(pin, { pin = it.filter(Char::isDigit).take(6) }, "PIN कोड", KeyboardType.Number)
                FormField(notes, { notes = it }, "नोट्स", singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    VillageEntity(
                        id = initial?.id ?: 0, name = name, district = district, state = state,
                        pinCode = pin, notes = notes,
                        createdAt = initial?.createdAt ?: System.currentTimeMillis()
                    )
                )
            }) { Text("सेव करें") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("रद्द करें") } }
    )
}
