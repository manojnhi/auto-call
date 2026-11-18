# Calling feasibility

## Jo code mein hai (supported Android APIs)
- SIM 1 detect: `SubscriptionManager` (slot 0), operator/number/state `TelephonyManager`.
- Call: `TelecomManager.placeCall()` + SIM 1 ka `PhoneAccountHandle`. Match na mile to app call nahi lagata.
- Call state: `TelephonyCallback` (API 31+) / `PhoneStateListener`: IDLE, RINGING, OFFHOOK.
- Permissions: READ_PHONE_STATE, CALL_PHONE, READ_PHONE_NUMBERS (optional). Call log / mic / SMS nahi.

## Jo Android nahi deta
- Normal SIM call mein sunne wale ki awaaz padhna ya apni awaaz daalna: **supported nahi**. AI ka SIM call par khud baat karna claim nahi kiya jaata.
- Ringing/Connected/Busy/No answer/Rejected ka bharosemand signal bina call-log ya dialer role ke nahi milta → har call ke baad owner natija chunta hai.

## AI order-taking ka asli raasta
1. **Abhi (verified design):** "ऑर्डर सहायक" — owner/staff ke saath. Awaaz `RecognizerIntent` (hi-IN) se, jawab/pushti text + TextToSpeech. Order sirf "हाँ" ke baad Confirmed.
2. **Baad mein:** approved telephony provider (cloud calling API) jahan two-way audio aadhikarik roop se milta hai — tab AI provider backend se judega.
Real-device test natija: _abhi koi device test nahi hua._
