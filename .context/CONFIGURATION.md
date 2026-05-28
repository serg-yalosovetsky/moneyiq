# Configuration

## Project Entry

Open or build from:

```text
moneyiq/
```

Gradle root:

```text
moneyiq/settings.gradle.kts
```

App module:

```text
moneyiq/app
```

## Android Config

From `app/build.gradle.kts`:

- namespace: `org.pixelrush.moneyiq`
- applicationId: `org.pixelrush.moneyiq`
- minSdk: `26`
- targetSdk: `36`
- compileSdk: `36`
- versionName: `1.0.0`
- versionCode: `1`

## Permissions

Declared in `AndroidManifest.xml`:

- `USE_BIOMETRIC`
- `RECEIVE_BOOT_COMPLETED`
- `POST_NOTIFICATIONS`
- `INTERNET`

## Local Build Notes

`moneyiq/gradlew.bat` exists and works. Set `JAVA_HOME` before invoking it:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio2\jbr'
Set-Location 'G:\code\one-money-clone\moneyiq'
& '.\gradlew.bat' ':app:compileDebugKotlin'          # compile check
& '.\gradlew.bat' ':app:testDebugUnitTest' '--no-daemon'   # unit tests
& '.\gradlew.bat' ':app:assembleDebug' '--no-daemon'       # build APK
```

Convenience scripts at the repo root: `build-apk.bat`, `test.bat` — both set `JAVA_HOME` automatically.

## Sentry Auth Token

The Sentry Gradle plugin uploads ProGuard mappings during release builds and requires an auth token. It is **never committed to VCS**.

Add to `moneyiq/local.properties` (gitignored):

```properties
sentry.auth.token=<your token from sentry.io Settings → Auth Tokens>
```

For CI set the `SENTRY_AUTH_TOKEN` environment variable. The `app/build.gradle.kts` reads `local.properties` first, then falls back to the env var.

DSN (safe to commit — not a secret):
```
https://8f8838dbabb042f825cb7b96f1a8f6d6@o4504272346480640.ingest.us.sentry.io/4511470109720576
```
Org: `serg-yalosovetsky`, project: `one_money`.

## Known Build Warning

Android Gradle Plugin `8.7.3` warns that it was tested up to compile SDK 35 while this app uses compile SDK 36. This is a warning only; builds succeed. Suppress with `android.suppressUnsupportedCompileSdk=36` in `gradle.properties` if needed.
