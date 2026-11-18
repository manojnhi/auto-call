# Architecture

```
app/src/main/java/com/gramcall/ai/
  database/    Room entities, DAOs, migrations (v3)
  repository/  GramRepository (gaon/shehar/grahak/campaign), BizRepository (product/order/report/settings), BackupManager
  biz/         Pure Kotlin logic: OrderCalc, HindiOrderParser, DateRanges, ReportCalc, Units, Scripts/Invoice
  calling/     SimManager (SIM 1), CallStateMonitor, CampaignLogic/rules
  security/    SecureStore (EncryptedSharedPreferences, PBKDF2 PIN)
  network/     CloudClient (HTTPS, backend ke liye)
  features/    Compose screens (dashboard, villages, cities, customers, calling, products, orders, reports, ai, settings, gate)
  ui/          Theme, AppRoot (navigation), ViewModels
backend/       Node + Express + PostgreSQL (auth + snapshot backup)
```

- **Offline-first:** sab data Room mein; internet sirf optional cloud backup ke liye.
- **Order saving:** `BizRepository.saveOrder` ek transaction mein order + items + stock badlav.
  Order mein customer/gaon/product ka snapshot rehta hai, isliye baad mein delete hone par history nahi bigadti.
- **Campaign:** DB mein campaign + har customer ki `call_history` row (PENDING). App restart par wahin se. Ek time par ek call, har call ek tap se.
- **AI provider (pluggable):** abhi `HindiOrderParser` (rule-based, on-device) sirf product list se milata hai. Remote LLM jodna ho to `parseOrder` ke peeche
  ek interface lagakar backend endpoint se bulao — API key sirf server par rahegi, APK mein kabhi nahi.
- **Sync:** abhi snapshot backup (version check se conflict 409). Row-level sync nahi hai.
