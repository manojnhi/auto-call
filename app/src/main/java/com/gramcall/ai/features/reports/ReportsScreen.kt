package com.gramcall.ai.features.reports

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.biz.*
import com.gramcall.ai.calling.CallStatus
import com.gramcall.ai.features.calling.InfoCard
import com.gramcall.ai.features.calling.KV
import com.gramcall.ai.repository.ReportData
import com.gramcall.ai.ui.*
import com.gramcall.ai.utils.CsvUtils
import com.gramcall.ai.utils.Fmt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(vm: BizViewModel, appVm: AppViewModel) {
    val ctx = LocalContext.current
    val villages by appVm.allVillages.collectAsStateWithLifecycle()
    val cities by appVm.allCities.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var preset by remember { mutableStateOf(DatePreset.TODAY) }
    var customFrom by remember { mutableStateOf<Long?>(null) }
    var customTo by remember { mutableStateOf<Long?>(null) }
    var villageId by remember { mutableStateOf<Long?>(null) }
    var cityId by remember { mutableStateOf<Long?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<ReportData?>(null) }

    LaunchedEffect(preset, customFrom, customTo, villageId, cityId, status) {
        data = vm.report(preset, customFrom, customTo, villageId, cityId, status)
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val d = data
        if (uri != null && d != null) vm.writeText(uri, reportCsv(d), "रिपोर्ट एक्सपोर्ट हो गई")
    }

    fun pickDate(initial: Long?, onPicked: (Long) -> Unit) {
        val c = Calendar.getInstance().apply { if (initial != null) timeInMillis = initial }
        DatePickerDialog(ctx, { _, y, m, d ->
            onPicked(Calendar.getInstance().apply { clear(); set(y, m, d, 12, 0, 0) }.timeInMillis)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
    fun dstr(t: Long?) = if (t == null) "चुनें" else SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN")).format(Date(t))

    val villageOptions: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to "सभी गाँव") + villages.map { it.id to it.name }
    val cityOptions: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to "सभी शहर") + cities.map { it.id to it.name }
    val statusOptions: List<Pair<String?, String>> =
        listOf<Pair<String?, String>>(null to "सभी ऑर्डर स्थितियाँ") + OrderStatus.ALL.map { it to OrderStatus.label(it) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("तारीख अनुसार") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("गाँव/शहर अनुसार") })
        }
        DropdownField("तारीख", DatePreset.values().map { it to it.label }, preset, { preset = it })
        if (preset == DatePreset.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickDate(customFrom) { customFrom = it } }, modifier = Modifier.weight(1f)) { Text("से: ${dstr(customFrom)}") }
                OutlinedButton(onClick = { pickDate(customTo) { customTo = it } }, modifier = Modifier.weight(1f)) { Text("तक: ${dstr(customTo)}") }
            }
        }
        DropdownField("गाँव", villageOptions, villageId, { villageId = it; if (it != null) cityId = null })
        DropdownField("शहर", cityOptions, cityId, { cityId = it; if (it != null) villageId = null })
        DropdownField("ऑर्डर स्थिति", statusOptions, status, { status = it })

        val d = data
        if (d == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (tab == 0) {
            val s = ReportCalc.summarize(d.calls, d.orders)
            InfoCard("सारांश") {
                KV("कुल कॉल", s.totalCalls.toString())
                KV("कुल ऑर्डर", s.totalOrders.toString())
                KV("कुल बिक्री", Money.fmt(s.totalSales))
                KV("संपर्क किए ग्राहक", s.customersContacted.toString())
                KV("सफल कॉल (बात हुई)", s.successfulCalls.toString())
                KV("फेल कॉल", s.failedCalls.toString())
                KV("नो आंसर", s.noAnswer.toString())
                KV("रद्द ऑर्डर", s.cancelledOrders.toString())
            }
        } else {
            if (villageId == null && cityId == null) {
                Text("ऊपर से गाँव या शहर चुनें।", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val r = ReportCalc.village(d.calls, d.orders, d.items)
                InfoCard("गाँव/शहर रिपोर्ट") {
                    KV("कॉल किए ग्राहक", r.customersCalled.toString())
                    KV("ऑर्डर मिले", r.ordersReceived.toString())
                    KV("नो आंसर", r.noAnswer.toString())
                    KV("बात हुई, ऑर्डर नहीं", r.talkedNoOrder.toString())
                    KV("कुल बिक्री", Money.fmt(r.totalSales))
                }
                r.rows.forEach { row ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${row.customerName} • ${row.mobile}", style = MaterialTheme.typography.titleSmall)
                            Text("कॉल: ${row.callStatus?.let { CallStatus.label(it) } ?: "—"}", style = MaterialTheme.typography.bodySmall)
                            Text("ऑर्डर: ${row.orderSummary}", style = MaterialTheme.typography.bodySmall)
                            Text("राशि: ${Money.fmt(row.amount)} • तारीख: ${Fmt.dateTime(row.orderDate)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { exportLauncher.launch("report.csv") }, modifier = Modifier.fillMaxWidth()) { Text("CSV एक्सपोर्ट") }
        }
    }
}

private fun reportCsv(d: ReportData): String {
    val rows = mutableListOf(listOf("type", "date", "customer", "mobile", "status", "amount", "detail"))
    d.calls.forEach {
        rows.add(listOf("call", Fmt.dateTime(it.startedAt), it.customerName, it.mobile, CallStatus.label(it.status), "", Fmt.duration(it.durationSec)))
    }
    val items = d.items.groupBy { it.orderId }
    d.orders.forEach { o ->
        val text = items[o.id].orEmpty().joinToString("; ") { "${Units.fmt(it.quantity)} ${Units.short(it.unit)} ${it.productName}" }
        rows.add(listOf("order", Fmt.dateTime(o.orderAt), o.customerName, o.customerMobile, OrderStatus.label(o.status), o.total.toString(), text))
    }
    return CsvUtils.toCsv(rows)
}
