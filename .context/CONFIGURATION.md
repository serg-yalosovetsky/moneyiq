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

## local.properties Keys

`moneyiq/local.properties` is gitignored and holds all local secrets:

```properties
sentry.auth.token=<token from sentry.io Settings → Auth Tokens>
monoflow.url=<MonoFlow sync base URL, e.g. https://mono.example.com>
monoflow.token=<MonoFlow Bearer token>
signing.storeFile=<absolute path to .keystore>
signing.storePassword=<keystore password>
signing.keyAlias=<key alias>
signing.keyPassword=<key password>
```

`build.gradle.kts` reads each key and falls back to the corresponding environment variable:
- `sentry.auth.token` → `SENTRY_AUTH_TOKEN`
- Signing keys → `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`

`monoflow.url` and `monoflow.token` become `BuildConfig.DEBUG_MONOFLOW_URL` / `DEBUG_MONOFLOW_TOKEN` (empty string in release builds).

## Sentry Auth Token

The Sentry Gradle plugin uploads ProGuard mappings during release builds and requires an auth token. It is **never committed to VCS**. Set via `sentry.auth.token` in `local.properties` or the `SENTRY_AUTH_TOKEN` env var (see above).

For CI set the `SENTRY_AUTH_TOKEN` environment variable. The `app/build.gradle.kts` reads `local.properties` first, then falls back to the env var.

DSN (safe to commit — not a secret):
```
https://8f8838dbabb042f825cb7b96f1a8f6d6@o4504272346480640.ingest.us.sentry.io/4511470109720576
```
Org: `serg-yalosovetsky`, project: `one_money`.

**Important:** `AndroidManifest.xml` has `io.sentry.auto-init=false`. Do not remove it — without it the Sentry `SentryInitProvider` ContentProvider crashes on startup when the DSN is not in the manifest. Sentry is initialized manually in `MoneyIQApp.onCreate()`.

## CI/CD (GitHub Actions)

Two workflows in `.github/workflows/`:

- **`build.yml`** — triggers on push to `main`, PRs, and version tags (`v*.*.*`). Runs unit tests, then builds a debug APK (push to main), or a signed release APK + GitHub Release (on tags).
- **`ci.yml`** — fast compile + unit test check on push/PRs.

GitHub Actions secrets required for release:
| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `STORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias in the keystore |
| `KEY_PASSWORD` | Key password |
| `SENTRY_AUTH_TOKEN` | ProGuard mapping upload |

**Important:** `gradlew` must have the executable bit in git. Fix with:
```bash
git update-index --chmod=+x moneyiq/gradlew
```
Without this, CI fails with `Permission denied: ./gradlew`.

## Known Build Warning

Android Gradle Plugin `8.7.3` warns that it was tested up to compile SDK 35 while this app uses compile SDK 36. This is a warning only; builds succeed. Suppress with `android.suppressUnsupportedCompileSdk=36` in `gradle.properties` if needed.
