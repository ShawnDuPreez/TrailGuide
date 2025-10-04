@echo off
REM Build Release APK for Testing
REM This script reminds you to deploy the API server first

echo ========================================
echo   Build Release APK for Testing
echo ========================================
echo.

echo IMPORTANT: Before building the APK for testers,
echo make sure your API server is deployed to the cloud!
echo.
echo Recommended: Deploy to Railway (https://railway.app)
echo.

set /p DEPLOYED="Has your API server been deployed? (y/n): "

if /i "%DEPLOYED%" NEQ "y" (
    echo.
    echo Please deploy your API server first:
    echo 1. Sign up at https://railway.app
    echo 2. Deploy the api-proxy folder
    echo 3. Get your Railway URL
    echo 4. Update build.gradle.kts with the URL
    echo.
    echo Then run this script again.
    pause
    exit /b 1
)

echo.
set /p API_URL="Enter your deployed API URL (e.g., https://your-app.railway.app/): "

if "%API_URL%"=="" (
    echo Error: API URL cannot be empty!
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Updating API URL
echo ========================================
echo.

echo Current URL will be set to: %API_URL%
echo.
echo NOTE: You need to manually update this in:
echo   app/build.gradle.kts
echo   Look for: buildConfigField("String", "API_BASE_URL", ...)
echo   Change it to: buildConfigField("String", "API_BASE_URL", "\"%API_URL%\"")
echo.

set /p UPDATED="Have you updated build.gradle.kts with this URL? (y/n): "

if /i "%UPDATED%" NEQ "y" (
    echo.
    echo Please update build.gradle.kts first, then run this script again.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Building Release APK
echo ========================================
echo.

call gradlew clean
call gradlew assembleRelease

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   SUCCESS!
    echo ========================================
    echo.
    echo APK Location: app\build\outputs\apk\release\app-release.apk
    echo API URL: %API_URL%
    echo.
    echo The APK is ready for testing!
    echo.
    echo To distribute:
    echo 1. Upload APK to Google Drive, Dropbox, or Firebase
    echo 2. Share link with testers
    echo 3. Testers just install and run - no server setup needed!
    echo.
    echo Opening APK folder...
    start "" "app\build\outputs\apk\release"
) else (
    echo.
    echo Build failed! Please check the error messages above.
)

echo.
pause

