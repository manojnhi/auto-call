package com.gramcall.ai.features.ai

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.biz.*
import com.gramcall.ai.database.OrderEntity
import com.gramcall.ai.database.OrderItemEntity
import com.gramcall.ai.database.ProductEntity
import com.gramcall.ai.features.calling.InfoCard
import com.gramcall.ai.features.calling.KV
import com.gramcall.ai.features.orders.CustomerPickerDialog
import com.gramcall.ai.features.orders.ProductPickerDialog
import com.gramcall.ai.repository.Keys
import com.gramcall.ai.ui.*
import java.util.Locale

private class CartLine(
    val raw: String, product: ProductEntity?, qty: Double?, val candidates: List<ProductEntity>
) {
    var product by mutableStateOf(product)
    var qty by mutableStateOf(qty)
    val ready get() = product != null && (qty ?: 0.0) > 0.0
}

/** AI order sahayak: ग्राहक की बात -> सामान/मात्रा/कीमत -> पुष्टि -> ऑर्डर. Kuch bhi apni taraf se nahi banata. */
@Composable
fun OrderAssistantScreen(vm: BizViewModel, presetCustomerId: Long, campaignId: Long, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val products by vm.activeProducts.collectAsStateWithLifecycle()

    var order by remember { mutableStateOf<OrderEntity?>(null) }
    var text by remember { mutableStateOf("") }
    val cart = remember { mutableStateListOf<CartLine>() }
    var parsedOnce by remember { mutableStateOf(false) }
    var readBack by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf("") }
    var pickCustomer by remember { mutableStateOf(false) }
    var pickProduct by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val camp: Long? = if (campaignId > 0) campaignId else null
    LaunchedEffect(presetCustomerId) {
        if (presetCustomerId > 0) {
            vm.pickCustomer(presetCustomerId, camp, "ASSISTED") { o -> order = o; address = o?.deliveryAddress.orEmpty() }
        }
    }

    // TTS (sirf phone ke speaker par, owner/customer ko sunane ke liye)
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var ref: TextToSpeech? = null
        ref = TextToSpeech(ctx) { st ->
            if (st == TextToSpeech.SUCCESS) {
                val r = ref?.setLanguage(Locale("hi", "IN"))
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        ref
    }
    DisposableEffect(Unit) { onDispose { tts?.shutdown() } }
    fun speak(t: String) {
        if (!ttsReady) return
        tts?.setSpeechRate((settings[Keys.VOICE_SPEED]?.toFloatOrNull() ?: 1f))
        tts?.setPitch(when (settings[Keys.VOICE_TYPE]) { "भारी" -> 0.8f; "पतली" -> 1.2f; else -> 1f })
        tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, "gramcall")
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val said = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!said.isNullOrBlank()) text = if (text.isBlank()) said else "$text और $said"
        }
    }

    val shop = settings[Keys.SHOP_NAME].orEmpty().ifBlank { "GramCall" }
    val greeting = Scripts.fill(settings[Keys.GREETING] ?: Scripts.DEFAULT_GREETING, mapOf("business" to shop))
    val totals = OrderCalc.totals(cart.filter { it.ready }.map { it.qty!! to it.product!!.price }, 0.0)
    val allReady = cart.isNotEmpty() && cart.all { it.ready }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoCard("1. ग्राहक") {
            OutlinedButton(onClick = { pickCustomer = true }, modifier = Modifier.fillMaxWidth()) {
                val o = order
                Text(if (o == null) "ग्राहक चुनें *" else "${o.customerName} (${o.customerMobile})")
            }
            Text(greeting, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { speak(greeting) }, enabled = ttsReady) {
                Text(if (ttsReady) "ग्रीटिंग आवाज़ में सुनाएँ" else "हिंदी आवाज़ (TTS) उपलब्ध नहीं")
            }
        }

        InfoCard("2. ग्राहक ने क्या कहा") {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("जैसे: 5 किलो आटा और 2 किलो चीनी") }, minLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "ग्राहक का ऑर्डर बोलिए")
                    }
                    try { micLauncher.launch(i) } catch (e: ActivityNotFoundException) { vm.messages.tryEmit("इस फ़ोन पर वॉइस इनपुट उपलब्ध नहीं — लिखकर डालें") }
                }) { Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(6.dp)); Text("बोलें") }
                Button(
                    onClick = { parsedOnce = true; readBack = null },
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
                ) { Text("समझें") }
            }
        }

        // parse trigger
        LaunchedEffect(parsedOnce) {
            if (parsedOnce) {
                val (lines, prods) = vm.parseOrder(text)
                cart.clear()
                lines.forEach { l ->
                    val p = prods.firstOrNull { it.id == l.product?.id }
                    cart.add(CartLine(l.raw, p, l.quantity, l.candidates.mapNotNull { c -> prods.firstOrNull { it.id == c.id } }))
                }
                if (lines.isEmpty()) vm.messages.tryEmit("कोई सामान पहचाना नहीं गया — नीचे से हाथ से जोड़ें")
                parsedOnce = false
            }
        }

        InfoCard("3. सामान जाँचें") {
            if (cart.isEmpty()) Text("अभी कोई सामान नहीं", color = MaterialTheme.colorScheme.onSurfaceVariant)
            cart.forEachIndexed { i, line ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(10.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("“${line.raw}”", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { cart.removeAt(i) }) { Icon(Icons.Filled.Close, "हटाएँ") }
                        }
                        val opts = (if (line.candidates.isNotEmpty()) line.candidates else products)
                        DropdownField(
                            "सामान", listOf<Pair<Long?, String>>(null to "— चुनें (जाँचें) —") + opts.map { it.id to it.name },
                            line.product?.id, { id -> line.product = products.firstOrNull { it.id == id } }
                        )
                        var qText by remember(line) { mutableStateOf(line.qty?.let { Units.fmt(it) } ?: "") }
                        OutlinedTextField(
                            value = qText,
                            onValueChange = { t -> qText = t; line.qty = t.toDoubleOrNull() },
                            singleLine = true,
                            label = { Text("मात्रा" + (line.product?.let { " (${Units.short(it.unit)})" } ?: "")) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        val p = line.product
                        if (p != null && line.qty != null) {
                            Text("${Money.fmt(p.price)} × ${Units.fmt(line.qty!!)} = ${Money.fmt(OrderCalc.lineTotal(line.qty!!, p.price))}", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("⚠ सामान/मात्रा पक्की करें — अंदाज़ा नहीं लगाया गया", color = Danger, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { pickProduct = true }, modifier = Modifier.fillMaxWidth()) { Text("+ सामान हाथ से जोड़ें") }
            KV("कुल", Money.fmt(totals.total))
        }

        InfoCard("4. पुष्टि") {
            FormField(address, { address = it }, "डिलीवरी का पता", singleLine = false)
            settings[Keys.DELIVERY_INFO]?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(
                enabled = allReady,
                onClick = {
                    val parts = cart.map { it.product!!.name to (it.qty!! to it.product!!.unit) }
                    val t = Scripts.fill(
                        settings[Keys.CONFIRM_SCRIPT] ?: Scripts.DEFAULT_CONFIRM,
                        mapOf("items" to Scripts.itemsText(parts), "total" to Money.fmt(totals.total))
                    )
                    readBack = t
                    speak(t)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ऑर्डर ग्राहक को दोहराएँ (पुष्टि के लिए)") }
            readBack?.let { Text(it, color = Green) }

            fun save(status: String) {
                val o = order ?: return
                saving = true
                val items = cart.map {
                    val p = it.product!!
                    OrderItemEntity(orderId = 0, productId = p.id, productName = p.name, unit = p.unit,
                        quantity = it.qty!!, unitPrice = p.price, lineTotal = OrderCalc.lineTotal(it.qty!!, p.price))
                }
                vm.saveOrder(o.copy(deliveryAddress = address.trim(), status = status, source = "ASSISTED", campaignId = camp), items) { id ->
                    saving = false
                    if (id != null) {
                        if (status == OrderStatus.CONFIRMED) speak(settings[Keys.THANKS_SCRIPT] ?: Scripts.DEFAULT_THANKS)
                        onDone()
                    }
                }
            }

            Button(
                enabled = !saving && order != null && allReady && readBack != null,
                onClick = { save(OrderStatus.CONFIRMED) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
            ) { Text("ग्राहक ने हाँ कहा — कन्फ़र्म ऑर्डर सेव करें") }
            OutlinedButton(
                enabled = !saving && order != null && allReady,
                onClick = { save(OrderStatus.PENDING) }, modifier = Modifier.fillMaxWidth()
            ) { Text("अभी पुष्टि नहीं — पेंडिंग सेव करें") }
            Text("कन्फ़र्म बटन तभी चलता है जब आप ऊपर पुष्टि दोहरा चुके हों।", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (pickCustomer) {
        CustomerPickerDialog(vm, onPick = { cw ->
            pickCustomer = false
            vm.pickCustomer(cw.customer.id, camp, "ASSISTED") { o -> order = o; address = o?.deliveryAddress.orEmpty() }
        }, onDismiss = { pickCustomer = false })
    }
    if (pickProduct) {
        ProductPickerDialog(products, onPick = { p ->
            pickProduct = false
            cart.add(CartLine(p.name, p, 1.0, emptyList()))
        }, onDismiss = { pickProduct = false })
    }
}
