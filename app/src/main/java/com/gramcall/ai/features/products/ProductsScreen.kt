package com.gramcall.ai.features.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.gramcall.ai.biz.Categories
import com.gramcall.ai.biz.Money
import com.gramcall.ai.biz.Units
import com.gramcall.ai.database.ProductEntity
import com.gramcall.ai.ui.*

@Composable
fun ProductsScreen(vm: BizViewModel) {
    val query by vm.productQuery.collectAsStateWithLifecycle()
    val category by vm.productCategory.collectAsStateWithLifecycle()
    val list by vm.products.collectAsStateWithLifecycle()
    val low by vm.lowStockCount.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var deleting by remember { mutableStateOf<ProductEntity?>(null) }
    var stockFor by remember { mutableStateOf<ProductEntity?>(null) }

    val catOptions: List<Pair<String?, String>> =
        listOf<Pair<String?, String>>(null to "सभी श्रेणियाँ") + Categories.DEFAULT.map { it to it }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SearchField(query, { vm.productQuery.value = it }, "नाम या SKU खोजें")
            Box(Modifier.padding(horizontal = 16.dp)) {
                DropdownField("श्रेणी", catOptions, category, { vm.productCategory.value = it })
            }
            if (low > 0) {
                Text(
                    "⚠ कम स्टॉक: $low प्रोडक्ट",
                    color = Danger, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            val l = list
            when {
                l == null -> LoadingBox()
                l.isEmpty() -> EmptyState("कोई प्रोडक्ट नहीं", "नीचे + बटन से पहला प्रोडक्ट जोड़ें")
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(l, key = { it.id }) { p ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name + if (!p.active) " (बंद)" else "", style = MaterialTheme.typography.titleMedium)
                                    Text("${p.category} • ${Money.fmt(p.price)} / ${Units.short(p.unit)}", style = MaterialTheme.typography.bodySmall)
                                    val isLow = p.stock <= p.minStock
                                    Text(
                                        "स्टॉक: ${Units.fmt(p.stock)} ${Units.short(p.unit)}" + if (isLow) "  ⚠ कम" else "",
                                        color = if (isLow) Danger else Green, style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                TextButton(onClick = { stockFor = p }) { Text("स्टॉक") }
                                IconButton(onClick = { editing = p; showDialog = true }) { Icon(Icons.Filled.Edit, "संपादित करें") }
                                IconButton(onClick = { deleting = p }) { Icon(Icons.Filled.Delete, "हटाएँ", tint = Danger) }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = null; showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Green, contentColor = Navy
        ) { Icon(Icons.Filled.Add, "प्रोडक्ट जोड़ें") }
    }

    if (showDialog) {
        ProductDialog(editing, onSave = { p -> vm.saveProduct(p) { ok -> if (ok) showDialog = false } }, onDismiss = { showDialog = false })
    }
    deleting?.let { p ->
        ConfirmDialog("प्रोडक्ट हटाएँ?", "\"${p.name}\" हट जाएगा। पुराने ऑर्डर में उसका नाम सुरक्षित रहेगा।",
            onConfirm = { vm.deleteProduct(p); deleting = null }, onDismiss = { deleting = null })
    }
    stockFor?.let { p ->
        var text by remember(p.id) { mutableStateOf(Units.fmt(p.stock)) }
        AlertDialog(
            onDismissRequest = { stockFor = null },
            title = { Text("${p.name} का स्टॉक") },
            text = { FormField(text, { text = it }, "नया स्टॉक (${Units.short(p.unit)})", KeyboardType.Decimal) },
            confirmButton = {
                TextButton(onClick = {
                    text.toDoubleOrNull()?.let { vm.setStock(p.id, it); stockFor = null }
                }) { Text("सेव करें") }
            },
            dismissButton = { TextButton(onClick = { stockFor = null }) { Text("रद्द करें") } }
        )
    }
}

@Composable
private fun ProductDialog(initial: ProductEntity?, onSave: (ProductEntity) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: Categories.DEFAULT.last()) }
    var unit by remember { mutableStateOf(initial?.unit ?: Units.KG) }
    var price by remember { mutableStateOf(initial?.price?.let { Units.fmt(it) } ?: "") }
    var stock by remember { mutableStateOf(initial?.stock?.let { Units.fmt(it) } ?: "0") }
    var minStock by remember { mutableStateOf(initial?.minStock?.let { Units.fmt(it) } ?: "5") }
    var sku by remember { mutableStateOf(initial?.sku ?: "") }
    var aliases by remember { mutableStateOf(initial?.aliases ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "नया प्रोडक्ट" else "प्रोडक्ट संपादित करें") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(name, { name = it }, "नाम *")
                DropdownField("श्रेणी", Categories.DEFAULT.map { it to it }, category, { category = it })
                DropdownField("इकाई", Units.ALL.map { it to Units.label(it) }, unit, { unit = it })
                FormField(price, { price = it }, "बिक्री कीमत (₹) *", KeyboardType.Decimal)
                FormField(stock, { stock = it }, "स्टॉक", KeyboardType.Decimal)
                FormField(minStock, { minStock = it }, "कम-स्टॉक सीमा", KeyboardType.Decimal)
                FormField(sku, { sku = it }, "SKU")
                FormField(aliases, { aliases = it }, "दूसरे नाम (कॉमा से, जैसे tel,oil)")
                FormField(desc, { desc = it }, "विवरण", singleLine = false)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("चालू (Active)", Modifier.weight(1f))
                    Switch(active, { active = it })
                }
                error?.let { Text(it, color = Danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pr = price.toDoubleOrNull()
                val st = stock.toDoubleOrNull()
                val ms = minStock.toDoubleOrNull()
                if (pr == null || st == null || ms == null) { error = "कीमत/स्टॉक संख्या में लिखें"; return@TextButton }
                onSave(
                    ProductEntity(
                        id = initial?.id ?: 0, name = name, category = category, unit = unit, price = pr,
                        stock = st, minStock = ms, sku = sku, description = desc, aliases = aliases,
                        active = active, createdAt = initial?.createdAt ?: System.currentTimeMillis()
                    )
                )
            }) { Text("सेव करें") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("रद्द करें") } }
    )
}
