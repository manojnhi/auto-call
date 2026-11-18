# Testing

## Automated
| Kya | Kahan | Kaise |
|---|---|---|
| Phone/CSV utils, order calc, Hindi parser, date ranges, report calc, campaign sequencing, calling hours | `app/src/test` | `./gradlew testDebugUnitTest` (GitHub Actions mein bhi) |
| Room: village/customer/duplicate phone, product, order save/edit/cancel + stock, campaign flow, opt-out, backup round-trip | `app/src/androidTest` | `./gradlew connectedDebugAndroidTest` (device/emulator chahiye) |
| Backend validators/config | `backend/tests` | `cd backend && npm test` |

Jo test aapke environment mein chalaya nahi gaya use "pass" na maanein. Abhi tak sirf backend tests chalaye gaye hain (6/6 pass).
Room migration tests (1→2, 2→3) tab likhenge jab `app/schemas/*.json` generate ho jayein.

## Manual (real phone par)
1. **Single SIM:** SIM Calling screen > SIM 1 mila? "तैयार" aaya? Test customer (apna dusra number, sahmati ON) ko 1-customer campaign chalao.
2. **Dual SIM:** SIM 1 aur SIM 2 dono active. Campaign call SIM 1 se hi jaye (phone ka call log dekho). Agar SIM 2 se gayi — call turant rokein aur batayein.
3. **SIM 1 outgoing:** "कॉल लगाएँ" → dialer khule, call lage; end karne par natija dialog aaye, duration dikhe.
4. **Busy:** dusre phone par call chalu rakho → natija "व्यस्त" chuno, history mein dikhe.
5. **Rejected / No answer:** call cut karo / uthao mat → "रिजेक्ट"/"नो आंसर"; usi customer par 24h mein naya campaign banao → "छोड़े जाएँगे" mein gine.
6. **Network failure:** airplane mode ON → SIM screen "एयरप्लेन मोड" bataye, call button par error.
7. **App restart mid-campaign:** call ke dauran ya beech mein app kill karo, dobara kholo → "अधूरा अभियान" dikhe, adhuri call par natija poocha jaye.
8. **Offline orders:** airplane mode mein customer jodo, order banao, report dekho — sab chalna chahiye.
9. **Backup/restore:** fayl backup banao, app data clear karo, restore karo → sab wapas.
10. **PIN:** PIN lagao, app band/khol → lock; 5 galat PIN → 30 sec ruk.
