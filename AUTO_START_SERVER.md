# Auto-Start API Server with App

## 🚀 Multiple Ways to Start Everything Together

I've created **3 convenient methods** to automatically start the API server when you launch the app!

---

## Method 1: One-Click Script (Recommended for Windows) ⭐

### Windows:
Double-click `start-dev.bat` or run in terminal:
```bash
.\start-dev.bat
```

### Mac/Linux:
```bash
./start-dev.sh
```

### What It Does:
1. ✅ Checks if Node.js is installed
2. ✅ Checks if `.env` file exists (creates from template if not)
3. ✅ Installs npm dependencies if needed
4. ✅ Starts API server in a separate window
5. ✅ Checks for connected device/emulator
6. ✅ Builds and installs the Android app
7. ✅ Launches the app
8. ✅ Keeps server running until you press a key

**Perfect for**: Quick development sessions, demos, testing

---

## Method 2: Gradle Task (Best for Terminal/Command Line)

### Start Everything:
```bash
./gradlew runWithServer
```

### What It Does:
1. ✅ Starts API server in background
2. ✅ Builds and installs the app
3. ✅ Shows success message

### Stop the Server:
```bash
./gradlew stopApiServer
```

### Available Tasks:
```bash
# Start API server only
./gradlew startApiServer

# Stop API server
./gradlew stopApiServer

# Start server + install app
./gradlew runWithServer

# List all development tasks
./gradlew tasks --group development
```

**Perfect for**: Command-line workflows, CI/CD, automation

---

## Method 3: Android Studio Run Configuration

### Setup (One-Time):

1. **Open Run Configurations**
   - Click `Run` → `Edit Configurations...`
   - Or click dropdown next to Run button → `Edit Configurations...`

2. **Add Gradle Task** (For Server)
   - Click `+` → `Gradle`
   - Name: `Start API Server`
   - Gradle project: `TrailGuide_Android`
   - Tasks: `startApiServer`
   - Click `OK`

3. **Modify App Configuration** (For App + Server)
   - Select `app` configuration
   - Click `+` in "Before launch" section
   - Select `Run Gradle task`
   - Choose `startApiServer`
   - Click `OK`

### Usage:
Now when you click **Run ▶️** in Android Studio, it will:
1. ✅ Start the API server
2. ✅ Build and install the app
3. ✅ Launch the app

**Perfect for**: Regular development in Android Studio

---

## Comparison

| Method | Pros | Cons | Best For |
|--------|------|------|----------|
| **Script** | Easy, all-in-one, checks dependencies | Requires separate window | Quick starts, demos |
| **Gradle Task** | Clean, automated, scriptable | Less user-friendly output | Terminal users, automation |
| **Android Studio** | Integrated, one-click in IDE | One-time setup required | Daily development |

---

## 🛠️ Prerequisites

