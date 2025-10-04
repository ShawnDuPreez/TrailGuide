#!/bin/bash
# TrailGuide Development Startup Script (Mac/Linux)
# This script starts both the API server and the Android app

echo "========================================"
echo "  TrailGuide Development Environment"
echo "========================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}[ERROR]${NC} Node.js is not installed!"
    echo "Please install Node.js from https://nodejs.org/"
    exit 1
fi

# Check if api-proxy directory exists
if [ ! -d "api-proxy" ]; then
    echo -e "${RED}[ERROR]${NC} api-proxy directory not found!"
    echo "Please ensure you're running this script from the project root."
    exit 1
fi

# Check if .env file exists
if [ ! -f "api-proxy/.env" ]; then
    echo -e "${YELLOW}[WARNING]${NC} api-proxy/.env not found!"
    echo "Creating from template..."
    cp "api-proxy/.env_template" "api-proxy/.env"
    echo ""
    echo -e "${YELLOW}[ACTION REQUIRED]${NC} Please edit api-proxy/.env with your Supabase credentials"
    echo "Opening .env file..."
    
    # Try to open with appropriate editor
    if command -v code &> /dev/null; then
        code "api-proxy/.env"
    elif command -v nano &> /dev/null; then
        nano "api-proxy/.env"
    else
        echo "Please edit api-proxy/.env manually"
    fi
    
    echo ""
    read -p "Press Enter after you've configured the .env file..."
fi

# Navigate to api-proxy and check dependencies
cd api-proxy

if [ ! -d "node_modules" ]; then
    echo -e "${GREEN}[INFO]${NC} Installing API server dependencies..."
    npm install
    if [ $? -ne 0 ]; then
        echo -e "${RED}[ERROR]${NC} Failed to install dependencies!"
        exit 1
    fi
fi

echo ""
echo "========================================"
echo "  Starting API Server"
echo "========================================"
echo "Server will run on http://localhost:3000"
echo ""

# Start the server in background
npm start &
SERVER_PID=$!

# Save PID for cleanup
echo $SERVER_PID > /tmp/trailguide-server.pid

# Wait for server to start
sleep 3

cd ..

echo ""
echo "========================================"
echo "  Starting Android App"
echo "========================================"
echo ""

# Check if emulator is running
if ! adb devices | grep -q "device$"; then
    echo -e "${YELLOW}[WARNING]${NC} No Android device/emulator detected!"
    echo "Please start your emulator or connect a device."
    echo ""
    echo "Starting emulator..."
    emulator -avd Pixel_8 &
    echo "Waiting for emulator to boot..."
    sleep 10
fi

# Build and install the app
echo -e "${GREEN}[INFO]${NC} Building and installing app..."
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "  SUCCESS!"
    echo "========================================"
    echo ""
    echo -e "${GREEN}API Server:${NC} Running at http://localhost:3000 (PID: $SERVER_PID)"
    echo -e "${GREEN}Android App:${NC} Installed and ready"
    echo ""
    echo "Starting the app..."
    adb shell am start -n com.trailguide.android/.presentation.MainActivity
    echo ""
    echo "========================================"
    echo "  Development Environment Ready!"
    echo "========================================"
    echo ""
    echo "- API Server is running in the background"
    echo "- Android app is running on your device/emulator"
    echo "- Check Logcat in Android Studio for app logs"
    echo ""
    echo "Press Ctrl+C to stop the API server and exit..."
    
    # Wait for Ctrl+C
    trap "echo ''; echo 'Stopping API server...'; kill $SERVER_PID 2>/dev/null; rm /tmp/trailguide-server.pid; exit" INT
    
    # Keep script running
    wait $SERVER_PID
else
    echo ""
    echo -e "${RED}[ERROR]${NC} Failed to build or install the app!"
    echo "Please check the error messages above."
    
    # Kill the server on error
    kill $SERVER_PID 2>/dev/null
    rm /tmp/trailguide-server.pid
    exit 1
fi

