package com.gramcall.ai.features.orders

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.biz.*
import com.gramcall.ai.features.calling.InfoCard
import com.gramcall.ai.features.calling.KV
import com.gramcall.ai.ui.*
import com.gramcall.ai.utils.Fmt
import kotlinx.coroutines.launch

private var printWebView: WebView? = null

fun printHtml(context: Context, html: String, jobName: String) {
    val wv = WebView(context)
    wv.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            pm.print(jobName, view.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().build())
            printWebView = null
        }
    }
    printWebView = wv
    wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

@Composable
fun OrderDetailScreen(vm: BizViewModel, id: Long, onEdit: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val order by remember(id) { vm.order(id) }.collectAsStateWithLifecycle(initialValue = null)
    val items by remember(id) { vm.orderItems(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var confirmCancel by remember { mutableStateOf(false) }

    val o = order
    if (o == null) { EmptyState("ऑर्डर नहीं मिला"); return }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoCard("ऑर्डर #${o.id}") {
            KV("ग्राहक", o.customerName)
            KV("मोबाइल", o.customerMobile)
            KV("गाँव / शहर", listOf(o.villageName, o.cityName).filter { it.isNotBlank() }.joinToString(" / ").ifEmpty { "—" })
            KV("तारीख", Fmt.dateTime(o.orderAt))
            KV("पता", o.deliveryAddress.ifEmpty { "—" })
            KV("स्रोत", if (o.source == "ASSISTED") "AI सहायक" else "मैन्युअल")
            if (o.note.isNotEmpty()) KV("नोट", o.note)
        }
        InfoCard("सामान") {
            items.forEach {
                Text("${it.productName}: ${Units.fmt(it.quantity)} ${Units.short(it.unit)} × ${Money.fmt(it.unitPrice)} = ${Money.fmt(it.lineTotal)}")
            }
            Divider()
            KV("उप-योग", Money.fmt(o.subtotal))
            KV("छूट", Money.fmt(o.discount))
            KV("कुल", Money.fmt(o.total))
        }
        DropdownField(
            "स्थिति", OrderStatus.ALL.map { it to OrderStatus.label(it) }, o.status,
            { vm.setOrderStatus(o.id, it) }
        )
        Button(
            onClick = onEdit, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
        ) { Text("ऑर्डर संपादित करें") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    vm.invoiceText(o.id)?.let { t ->
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, t) }
                        ctx.startActivity(Intent.createChooser(send, "इनवॉइस शेयर करें"))
                    }
                }
            }, modifier = Modifier.weight(1f)) { Text("शेयर") }
            OutlinedButton(onClick = {
                scope.launch { vm.invoiceText(o.id)?.let { printHtml(ctx, Invoice.html(it), "Invoice-${o.id}") } }
            }, modifier = Modifier.weight(1f)) { Text("प्रिंट") }
        }
        if (o.status != OrderStatus.CANCELLED) {
            OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth()) {
                Text("ऑर्डर रद्द करें", color = Danger)
            }
        }
    }
    if (confirmCancel) {
        ConfirmDialog("ऑर्डर रद्द करें?", "स्टॉक वापस जुड़ जाएगा।", "रद्द करें",
            onConfirm = { confirmCancel = false; vm.setOrderStatus(o.id, OrderStatus.CANCELLED) },
            onDismiss = { confirmCancel = false })
    }
}
