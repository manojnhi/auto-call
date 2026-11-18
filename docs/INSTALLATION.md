# Installation

## A. GitHub par APK banana (sabse aasaan)
1. GitHub par naya (private) repo banao, phir apne computer par:
   ```
   git init && git add . && git commit -m "GramCall AI"
   git branch -M main
   git remote add origin https://github.com/<aap>/GramCall-AI.git
   git push -u origin main
   ```
2. Repo ke **Actions** tab mein "Android CI" chalega (har push par, ya "Run workflow" se).
3. Run khatam hone par neeche **Artifacts > GramCall-AI-debug-apk** download karo, zip kholo, `app-debug.apk` phone mein install karo (Unknown sources allow karna hoga).
4. Agar run lal (fail) ho: run kholo, error wali line copy karke bhejo — usi se fix hoga. Reports `build-reports` artifact mein bhi hain.

## B. Android Studio mein
1. Android Studio Jellyfish+ (JDK 17). Folder open karo, Gradle sync.
2. `gradlew` repo mein nahi hai. Terminal mein ek baar: `gradle wrapper --gradle-version 8.7` (ya Studio ka Gradle use karo).
3. `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
4. Tests: `./gradlew testDebugUnitTest` (JVM), `./gradlew connectedDebugAndroidTest` (phone/emulator).

## C. Signed release APK
1. Keystore banao: `keytool -genkey -v -keystore release.jks -alias gramcall -keyalg RSA -keysize 2048 -validity 10000`
2. **Local:** env vars set karke `KEYSTORE_PATH=/path/release.jks KEYSTORE_PASSWORD=... KEY_ALIAS=gramcall KEY_PASSWORD=... ./gradlew assembleRelease`
3. **GitHub:** repo Secrets mein `KEYSTORE_BASE64` (`base64 -w0 release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` daalo, Actions > "Release APK (signed)" > Run workflow.
4. Keystore aur uske password kabhi repo mein commit mat karna (.gitignore mein `*.jks` hai). Keystore kho gaya to update nahi de paoge.

## D. Backend (optional, sirf cloud backup ke liye)
```
cd backend
cp .env.example .env     # DATABASE_URL, JWT_SECRET (32+ akshar) bharo
npm install
npm run migrate
npm start
```
Server ko HTTPS ke peeche chalao (reverse proxy / hosting). App sirf `https://` URL maanta hai.