### All Methods Require:
- ✅ **Node.js** installed ([Download](https://nodejs.org/))
- ✅ **npm** (comes with Node.js)
- ✅ **api-proxy/.env** file configured

### First-Time Setup:

1. **Install Node.js dependencies:**
   ```bash
   cd api-proxy
   npm install
   cd ..
   ```

2. **Configure `.env` file:**
   ```bash
   cd api-proxy
   cp .env_template .env
   # Edit .env with your Supabase credentials
   ```

3. **Test the server manually:**
   ```bash
   cd api-proxy
   npm start
   # Should show: "API Proxy running on http://localhost:3000"
   ```

---

## 📋 Quick Start Examples

### Example 1: Using the Script (Windows)
```bash
# Just double-click start-dev.bat
# Or in PowerShell/CMD:
.\start-dev.bat

# The script will:
# - Open API server in a new window
# - Build and install the app
# - Launch the app
# - Wait for you to press a key to stop
```

### Example 2: Using Gradle
```bash
# Terminal/Command Prompt
cd "c:\Users\v6dri\OneDrive\Uni\Y3\SEM 2\PROF3D\TrailGuide_Android"

# Start server and install app
./gradlew runWithServer

# When done, stop the server
./gradlew stopApiServer
```

### Example 3: Using Android Studio
```
1. Open Android Studio
2. Configure run configuration (see Method 3 above)
3. Click Run ▶️ button
4. Everything starts automatically!
```

---

## 🧪 Verify Everything Works

### Test 1: Check Server is Running
Open browser: http://localhost:3000/api/trails

You should see JSON response with trails data.

### Test 2: Check App Connects to Server
1. Open the app on emulator/device
2. Go to **Trails** screen
3. You should see trails loaded from the server

### Test 3: Check Logs
```bash
# Server logs (in the API server window)
# Should show: GET /api/trails 200

# App logs
adb logcat | grep -i "TrailApiService\|Retrofit"
# Should show successful API calls
```

---

## 🐛 Troubleshooting

### Issue: "Node.js is not installed"
**Solution**: 
```bash
# Download and install from:
https://nodejs.org/

# Verify installation:
node --version
npm --version
```

### Issue: "Port 3000 is already in use"
**Solution**:
```bash
# Windows: Kill the process
netstat -ano | findstr :3000
taskkill /PID [PID_NUMBER] /F

# Mac/Linux: Kill the process
lsof -ti:3000 | xargs kill -9
```

### Issue: "Cannot find module" errors
**Solution**:
```bash
# Reinstall dependencies
cd api-proxy
rm -rf node_modules
npm install
```

### Issue: ".env file not found"
**Solution**:
```bash
cd api-proxy
cp .env_template .env
# Edit .env and add your Supabase credentials
```

### Issue: "App can't connect to server"
**Solution**:
1. Verify server is running: `http://localhost:3000/api/trails`
2. Check `app/build.gradle.kts` has correct API URL:
   - Emulator: `http://10.0.2.2:3000/`
   - Physical device: `http://YOUR_LOCAL_IP:3000/`
3. Check firewall isn't blocking port 3000

### Issue: Script fails on Mac/Linux
**Solution**:
```bash
# Make script executable
chmod +x start-dev.sh

# Run with bash
bash start-dev.sh
```

---

## 🔧 Advanced Configuration

### Change API Server Port

1. **Edit `api-proxy/server.js`:**
   ```javascript
   const PORT = process.env.PORT || 4000; // Change from 3000
   ```

2. **Edit `api-proxy/.env`:**
   ```
   PORT=4000
   ```

3. **Edit `app/build.gradle.kts`:**
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:4000/\"")
   ```

### Run Server in Production Mode

```bash
cd api-proxy
npm start -- --production
```

### View Server Logs

```bash
# If server is in background
cd api-proxy
npm run logs  # (if you add this script to package.json)

# Or check the separate window/terminal where server is running
```

---

## 📊 What's Running?

After starting with any method:

```
┌─────────────────────────────────────────┐
│  API Server (Node.js)                   │
│  Port: 3000                             │
│  URL: http://localhost:3000             │
│  Status: Running in separate window     │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Android App                            │
│  Device: Pixel_8 (Emulator)            │
│  Package: com.trailguide.android        │
│  Status: Running and connected          │
└─────────────────────────────────────────┘
```

---

## 🎯 Recommended Workflow

### Daily Development:
```bash
1. Open Android Studio
2. Run configured setup (Method 3)
3. Start coding!
4. Server runs in background
5. App auto-reloads on changes
```

### Quick Testing:
```bash
1. Double-click start-dev.bat
2. Wait for app to launch
3. Test your changes
4. Press any key to stop
```

### Command-Line Development:
```bash
1. ./gradlew runWithServer
2. Code and rebuild as needed
3. ./gradlew stopApiServer when done
```

---

## 📁 Files Created

| File | Purpose |
|------|---------|
| `start-dev.bat` | Windows startup script |
| `start-dev.sh` | Mac/Linux startup script |
| `app/build.gradle.kts` | Gradle tasks added |
| `AUTO_START_SERVER.md` | This guide |

---

## ✅ Success Checklist

- [ ] Node.js installed (`node --version` works)
- [ ] Dependencies installed (`api-proxy/node_modules` exists)
- [ ] `.env` file configured (`api-proxy/.env` with Supabase credentials)
- [ ] Server starts manually (`cd api-proxy && npm start` works)
- [ ] Chosen your preferred method (Script, Gradle, or Android Studio)
- [ ] Tested and verified server + app work together

---

## 🎉 You're All Set!

Now you can start both the server and app with one command!

**Try it now:**
```bash
# Windows
.\start-dev.bat

# Or Gradle
./gradlew runWithServer

# Or click Run in Android Studio
```

---

**Last Updated**: October 2025  
**Need Help?** Check [README_RUN_APP.md](README_RUN_APP.md) or [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md)

