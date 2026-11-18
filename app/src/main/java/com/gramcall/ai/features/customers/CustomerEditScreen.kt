package com.gramcall.ai.features.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.database.CustomerEntity
import com.gramcall.ai.ui.*

private val categories = listOf("सामान्य", "नियमित", "थोक")
private val languages = listOf("हिंदी", "Hinglish", "English")

@Composable
fun CustomerEditScreen(vm: AppViewModel, id: Long, onDone: () -> Unit) {
    var initial by remember { mutableStateOf<CustomerEntity?>(null) }
    var loaded by remember { mutableStateOf(id == 0L) }
    LaunchedEffect(id) {
        if (id != 0L) {
            initial = vm.loadCustomer(id)
            loaded = true
        }
    }
    if (!loaded) {
        LoadingBox()
        return
    }
    if (id != 0L && initial == null) {
        EmptyState("ग्राहक नहीं मिला")
        return
    }
    CustomerForm(vm, initial, onDone)
}

@Composable
private fun CustomerForm(vm: AppViewModel, initial: CustomerEntity?, onDone: () -> Unit) {
    val villages by vm.allVillages.collectAsStateWithLifecycle()
    val cities by vm.allCities.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var mobile by remember { mutableStateOf(initial?.mobile ?: "") }
    var alt by remember { mutableStateOf(initial?.altMobile ?: "") }
    var villageId by remember { mutableStateOf(initial?.villageId) }
    var cityId by remember { mutableStateOf(initial?.cityId) }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: categories[0]) }
    var language by remember { mutableStateOf(initial?.language ?: languages[0]) }
    var consent by remember { mutableStateOf(initial?.callConsent ?: false) }
    var time by remember { mutableStateOf(initial?.preferredTime ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var saving by remember { mutableStateOf(false) }

    val villageOptions: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to "— कोई नहीं —") + villages.map { it.id to it.name }
    val cityOptions: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to "— कोई नहीं —") + cities.map { it.id to it.name }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FormField(name, { name = it }, "पूरा नाम *")
        FormField(mobile, { mobile = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(15) }, "मोबाइल नंबर *", KeyboardType.Phone)
        FormField(alt, { alt = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(15) }, "दूसरा मोबाइल नंबर", KeyboardType.Phone)
        DropdownField("गाँव", villageOptions, villageId, { villageId = it })
        DropdownField("शहर", cityOptions, cityId, { cityId = it })
        if (villages.isEmpty() && cities.isEmpty()) {
            Text(
                "पहले गाँव या शहर जोड़ें, फिर ग्राहक सेव होगा।",
                color = Danger, style = MaterialTheme.typography.bodySmall
            )
        }
        FormField(address, { address = it }, "पूरा पता", singleLine = false)
        DropdownField("श्रेणी", categories.map { it to it }, category, { category = it })
        DropdownField("पसंदीदा भाषा", languages.map { it to it }, language, { language = it })
        FormField(time, { time = it }, "कॉल का पसंदीदा समय (जैसे शाम 5-7)")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("कॉल की सहमति")
                Text(
                    "ग्राहक ने कॉल पर संपर्क की अनुमति दी है",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = consent, onCheckedChange = { consent = it })
        }
        FormField(notes, { notes = it }, "नोट्स", singleLine = false)

        Button(
            enabled = !saving,
            onClick = {
                saving = true
                val consentDate = when {
                    !consent -> null
                    initial?.callConsent == true -> initial?.consentDate
                    else -> System.currentTimeMillis()
                }
                vm.saveCustomer(
                    CustomerEntity(
                        id = initial?.id ?: 0, name = name, mobile = mobile, altMobile = alt,
                        villageId = villageId, cityId = cityId, address = address,
                        category = category, language = language, callConsent = consent,
                        consentDate = consentDate, preferredTime = time, notes = notes,
                        createdAt = initial?.createdAt ?: System.currentTimeMillis()
                    )
                ) { r ->
                    saving = false
                    if (r.ok) onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
        ) { Text("सेव करें") }
    }
}
