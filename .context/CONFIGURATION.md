# Configuration

## Project Entry

Open or build from:

```text
onemoney/
```

Gradle root:

```text
onemoney/settings.gradle.kts
```

App module:

```text
onemoney/app
```

## Android Config

From `app/build.gradle.kts`:

- namespace: `org.syalosovetskyi.onemoney`
- applicationId: `org.syalosovetskyi.onemoney`
- minSdk: `26`
- targetSdk: `36`
- compileSdk: `36`
- versionName: `1.0.2`
- versionCode: `3`

## Permissions

Declared in `AndroidManifest.xml`:

- `USE_BIOMETRIC`
- `RECEIVE_BOOT_COMPLETED`
- `POST_NOTIFICATIONS`
- `INTERNET`

## Local Build Notes

`onemoney/gradlew.bat` exists and works. Set `JAVA_HOME` before invoking it:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio2\jbr'
Set-Location 'G:\code\one-money-clone\onemoney'
& '.\gradlew.bat' ':app:compileDebugKotlin'          # compile check
& '.\gradlew.bat' ':app:testDebugUnitTest' '--no-daemon'   # unit tests
& '.\gradlew.bat' ':app:assembleDebug' '--no-daemon'       # build APK
```

Convenience scripts at the repo root: `build-apk.bat`, `test.bat` — both set `JAVA_HOME` automatically.

## local.properties Keys

`onemoney/local.properties` is gitignored and holds all local secrets:

```properties
glitchtip.dsn=<DSN проекта mesh/onemoney на glitchtip.ibotz.fun>
monoflow.url=<MonoFlow sync base URL, e.g. https://mono.example.com>
monoflow.token=<MonoFlow Bearer token>
signing.storeFile=<absolute path to .keystore>
signing.storePassword=<keystore password>
signing.keyAlias=<key alias>
signing.keyPassword=<key password>
```

`build.gradle.kts` reads each key and falls back to the corresponding environment variable:
- `glitchtip.dsn` → `GLITCHTIP_DSN`
- Signing keys → `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`

`monoflow.url` and `monoflow.token` become `BuildConfig.DEBUG_MONOFLOW_URL` / `DEBUG_MONOFLOW_TOKEN` (empty string in release builds).

## Приёмник падений (GlitchTip)

Падения уезжают в СВОЙ GlitchTip: `https://glitchtip.ibotz.fun`, организация `mesh`,
проект `onemoney` (id 24). Публичный sentry.io больше не используется — ни приложением,
ни сборкой (ADR-075, serg/tasks#679).

DSN задаётся **только** через `glitchtip.dsn` в `local.properties` или переменную
`GLITCHTIP_DSN` (в CI — секрет репозитория), попадает в код как `BuildConfig.GLITCHTIP_DSN`.
В git его нет.

**Пустой DSN = отправка выключена целиком**: `MoneyIQApp` не инициализирует Sentry и
пишет об этом в лог. Сборка при этом успешна, и по самому APK этого не видно — значит,
секрет `GLITCHTIP_DSN` обязан быть у КАЖДОГО пайплайна, который публикует APK.

**Вложения выключены и включать их нельзя** без отдельного решения Сержа:
`isAttachScreenshot`, `isAttachViewHierarchy`, `isSendDefaultPii`,
`isEnableUserInteractionTracing` = `false`. Скриншот увозит не текст ошибки, а балансы,
а UI GlitchTip открыт по домену без SSO.

**Выгрузка символов на сборке выключена** (`includeSourceContext`,
`autoUploadProguardMapping`, `uploadNativeSymbols`, `telemetry` = `false`): раньше
плагин заливал в публичный sentry.io mapping и ИСХОДНИКИ. Цена — обфусцированные
стектрейсы релиза. Возвращать выгрузку можно только на свой приёмник.
`SENTRY_AUTH_TOKEN` не нужен и в пайплайнах не передаётся.

**Important:** `AndroidManifest.xml` has `io.sentry.auto-init=false`. Do not remove it — without it the Sentry `SentryInitProvider` ContentProvider crashes on startup when the DSN is not in the manifest. Sentry is initialized manually in `onemoneyApp.onCreate()`.

## Claude Code Integration

Project-level Claude Code settings live in `.claude/settings.json` (tracked in git).

**PostToolUse hook — auto Kotlin compile:** After every `Edit` or `Write` of a `.kt` file, the hook automatically runs `.\gradlew :app:compileDebugKotlin`. This catches compile errors immediately after each file change without requiring a manual build.

The hook reads the tool input JSON from stdin to get `file_path`, checks for `.kt` extension, then compiles:

```powershell
$json = [Console]::In.ReadToEnd() | ConvertFrom-Json
$f = $json.tool_input.file_path
if ($f -like '*.kt') {
    Set-Location 'G:\code\one-money-clone\onemoney'
    .\gradlew :app:compileDebugKotlin 2>&1 | Select-Object -Last 6
}
```

**Note:** This hook is project-local — it only activates in the `G:\code\one-money-clone` project context. It assumes `JAVA_HOME` is set (Android Studio sets it automatically).

## CI/CD (GitHub Actions)

One workflow: `.github/workflows/build.yml`

| Trigger | Jobs run |
|---|---|
| push to `main` or PR | `test` (unit tests only) |
| tag `v*.*.*` | `test` → `release` (signed APK + GitHub Release) |

GitHub Actions secrets required for release:
| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `STORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias in the keystore |
| `KEY_PASSWORD` | Key password |
| `GLITCHTIP_DSN` | Приёмник падений. **Без него опубликованный APK не шлёт падений вообще**, и сборка об этом не сообщает |

**Important:** `gradlew` already has the executable bit in git (`100755`). Each CI job also runs `chmod +x gradlew` as a safety step.

**Приёмник на CI:** `GLITCHTIP_DSN` передаётся в шаг сборки релиза. Пустой DSN сборку НЕ ломает — APK соберётся и опубликуется молча немым, узнать об этом можно только по строке в логе устройства. Поэтому секрет обязателен для каждого пайплайна, публикующего APK. `SENTRY_AUTH_TOKEN` больше не используется: выгрузка символов выключена в `build.gradle.kts`.
