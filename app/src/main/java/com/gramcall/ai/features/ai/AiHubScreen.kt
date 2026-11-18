package com.gramcall.ai.features.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gramcall.ai.biz.Scripts
import com.gramcall.ai.features.calling.InfoCard
import com.gramcall.ai.repository.Keys
import com.gramcall.ai.ui.*

val VOICE_TYPES = listOf("सामान्य", "भारी", "पतली")
val AI_LANGUAGES = listOf("हिंदी", "Hinglish")

@Composable
fun AiHubScreen(vm: BizViewModel, onOpenAssistant: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    var agent by remember(s) { mutableStateOf(s[Keys.AGENT_NAME] ?: "GramCall Assistant") }
    var lang by remember(s) { mutableStateOf(s[Keys.LANGUAGE] ?: AI_LANGUAGES[0]) }
    var voice by remember(s) { mutableStateOf(s[Keys.VOICE_TYPE] ?: VOICE_TYPES[0]) }
    var speed by remember(s) { mutableFloatStateOf(s[Keys.VOICE_SPEED]?.toFloatOrNull() ?: 1f) }
    var greeting by remember(s) { mutableStateOf(s[Keys.GREETING] ?: Scripts.DEFAULT_GREETING) }
    var confirm by remember(s) { mutableStateOf(s[Keys.CONFIRM_SCRIPT] ?: Scripts.DEFAULT_CONFIRM) }
    var thanks by remember(s) { mutableStateOf(s[Keys.THANKS_SCRIPT] ?: Scripts.DEFAULT_THANKS) }
    var delivery by remember(s) { mutableStateOf(s[Keys.DELIVERY_INFO] ?: "") }
    var productInfo by remember(s) { mutableStateOf(s[Keys.PRODUCT_INFO] ?: "") }
    var priceInfo by remember(s) { mutableStateOf(s[Keys.PRICE_INFO] ?: "") }
    var business by remember(s) { mutableStateOf(s[Keys.SHOP_NAME] ?: "") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoCard("AI सहायक — अभी क्या काम करता है") {
            Text("✓ ऑर्डर सहायक: ग्राहक की बात (बोलकर या लिखकर) से सामान, मात्रा और कीमत निकालता है। " +
                "सिर्फ़ आपके प्रोडक्ट और आपकी कीमतें इस्तेमाल होती हैं; जो समझ न आए उसे “जाँचें” दिखाता है।", style = MaterialTheme.typography.bodySmall)
            Text("✓ ऑर्डर तभी “कन्फ़र्म” होता है जब आप ग्राहक की पुष्टि के बाद बटन दबाएँ।", style = MaterialTheme.typography.bodySmall)
            Text("✗ सामान्य SIM कॉल पर AI खुद ग्राहक से बात नहीं करता — Android इसकी अनुमति नहीं देता। " +
                "इसके लिए अलग टेलीफ़ोनी प्रोवाइडर चाहिए। समझने वाला हिस्सा बदला जा सकता है (OrderUnderstandingProvider); अभी सिर्फ़ ऑन-डिवाइस नियम-आधारित प्रोवाइडर चालू है।", style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onOpenAssistant, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
            ) { Text("ऑर्डर सहायक खोलें") }
        }

        InfoCard("AI सेटिंग्स") {
            FormField(agent, { agent = it }, "एजेंट का नाम")
            DropdownField("भाषा", AI_LANGUAGES.map { it to it }, lang, { lang = it })
            DropdownField("आवाज़ का प्रकार", VOICE_TYPES.map { it to it }, voice, { voice = it })
            Text("बोलने की गति: %.1fx".format(speed))
            Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..1.5f)
            FormField(business, { business = it }, "बिज़नेस / दुकान का नाम")
            FormField(greeting, { greeting = it }, "ग्रीटिंग संदेश ({business} लिख सकते हैं)", singleLine = false)
            FormField(confirm, { confirm = it }, "ऑर्डर पुष्टि स्क्रिप्ट ({items}, {total})", singleLine = false)
            FormField(thanks, { thanks = it }, "समापन संदेश", singleLine = false)
            FormField(delivery, { delivery = it }, "डिलीवरी की जानकारी", singleLine = false)
            FormField(productInfo, { productInfo = it }, "प्रोडक्ट जानकारी (नोट)", singleLine = false)
            FormField(priceInfo, { priceInfo = it }, "कीमत जानकारी (नोट)", singleLine = false)
            Text("कीमतें हमेशा प्रोडक्ट लिस्ट से आती हैं; ऊपर के नोट सिर्फ़ आपके लिए दिखते हैं।", style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = {
                    vm.saveSettings(
                        mapOf(
                            Keys.AGENT_NAME to agent, Keys.LANGUAGE to lang, Keys.VOICE_TYPE to voice,
                            Keys.VOICE_SPEED to speed.toString(), Keys.SHOP_NAME to business,
                            Keys.GREETING to greeting, Keys.CONFIRM_SCRIPT to confirm, Keys.THANKS_SCRIPT to thanks,
                            Keys.DELIVERY_INFO to delivery, Keys.PRODUCT_INFO to productInfo, Keys.PRICE_INFO to priceInfo
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
            ) { Text("सेव करें") }
        }
    }
}
