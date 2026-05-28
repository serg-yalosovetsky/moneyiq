@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Android\Android Studio2\jbr"
set "PROJECT_DIR=%~dp0moneyiq"

echo Running unit tests...
echo.

cd /d "%PROJECT_DIR%"
call gradlew.bat :app:testDebugUnitTest --no-daemon %*

if %ERRORLEVEL% equ 0 (
    echo.
    echo All tests passed.
    echo Report: %PROJECT_DIR%\app\build\reports\tests\testDebugUnitTest\index.html
) else (
    echo.
    echo Tests FAILED. See report:
    echo %PROJECT_DIR%\app\build\reports\tests\testDebugUnitTest\index.html
    exit /b 1
)
