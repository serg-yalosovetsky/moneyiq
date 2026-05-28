# MoneyIQ

MoneyIQ is a native Android personal finance app inspired by the 1Money experience. It is built with Kotlin and Jetpack Compose and focuses on fast daily expense tracking, category-first analytics, budgets, reports, local persistence, and home-screen widgets.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

## What It Does

- Tracks expenses, income, transfers, debts, and repayments.
- Organizes operations by accounts and categories.
- Shows category spending with a central donut chart and quick category chips.
- Supports monthly budgets and budget progress screens.
- Provides transaction search and category-based filtering.
- Includes overview and report screens for financial summaries.
- Stores data locally with Room.
- Persists settings with DataStore.
- Includes Glance widgets for quick expense entry and balance display.
- Supports biometric permission hooks and notification/background worker setup.

## Tech Stack

| Layer | Tools |
| --- | --- |
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose, Material 3 |
| Architecture | ViewModels, repositories, Hilt dependency injection |
| Database | Room 2.7.0 |
| Settings | AndroidX DataStore |
| Background work | WorkManager |
| Widgets | AndroidX Glance |
| Charts | MPAndroidChart |
| Build | Android Gradle Plugin 8.7.3, KSP |

## Project Layout

```text
.
├── moneyiq/                  # Main Android project
│   ├── app/                  # Application module
│   │   └── src/main/java/org/pixelrush/moneyiq/
│   │       ├── data/         # Room entities, DAOs, repositories
│   │       ├── di/           # Hilt modules
│   │       ├── ui/           # Compose screens and widgets
│   │       ├── util/         # Import/export and category helpers
│   │       └── workers/      # Background sync/backup workers
│   ├── gradle/               # Version catalog and Gradle wrapper files
│   └── import/               # Import schema and sample data
├── apks_extracted/           # Extracted reference APK artifacts
├── 1money.apk                # Reference APK
└── 1Money_3.6.1.apks         # Reference APK bundle
```

## Requirements

- Android Studio with JDK 17.
- Android SDK with compile SDK 36.
- Gradle 8.9 or compatible local Gradle installation.

The repository currently contains Gradle wrapper metadata but no `gradlew` launcher script. Open `moneyiq/` in Android Studio, or run Gradle with an installed/wrapper Gradle binary available on your machine.

## Build

From the Android project directory:

```powershell
cd moneyiq
gradle app:compileDebugKotlin
```

If you use the locally cached Gradle distribution on this machine:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio2\jbr'
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" app:compileDebugKotlin
```

For a debug APK:

```powershell
cd moneyiq
gradle app:assembleDebug
```

## Testing

### Run unit tests

```powershell
cd moneyiq
gradle :app:testDebugUnitTest
```

Or with the locally cached Gradle binary:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio2\jbr'
$gradle = "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat"
& $gradle :app:testDebugUnitTest --no-daemon
```

HTML report is written to:

```
moneyiq/app/build/reports/tests/testDebugUnitTest/index.html
```

### Run instrumented tests (requires emulator or device)

```powershell
gradle :app:connectedDebugAndroidTest
```

### Code coverage report

Coverage is collected via JaCoCo, which is built into the Android Gradle Plugin. The `debug` build type has `enableUnitTestCoverage = true` already configured.

```powershell
gradle :app:createDebugUnitTestCoverageReport
```

HTML report is written to:

```
moneyiq/app/build/reports/coverage/test/debug/index.html
```

Open `index.html` in a browser to see per-class and per-package coverage percentages. The current suite covers repositories, utilities, ViewModels, and calculator logic — roughly the business-logic layer.

## Current Verification

The Kotlin debug compilation has been verified with:

```powershell
app:compileDebugKotlin
```

The build completes successfully. Existing warnings are mostly deprecations and an Android Gradle Plugin warning for `compileSdk = 36`.

## Notes

This repository includes extracted APK/reference folders used while recreating the app experience. The source app lives in `moneyiq/`; treat the root APK artifacts and extraction folders as supporting material, not production source.

## License

No license file is currently included.
