package com.gramcall.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun AppRoot(session: SessionViewModel = viewModel()) {
    val gate by session.gate.collectAsStateWithLifecycle()
    when (gate) {
        Gate.LOADING -> SplashScreen()
        Gate.ONBOARDING -> OnboardingScreen(session)
        Gate.LOCKED -> LockScreen(session)
        Gate.OPEN -> MainShell(session = session)
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GramCall AI", style = MaterialTheme.typography.displaySmall, color = Green)
            Spacer(Modifier.height(8.dp))
            Text("किराना दुकान का स्मार्ट साथी", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun OnboardingScreen(session: SessionViewModel) {
    var shop by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("स्वागत है", style = MaterialTheme.typography.headlineMedium)
        Text(
            "GramCall AI से गाँव-वार ग्राहक, कॉल अभियान और ऑर्डर संभालें। " +
                "कॉल सिर्फ़ उन्हीं ग्राहकों को होती है जिन्होंने सहमति दी है।",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FormField(shop, { shop = it }, "दुकान का नाम *")
        FormField(mobile, { mobile = it }, "दुकान का मोबाइल", KeyboardType.Phone)
        Text("ऐप लॉक (वैकल्पिक): 4-6 अंक का PIN", style = MaterialTheme.typography.titleSmall)
        listOf(pin to "PIN", pin2 to "PIN दोबारा").forEachIndexed { i, (v, label) ->
            OutlinedTextField(
                value = v, onValueChange = { t -> val d = t.filter(Char::isDigit).take(6); if (i == 0) pin = d else pin2 = d },
                label = { Text(label) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
        }
        error?.let { Text(it, color = Danger) }
        Button(
            onClick = {
                when {
                    shop.isBlank() -> error = "दुकान का नाम लिखें"
                    pin.isNotEmpty() && (pin.length < 4 || pin != pin2) -> error = "PIN 4-6 अंक का हो और दोनों बार एक जैसा"
                    else -> session.completeOnboarding(shop, mobile, pin)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
        ) { Text("शुरू करें") }
    }
}

@Composable
private fun LockScreen(session: SessionViewModel) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var wait by remember { mutableIntStateOf(session.lockoutSeconds()) }
    LaunchedEffect(wait) { if (wait > 0) { delay(1000); wait = session.lockoutSeconds() } }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("GramCall AI", style = MaterialTheme.typography.headlineMedium, color = Green)
        Spacer(Modifier.height(16.dp))
        Text("PIN डालें")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(6); error = null },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        if (wait > 0) Text("बहुत ज़्यादा गलत कोशिशें — $wait सेकंड रुकें", color = Danger)
        else error?.let { Text(it, color = Danger) }
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = pin.length >= 4 && wait == 0,
            onClick = {
                if (!session.unlock(pin)) { error = "PIN गलत है"; pin = ""; wait = session.lockoutSeconds() }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Navy)
        ) { Text("खोलें") }
    }
}
