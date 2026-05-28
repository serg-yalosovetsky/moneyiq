@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Android\Android Studio2\jbr"
set "PROJECT_DIR=%~dp0moneyiq"
set "APK_OUT=%PROJECT_DIR%\app\build\outputs\apk\debug\app-debug.apk"

echo Building debug APK...
echo.

cd /d "%PROJECT_DIR%"
call gradlew.bat :app:assembleDebug --no-daemon %*

if %ERRORLEVEL% equ 0 (
    echo.
    echo Build successful.
    echo APK: %APK_OUT%
) else (
    echo.
    echo Build FAILED.
    exit /b 1
)
