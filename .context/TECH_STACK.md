# Tech Stack

## Platform

- Android native app.
- Minimum SDK: 26.
- Target SDK: 36.
- Compile SDK: 36.
- Java/Kotlin JVM target: 17.

## Language And Build

- Kotlin `2.1.0`.
- Android Gradle Plugin `8.7.3`.
- KSP `2.1.0-1.0.29`.
- Gradle version catalog: `moneyiq/gradle/libs.versions.toml`.

## UI

- Jetpack Compose with Material 3.
- Material icons extended are used heavily.
- Navigation Compose powers app navigation.
- Glance powers app widgets.

## Data And DI

- Room `2.7.0` for local persistence.
- Hilt `2.54` for dependency injection.
- DataStore Preferences for settings (`SettingsRepository`).
- Kotlin coroutines and Flow for reactive reads.

## Background Work And Extras

- WorkManager for background work (`NotificationWorker`, `DriveBackupWorker`, `MonoFlowSyncWorker`).
- MPAndroidChart is available for charts.
- AndroidX Biometric (`BiometricPrompt` in `MainActivity`, triggers after 30s in background).

## Crash Reporting

- Sentry Android SDK `7.20.0` (`io.sentry:sentry-android`).
- Gradle plugin `io.sentry.android.gradle:4.14.1` — uploads ProGuard mappings and source context on release builds.
- `AndroidManifest.xml` contains `io.sentry.auto-init=false` — disables `SentryInitProvider` (the ContentProvider that fires before `Application.onCreate()`). Without this the app crashes if no DSN is in the manifest.
- Initialized in `MoneyIQApp.onCreate` via `SentryAndroid.init` with: DSN hardcoded, `isDebug = BuildConfig.DEBUG`, `environment = "debug"|"production"`, `release = "moneyiq@<versionName>"`, `tracesSampleRate = 1.0`, `attachScreenshot = true`, `attachViewHierarchy = true`, `enableUserInteractionTracing = true`.
- Auth token for the build plugin is read from `local.properties` (`sentry.auth.token`) with `SENTRY_AUTH_TOKEN` env-var fallback for CI. Never commit the token.
- `includeSourceContext` is set to `sentryToken.isNotEmpty()` — if the token is absent (e.g. CI without the secret), source upload is skipped and the build still succeeds.

## Testing

- JUnit 4.13.2 — test runner.
- MockK 1.13.13 — mocking.
- Turbine 1.2.0 — Flow testing.
- Truth 1.4.4 — assertions.
- kotlinx-coroutines-test 1.10.1 — `UnconfinedTestDispatcher`, `runTest`.
- androidx.arch.core:core-testing 2.2.0 — `InstantTaskExecutorRule`.
- Robolectric 4.14.1 — Android context in JVM tests.
- Room testing 2.7.0 — in-memory database.
- org.json:json:20231013 — BackupSerializer tests (org.json unavailable in pure JVM without this).
- `util/MainDispatcherRule.kt` — shared JUnit rule that sets `UnconfinedTestDispatcher` as `Dispatchers.Main`.

## Conventions

- Prefer Compose UI changes in the owning screen package.
- Keep repository methods as the only place that mutates cross-entity state such as account balances.
- Add Room migrations for schema changes; do not silently rely on destructive migration for user data.
- Use existing icon keys and `CategoryStyleUtil` before inventing new category styling logic.
- Calculator/date-picker components must be imported from `ui.components.calculator`, not from `ui.categories`.
