package com.gramcall.ai.features.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.biz.Money
import com.gramcall.ai.biz.Units
import com.gramcall.ai.database.CustomerWithPlace
import com.gramcall.ai.database.ProductEntity
import com.gramcall.ai.ui.BizViewModel
import com.gramcall.ai.ui.FormField

@Composable
fun CustomerPickerDialog(vm: BizViewModel, onPick: (CustomerWithPlace) -> Unit, onDismiss: () -> Unit) {
    val q by vm.pickerQuery.collectAsStateWithLifecycle()
    val list by vm.pickerCustomers.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ग्राहक चुनें") },
        text = {
            Column {
                FormField(q, { vm.pickerQuery.value = it }, "नाम या मोबाइल")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(list.take(50), key = { it.customer.id }) { c ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(c) }.padding(vertical = 8.dp)) {
                            Text(c.customer.name)
                            Text(
                                "${c.customer.mobile} • " + listOfNotNull(c.villageName, c.cityName).joinToString(" / "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("बंद करें") } }
    )
}

@Composable
fun ProductPickerDialog(products: List<ProductEntity>, onPick: (ProductEntity) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val shown = products.filter { it.name.contains(q.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("सामान चुनें") },
        text = {
            Column {
                FormField(q, { q = it }, "सामान खोजें")
                Spacer(Modifier.height(8.dp))
                if (products.isEmpty()) Text("पहले प्रोडक्ट मैनेजमेंट में सामान जोड़ें")
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(shown, key = { it.id }) { p ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(p) }.padding(vertical = 8.dp)) {
                            Text(p.name)
                            Text(
                                "${Money.fmt(p.price)} / ${Units.short(p.unit)} • स्टॉक ${Units.fmt(p.stock)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("बंद करें") } }
    )
}
