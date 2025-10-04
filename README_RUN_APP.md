# TrailGuide Android - Quick Start Guide

## 🚀 Running the App in Android Studio

This guide will help you get the TrailGuide Android app running in Android Studio.

---

## 📋 Prerequisites

### Required Software:
- ✅ **Android Studio** (Electric Eel or newer)
- ✅ **Java JDK** (11 or newer)
- ✅ **Android SDK** (API 26+)
- ✅ **Emulator** or physical Android device

### Required Accounts:
- ✅ **Supabase Account** - [Sign up here](https://app.supabase.com)
- ✅ **Google Cloud Console** (optional, for Google OAuth) - [Console](https://console.cloud.google.com)

---

## 📂 Step 1: Open Project in Android Studio

### Option A: Open Existing Project
1. Launch **Android Studio**
2. Click **"Open"** on the welcome screen
3. Navigate to: `C:\Users\v6dri\OneDrive\Uni\Y3\SEM 2\PROF3D\TrailGuide_Android`
4. Click **"OK"**

### Option B: From Command Line
```bash
# Navigate to project directory
cd "C:\Users\v6dri\OneDrive\Uni\Y3\SEM 2\PROF3D\TrailGuide_Android"

# Open in Android Studio
studio .
```

### Wait for Gradle Sync
- Android Studio will automatically sync Gradle dependencies
- This may take 2-5 minutes on first open
- Look for "Gradle sync finished" in the status bar

---

## ⚙️ Step 2: Configure Supabase

### 2.1 Get Your Supabase Credentials

1. Go to [Supabase Dashboard](https://app.supabase.com)
2. Select your TrailGuide project
3. Click **Settings** (⚙️) → **API**
4. Copy these values:
   - **Project URL** (e.g., `https://xxxxx.supabase.co`)
   - **anon/public key** (long string starting with `eyJ...`)

### 2.2 Update `build.gradle.kts`

Open `app/build.gradle.kts` and find the `buildConfigField` section:

```kotlin
buildConfigField("String", "SUPABASE_URL", "\"YOUR_SUPABASE_URL_HERE\"")
buildConfigField("String", "SUPABASE_KEY", "\"YOUR_SUPABASE_ANON_KEY_HERE\"")
```

Replace with your actual values:
```kotlin
buildConfigField("String", "SUPABASE_URL", "\"https://xxxxx.supabase.co\"")
buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"")
```

### 2.3 Configure Supabase Dashboard

**CRITICAL**: You MUST add these redirect URLs in Supabase:

1. Go to: **Authentication** → **URL Configuration** → **Redirect URLs**
2. Click **"Add URL"** and add:
   ```
   trailguide://auth-callback
   ```
3. Scroll down to **Site URL** and set it to:
   ```
   trailguide://auth-callback
   ```
4. Go to: **Authentication** → **Providers** → **Email**
5. Enable Email provider
6. Set **"Confirm email redirect URL"** to:
   ```
   trailguide://auth-callback
   ```
7. Click **Save**

**Optional - For Google Sign-In:**
1. Go to: **Authentication** → **Providers** → **Google**
2. Enable Google provider
3. Add your Google OAuth Client ID and Secret
4. Click **Save**

---

## 📱 Step 3: Set Up Emulator or Device

### Option A: Use Android Emulator

1. In Android Studio, click **Device Manager** (📱 icon on right sidebar)
2. Click **"Create Device"**
3. Select **"Pixel 8"** or any device with API 26+
4. Click **"Next"**
5. Select **Android 13 (API 33)** or newer
6. Click **"Next"** → **"Finish"**
7. Click the **▶ Play** button to start the emulator

### Option B: Use Physical Device

1. Enable **Developer Options** on your Android device:
   - Go to **Settings** → **About Phone**
   - Tap **Build Number** 7 times
2. Enable **USB Debugging**:
   - Go to **Settings** → **Developer Options**
   - Toggle **USB Debugging** ON
3. Connect your device via USB
4. Accept the **"Allow USB Debugging"** prompt on your device

---

## ▶️ Step 4: Run the App

### 🚀 NEW: Auto-Start Server + App (Recommended)

**Option 1: One-Click Script**
```bash
# Windows: Double-click or run
.\start-dev.bat

# Mac/Linux
./start-dev.sh
```
This starts the API server AND the app automatically!

**Option 2: Gradle Task**
```bash
# Start server + install app
./gradlew runWithServer

# Stop server when done
./gradlew stopApiServer
```

**→ [Full Auto-Start Guide](AUTO_START_SERVER.md)** - Multiple ways to start everything together!

---

### Manual Method (Traditional):

**From Android Studio:**
1. Make sure your emulator or device is running
2. In the toolbar, select your device from the dropdown
3. Click the **▶ Run** button (green play icon)
4. Or press **Shift + F10** (Windows/Linux) or **Control + R** (Mac)

**From Terminal:**
```bash
# Clean build
./gradlew clean

# Install on connected device/emulator
./gradlew installDebug

# Or build and run in one command
./gradlew installDebug && adb shell am start -n com.trailguide.android/.presentation.MainActivity
```

**Note**: Manual method requires starting API server separately:
```bash
cd api-proxy
npm start
```

---

## ✅ Step 5: Verify Everything Works

### Test Deep Links:
```bash
# Test that OAuth callbacks work
adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback" com.trailguide.android
```

The app should open. If it doesn't, check your AndroidManifest.xml.

### Test Authentication:

1. **Email Sign-Up:**
   - Open the app
   - Go to **Profile** tab
   - Tap **"Register with Email"**
   - Enter email, password, and name
   - Check your email for confirmation link
   - Click the link → App should open and you're signed in ✅

2. **Google Sign-In** (if configured):
   - Tap **"Sign in with Google"**
   - Browser opens with Google account picker
   - Select account and grant permissions
   - App comes back and you're signed in ✅

3. **Sign-Out:**
   - Tap **"Sign Out"**
   - Should sign out cleanly ✅

---

## 🔧 Troubleshooting

### Issue: "Gradle sync failed"

**Solution:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

In Android Studio: **File** → **Invalidate Caches** → **Invalidate and Restart**

---

### Issue: "Supabase credentials not found"

**Solution:**
- Check `app/build.gradle.kts` has correct `SUPABASE_URL` and `SUPABASE_KEY`
- Make sure strings are wrapped in quotes: `"\"value\""`
- Click **Sync Now** in the banner that appears

---

### Issue: "OAuth redirect URL not allowed"

**Solution:**
- Go to Supabase Dashboard → **Authentication** → **URL Configuration**
- Verify `trailguide://auth-callback` is in **Redirect URLs**
- Verify **Site URL** is set to `trailguide://auth-callback`
- Must match EXACTLY (no typos, no extra spaces)

---

### Issue: "App crashes on startup"

**Solution:**
```bash
# Check logs
adb logcat | grep -i "TrailGuide\|MainActivity\|SupabaseAuth"

# Clear app data and reinstall
adb shell pm clear com.trailguide.android
./gradlew installDebug
```

---

### Issue: "Google sign-in doesn't work"

**Solution:**
1. Verify Google OAuth is configured in Supabase
2. Check Supabase logs: **Authentication** → **Logs**
3. Ensure redirect URLs are correct
4. Test manually:
   ```bash
   adb logcat | grep -i "OAuth\|MainActivity"
   ```
   Then try signing in and watch for errors

---

### Issue: "Deep links don't open the app"

**Solution:**
1. Verify AndroidManifest.xml has the intent-filter (already included)
2. Test manually:
   ```bash
   adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback" com.trailguide.android
   ```
3. If it fails, check logcat for errors
4. Ensure `android:launchMode="singleTask"` is set (already done)

---

## 📊 View Logs in Android Studio

### Logcat Window:
1. Click **Logcat** tab at the bottom of Android Studio
2. Select your device/emulator from the dropdown
3. Set filter to: `package:com.trailguide.android`
4. Watch for:
   - `MainActivity: Deep link received`
   - `AuthRepository: OAuth flow initiated`
   - `MainActivity: OAuth callback processed`

### Filter by Tag:
```
MainActivity|AuthRepository|SupabaseAuth
```

---

## 🎯 Quick Command Reference

### Build Commands:
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Run tests
./gradlew test
```

### ADB Commands:
```bash
# List devices
adb devices

# Install APK manually
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Clear app data
adb shell pm clear com.trailguide.android

# Start app
adb shell am start -n com.trailguide.android/.presentation.MainActivity

# View logs
adb logcat | grep -i "TrailGuide"
```

---

## 📁 Project Structure

```
TrailGuide_Android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/trailguide/android/
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── MainActivity.kt ← OAuth callback handler
│   │   │   │   │   ├── screens/
│   │   │   │   │   └── viewmodel/
│   │   │   │   ├── data/
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── AuthRepository.kt ← Auth logic
│   │   │   │   │   └── remote/
│   │   │   │   └── di/
│   │   │   │       └── AppModule.kt ← Supabase config
│   │   │   ├── AndroidManifest.xml ← Deep link config
│   │   │   └── res/
│   │   └── test/
│   └── build.gradle.kts ← Supabase credentials
├── build.gradle.kts
├── settings.gradle.kts
├── README.md ← This file
└── docs/
    ├── QUICK_FIX_GUIDE.md
    ├── OAUTH_FIXES_SUMMARY.md
    └── SUPABASE_STEP_BY_STEP.md
```

---

## 📖 Additional Documentation

- **`QUICK_FIX_GUIDE.md`** - Setup checklist and quick fixes
- **`OAUTH_FIXES_SUMMARY.md`** - Technical details of OAuth fixes
- **`SUPABASE_STEP_BY_STEP.md`** - Detailed Supabase configuration
- **`SUPABASE_AUTH_SETUP.md`** - Complete authentication setup guide

---

## 🎓 Key Features

- ✅ **Email Authentication** - Register and sign in with email
- ✅ **Google OAuth** - Sign in with Google account
- ✅ **Email Verification** - Confirm email via link
- ✅ **User Profiles** - View and manage user settings
- ✅ **Trail Discovery** - Browse hiking trails
- ✅ **Maps Integration** - View trails on Google Maps
- ✅ **Offline Downloads** - Download trails for offline use

---

## 🆘 Need Help?

### Check Documentation:
1. Read `QUICK_FIX_GUIDE.md` for common issues
2. Review `OAUTH_FIXES_SUMMARY.md` for OAuth details
3. Check `SUPABASE_STEP_BY_STEP.md` for Supabase setup

### Check Logs:
```bash
adb logcat | grep -i "MainActivity\|AuthRepository"
```

### Check Supabase:
- Go to **Authentication** → **Logs** in Supabase dashboard
- Look for redirect URL errors or authentication failures

### Verify Configuration:
```bash
# Test deep link
adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback" com.trailguide.android

# Check if app is installed
adb shell pm list packages | grep trailguide

# Check app info
adb shell dumpsys package com.trailguide.android | grep -i "scheme\|host"
```

---

## ✨ You're All Set!

The app is ready to run. Just:
1. ✅ Open in Android Studio
2. ✅ Configure Supabase credentials
3. ✅ Set up emulator/device
4. ✅ Click Run ▶️

Happy coding! 🚀

---

**Last Updated**: October 2025  
**Author**: TrailGuide Development Team

