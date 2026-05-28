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

The repository has Gradle wrapper metadata but no `gradlew.bat` launcher in `moneyiq/`.

Use Android Studio or an installed/cached Gradle binary. A known local command shape on this machine is:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio2\jbr'
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" app:compileDebugKotlin
```

## Known Build Warning

Android Gradle Plugin `8.7.3` warns that it was tested up to compile SDK 35 while this app uses compile SDK 36.
