@echo off
REM Fix OneDrive Build Lock Issues
REM Run this if you get AccessDeniedException

echo ========================================
echo   OneDrive Build Lock Fix
echo ========================================
echo.

echo This script will:
echo 1. Force delete build folders
echo 2. Clear Gradle cache
echo 3. Restart Gradle daemon
echo.

pause

echo [1/4] Stopping Gradle daemon...
call gradlew --stop

echo.
echo [2/4] Deleting build folders...
powershell -Command "Remove-Item -Path 'app\build' -Recurse -Force -ErrorAction SilentlyContinue"
powershell -Command "Remove-Item -Path 'build' -Recurse -Force -ErrorAction SilentlyContinue"
powershell -Command "Remove-Item -Path '.gradle' -Recurse -Force -ErrorAction SilentlyContinue"

echo.
echo [3/4] Cleaning project...
call gradlew clean

echo.
echo [4/4] Building project...
call gradlew assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   SUCCESS!
    echo ========================================
    echo Build completed successfully!
    echo.
    echo TIP: To prevent this issue:
    echo 1. Pause OneDrive sync while developing
    echo 2. Or move project out of OneDrive folder
    echo.
    echo See ONEDRIVE_FIX.md for more details.
) else (
    echo.
    echo ========================================
    echo   Build Still Failed
    echo ========================================
    echo.
    echo Try these steps:
    echo 1. Close Android Studio completely
    echo 2. Right-click OneDrive icon in taskbar
    echo 3. Select "Pause syncing" -^> "8 hours"
    echo 4. Run this script again
    echo.
)

echo.
pause

