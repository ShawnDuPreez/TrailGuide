# Render Deployment - Quick Steps

## ✅ Step-by-Step Guide

### Step 1: Environment Variables (✅ DONE!)
You've already set these up in Render:
- ✅ SUPABASE_URL
- ✅ SUPABASE_ANON_KEY
- ✅ PORT
- ✅ NODE_ENV
- ✅ API_VERSION

### Step 2: Deploy
Click **"Create Web Service"** button in Render

Wait 2-3 minutes for deployment to complete.

### Step 3: Get Your Render URL
After deployment, Render will show your URL:
```
https://trailguide-api-xyz.onrender.com
```

**Copy this entire URL!**

### Step 4: Test Your Deployment
Open this URL in your browser (add `/api/trails`):
```
https://trailguide-api-xyz.onrender.com/api/trails
```

You should see JSON with trails data. ✅

### Step 5: Update Android App

**Open:** `app/build.gradle.kts`

**Find line 46** (around there):
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://YOUR_RENDER_URL_HERE/\"")
```

**Replace with your actual Render URL:**
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://trailguide-api-xyz.onrender.com/\"")
```

⚠️ **Don't forget the trailing slash `/` at the end!**

### Step 6: Sync Gradle
In Android Studio:
- Click **"Sync Now"** when the banner appears
- Or: **File** → **Sync Project with Gradle Files**

### Step 7: Build Release APK

**Option A: Using the helper script**
```bash
.\build-for-testing.bat
```

**Option B: Using Gradle directly**
```bash
./gradlew clean
./gradlew assembleRelease
```

### Step 8: Find Your APK
Location:
```
app\build\outputs\apk\release\app-release.apk
```

### Step 9: Test the APK
1. Install on your device/emulator
2. Open the app
3. Go to Trails screen
4. Trails should load from Render! ✅

---

## 🎯 Quick Summary

**What you need to change:**
1. Line 46 in `app/build.gradle.kts`
2. Replace `https://YOUR_RENDER_URL_HERE/` with your actual Render URL
3. Don't forget the `/` at the end
4. Build release APK
5. Done! ✅

---

## 📝 Example

**Before:**
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://YOUR_RENDER_URL_HERE/\"")
```

**After:**
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://trailguide-api-abc123.onrender.com/\"")
```

---

## 🧪 Testing

### Test API Server (before building APK):
```bash
curl https://your-render-url.onrender.com/api/trails
```

### Test Release APK:
```bash
# Install
adb install -r app/build/outputs/apk/release/app-release.apk

# Launch
adb shell am start -n com.trailguide.android/.presentation.MainActivity

# Check logs
adb logcat | grep -i "Retrofit\|TrailApiService"
```

---

## 🎓 For Your Demo

**5 minutes before demo, wake up the server:**
```bash
curl https://your-render-url.onrender.com/api/trails
```

Or just open the URL in a browser!

This ensures instant response during your presentation. ✅

---

## ⚠️ Important Notes

1. **Always use HTTPS** (not HTTP) - Render provides SSL automatically
2. **Include the trailing slash** in the URL
3. **Test in browser first** before building APK
4. **Keep debug build pointing to localhost** (for development)

---

## 🆘 Troubleshooting

### APK can't connect to server
- Check the URL in build.gradle.kts
- Make sure you included the `/` at the end
- Verify server is running (open URL in browser)
- Rebuild APK after changing URL

### Server returns 404
- Check your server is actually deployed
- Verify environment variables are set
- Check Render logs for errors

### "Network error" in app
- Server might be sleeping (wake it up)
- Check your internet connection
- Verify URL is correct (HTTPS not HTTP)

---

**Next:** After deployment completes, copy your Render URL and update `app/build.gradle.kts` line 46!

