@echo off
REM TrailGuide Development Startup Script
REM This script starts both the API server and the Android app

echo ========================================
echo   TrailGuide Development Environment
echo ========================================
echo.

REM Check if Node.js is installed
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not installed!
    echo Please install Node.js from https://nodejs.org/
    pause
    exit /b 1
)

REM Check if api-proxy directory exists
if not exist "api-proxy" (
    echo [ERROR] api-proxy directory not found!
    echo Please ensure you're running this script from the project root.
    pause
    exit /b 1
)

REM Check if .env file exists
if not exist "api-proxy\.env" (
    echo [WARNING] api-proxy\.env not found!
    echo Creating from template...
    copy "api-proxy\.env_template" "api-proxy\.env"
    echo.
    echo [ACTION REQUIRED] Please edit api-proxy\.env with your Supabase credentials
    echo Opening .env file...
    start notepad "api-proxy\.env"
    echo.
    echo Press any key after you've configured the .env file...
    pause >nul
)

REM Navigate to api-proxy and check dependencies
cd api-proxy

if not exist "node_modules" (
    echo [INFO] Installing API server dependencies...
    call npm install
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Failed to install dependencies!
        cd ..
        pause
        exit /b 1
    )
)

echo.
echo ========================================
echo   Starting API Server
echo ========================================
echo Server will run on http://localhost:3000
echo.

REM Start the server in a new window
start "TrailGuide API Server" cmd /k "npm start"

REM Wait a moment for server to start
timeout /t 3 /nobreak >nul

cd ..

echo.
echo ========================================
echo   Starting Android App
echo ========================================
echo.

REM Check if emulator is running
adb devices 2>nul | find "device" >nul
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] No Android device/emulator detected!
    echo Please start your emulator or connect a device.
    echo.
    echo Starting emulator...
    REM Try to start default emulator
    emulator -avd Pixel_8 >nul 2>&1 &
    echo Waiting for emulator to boot...
    timeout /t 10 /nobreak >nul
)

REM Build and install the app
echo [INFO] Building and installing app...
call gradlew installDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   SUCCESS!
    echo ========================================
    echo.
    echo API Server: Running at http://localhost:3000
    echo Android App: Installed and ready
    echo.
    echo Starting the app...
    adb shell am start -n com.trailguide.android/.presentation.MainActivity
    echo.
    echo ========================================
    echo   Development Environment Ready!
    echo ========================================
    echo.
    echo - API Server console is open in a separate window
    echo - Android app is running on your device/emulator
    echo - Check Logcat in Android Studio for app logs
    echo.
    echo Press any key to stop the API server and exit...
    pause >nul
    
    REM Kill the API server window
    taskkill /FI "WindowTitle eq TrailGuide API Server*" /T /F >nul 2>&1
) else (
    echo.
    echo [ERROR] Failed to build or install the app!
    echo Please check the error messages above.
    pause
)

