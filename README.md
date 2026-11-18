# GramCall AI

Kirana dukaan ke liye Android app (Kotlin, Jetpack Compose, Room) + optional Node/Express backend.
Package: `com.gramcall.ai` · Database version 3 · Hindi UI.

> **Status (sach):** poora source likha gaya hai, lekin Android code ka build aur tests abhi tak kisi machine par
> chalaye nahi gaye (sirf backend ke 6 tests chale, pass). Pehla GitHub Actions run compile errors dikha sakta hai —
> wo error bhejo, theek ho jayega. Real-phone calling test bhi baaki hai.

## Kya-kya hai
- **Dashboard:** 10 cards (ग्राहक, गाँव, शहर, आज की कॉल, सफल, नो आंसर, आज के ऑर्डर, बिक्री, पेंडिंग, पूरे हुए)
- **गाँव / शहर / ग्राहक:** add-edit-delete-search, इलाके, CSV import/export, duplicate number rejection, calling consent + opt-out + consent log
- **SIM 1 कॉलिंग:** SIM 1 detect, permissions explain, sirf SIM 1 se call, call history
- **कॉल अभियान (START VILLAGE CALL):** sirf sahmati wale, 24h no-answer/reject skip, 9am-9pm, ek time par ek call (har call ek tap),
  Pause/Resume/Skip/Stop, natija chunna, app restart ke baad wapas, summary + ऑर्डर गिनती
- **प्रोडक्ट:** 12 default श्रेणियाँ, units, price, stock, low-stock alert, aliases
- **ऑर्डर:** banao/edit/cancel, status, discount, stock auto-adjust (transaction), invoice share/print, CSV export
- **AI ऑर्डर सहायक:** bolkar (Android voice input) ya likhkar ग्राहक की बात -> सामान/मात्रा/कीमत (sirf aapke products aur prices),
  na samajh aaye to "जाँचें" flag, **ऑर्डर "कन्फ़र्म" tab hi jab aap ग्राहक की हाँ ke baad button dabayein**
- **रिपोर्ट:** आज/कल/7/30 दिन/इस महीने/custom, गाँव-वार report, CSV export
- **सेटिंग्स / सुरक्षा:** दुकान जानकारी, PIN lock (PBKDF2, encrypted storage), logout, backup/restore (JSON), cloud snapshot backup
- **Splash / Onboarding / Lock** screens

## Jo nahi hai (jaan-boojhkar)
- **AI se do-tarfa baat SIM call par:** Android normal call ki awaaz tak app ko access nahi deta. `docs/CALLING_FEASIBILITY.md`
- Ringing/Connected/Busy/No-answer apne-aap pata nahi chalta (bina call-log/dialer role) — har call ke baad aap natija chunte ho
- DND/NCPR list ki jaanch: app nahi karta — number scrub karna aapki zimmedari hai
- Live row-level sync: abhi sirf snapshot backup (version conflict 409)
- Direct .xlsx import (CSV UTF-8 use karo)
- Custom app icon, Room migration tests (schema JSON build ke baad)

## GitHub par APK banana
`docs/INSTALLATION.md` dekho. Short: repo push karo -> **Actions > Android CI** -> artifact **GramCall-AI-debug-apk**.

## Local build
Android Studio Jellyfish+ (JDK 17) -> folder open -> sync. `gradlew` repo mein nahi hai: `gradle wrapper --gradle-version 8.7` ek baar chalao.

## Tests
- `./gradlew testDebugUnitTest` — phone/CSV utils, order calc, Hindi parser, date ranges, report calc, campaign sequencing, calling hours
- `./gradlew connectedDebugAndroidTest` — Room (customer/village/duplicate, product/order/stock, campaign flow, opt-out, backup round-trip)
- `cd backend && npm test` — backend validators/config (6/6 pass, chalaye gaye)
- Manual (real phone): `docs/TESTING.md`

## Docs
`docs/` : INSTALLATION · ARCHITECTURE · CALLING_FEASIBILITY · API_DOCUMENTATION · PRIVACY · TESTING
