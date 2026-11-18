package com.gramcall.ai.features.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.biz.*
import com.gramcall.ai.database.OrderEntity
import com.gramcall.ai.database.OrderItemEntity
import com.gramcall.ai.features.calling.InfoCard
import com.gramcall.ai.features.calling.KV
import com.gramcall.ai.ui.*

@Composable
fun OrderEditScreen(vm: BizViewModel, id: Long, presetCustomerId: Long, campaignId: Long, onDone: (Long) -> Unit) {
    var order by remember { mutableStateOf<OrderEntity?>(null) }
    val items = remember { mutableStateListOf<OrderItemEntity>() }
    var loaded by remember { mutableStateOf(id == 0L) }
    LaunchedEffect(id) {
        if (id != 0L) {
            val (o, its) = vm.loadOrder(id)
            order = o
            items.clear(); items.addAll(its)
            loaded = true
        }
    }
    if (!loaded) { LoadingBox(); return }
    if (id != 0L && order == null) { EmptyState("ऑर्डर नहीं मिला"); return }
    OrderForm(vm, order, items, presetCustomerId, if (campaignId > 0) campaignId else null, onDone)
}

@Composable
private fun OrderForm(
    vm: BizViewModel, initial: OrderEntity?, items: SnapshotStateList<OrderItemEntity>,
    presetCustomerId: Long, campaignId: Long?, onDone: (Long) -> Unit
) {
    val products by vm.activeProducts.collectAsStateWithLifecycle()
    var order by remember { mutableStateOf(initial) }
    var discount by remember { mutableStateOf(initial?.discount?.let { Units.fmt(it) } ?: "0") }
    var address by remember { mutableStateOf(initial?.deliveryAddress ?: "") }
    var status by remember { mutableStateOf(initial?.status ?: OrderStatus.CONFIRMED) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var pickCustomer by remember { mutableStateOf(false) }
    var pickProduct by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(presetCustomerId) {
        if (initial == null && presetCustomerId > 0) {
            vm.pickCustomer(presetCustomerId, campaignId) { fresh ->
                order = fresh
                if (address.isBlank()) address = fresh?.deliveryAddress.orEmpty()
            }
        }
    }

    val totals = OrderCalc.totals(items.map { it.quantity to it.unitPrice }, discount.toDoubleOrNull() ?: 0.0)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(onClick = { pickCustomer = true }, modifier = Modifier.fillMaxWidth()) {
            val o = order
            Text(if (o == null) "ग्राहक चुनें *" else "${o.customerName} (${o.customerMobile})")
        }

        Text("सामान", style = MaterialTheme.typography.titleMedium)
        items.forEachIndexed { i, it ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(it.productName)
                        Text(
                            "${Money.fmt(it.unitPrice)} / ${Units.short(it.unit)} • कुल ${Money.fmt(OrderCalc.lineTotal(it.quantity, it.unitPrice))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    var qText by remember(it.id, it.productId, i) { mutableStateOf(Units.fmt(it.quantity)) }
                    OutlinedTextField(
                        value = qText,
                        onValueChange = { t ->
                            qText = t
                            t.toDoubleOrNull()?.let { q -> items[i] = items[i].copy(quantity = q) }
                        },
                        singleLine = true, label = { Text(Units.short(it.unit)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(100.dp)
                    )
                    IconButton(onClick = { items.removeAt(i) }) { Icon(Icons.Filled.Close, "हटाएँ") }
                }
            }
        }
        OutlinedButton(onClick = { pickProduct = true }, modifier = Modifier.fillMaxWidth()) { Text("+ सामान जोड़ें") }

        FormField(discount, { discount = it }, "छूट (₹)", KeyboardType.Decimal)
        FormField(address, { address = it }, "डिलीवरी का पता", singleLine = false)
        DropdownField("स्थिति", OrderStatus.ALL.map { it to OrderStatus.label(it) }, status, { status = it })
        FormField(note, { note = it }, "नोट", singleLine = false)

        InfoCard("योग") {
            KV("उप-योग", Money.fmt(totals.subtotal))
            KV("छूट", Money.fmt(totals.discount))
            KV("कुल", Money.fmt(totals.total))
        }

        Button(
            enabled = !saving && order != null && items.isNotEmpty(),
            onClick = {
                val o = order ?: return@Button
                saving = true
                vm.saveOrder(
                    o.copy(discount = discount.toDoubleOrNull() ?: 0.0, deliveryAddress = address.trim(), status = status, note = note.trim()),
                    items.toList()
                ) { newId ->
                    saving = false
                    if (newId != null) onDone(newId)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
        ) { Text("ऑर्डर सेव करें") }
    }

    if (pickCustomer) {
        CustomerPickerDialog(vm, onPick = { cw ->
            pickCustomer = false
            val keepId = order?.id ?: 0L
            vm.pickCustomer(cw.customer.id) { fresh ->
                order = fresh?.copy(id = keepId, status = status)
                if (address.isBlank()) address = fresh?.deliveryAddress.orEmpty()
            }
        }, onDismiss = { pickCustomer = false })
    }
    if (pickProduct) {
        ProductPickerDialog(products, onPick = { p ->
            pickProduct = false
            items.add(
                OrderItemEntity(
                    orderId = 0, productId = p.id, productName = p.name, unit = p.unit,
                    quantity = 1.0, unitPrice = p.price, lineTotal = p.price
                )
            )
        }, onDismiss = { pickProduct = false })
    }
}
