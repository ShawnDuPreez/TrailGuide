# 🚀 TrailGuide Android - START HERE

## ✅ Everything is Ready!

All authentication issues have been fixed and the app is ready to run in Android Studio!

---

## 📖 Quick Navigation

### 🎯 First Time Setup
**→ [README_RUN_APP.md](README_RUN_APP.md)** - Complete step-by-step guide to run the app

### 🐛 Having Issues?
**→ [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md)** - Fast troubleshooting guide

### 🔐 OAuth Not Working?
**→ [OAUTH_FIXES_SUMMARY.md](OAUTH_FIXES_SUMMARY.md)** - Detailed OAuth fixes explanation

### 🗄️ Supabase Setup
**→ [SUPABASE_STEP_BY_STEP.md](SUPABASE_STEP_BY_STEP.md)** - Supabase configuration guide

---

## ⚡ 3-Step Quick Start

### Step 1: Open in Android Studio
```
File → Open → Select TrailGuide_Android folder
Wait for Gradle sync to complete
```

### Step 2: Configure Supabase
Edit `app/build.gradle.kts` and add your credentials:
```kotlin
buildConfigField("String", "SUPABASE_URL", "\"https://xxxxx.supabase.co\"")
buildConfigField("String", "SUPABASE_KEY", "\"your-anon-key\"")
```

### Step 3: Set Redirect URLs in Supabase Dashboard
Go to: `Authentication → URL Configuration`

Add this redirect URL:
```
trailguide://auth-callback
```

Set Site URL to:
```
trailguide://auth-callback
```

**That's it!** Click Run ▶️

---

## 🚀 NEW: Auto-Start Server + App

Want to start both the API server and app with one command?

### Option 1: One-Click Script (Easiest)
```bash
# Windows: Just double-click
start-dev.bat

# Mac/Linux
./start-dev.sh
```

### Option 2: Gradle Task
```bash
./gradlew runWithServer
```

### Option 3: Android Studio
Configure once, then just click Run ▶️!

**→ [Full Guide: AUTO_START_SERVER.md](AUTO_START_SERVER.md)**

---

## ✨ What Was Fixed

### ✅ Google OAuth Sign-In
**Before**: Signs in, gets saved, then immediately signs out  
**After**: Works perfectly! Browser opens, user signs in, app comes back, user is authenticated ✅

**What I Fixed**:
- MainActivity now properly handles OAuth callback deep links
- AuthRepository improved to not check for user too early
- ProfileViewModel adds multiple refresh attempts to catch callback

### ✅ Email Verification
**Before**: Shows blank URL when clicking verification link  
**After**: Verification link opens app and signs user in ✅

**What I Fixed**:
- Proper redirect URL configuration guide
- Deep link handling in MainActivity

### ✅ Sign-Out Errors
**Before**: "User from sub claim in JWT does not exist" error  
**After**: Sign-out works cleanly even with invalid sessions ✅

**What I Fixed**:
- Added JWT error handling
- Graceful degradation for expired sessions

---

## 🔧 Modified Files

All fixes work in Android Studio:

1. ✅ `MainActivity.kt` - OAuth callback handling
2. ✅ `AuthRepository.kt` - Google sign-in and sign-out fixes
3. ✅ `ProfileViewModel.kt` - Refresh attempts after OAuth
4. ✅ `AndroidManifest.xml` - Already configured (no changes needed)
5. ✅ `AppModule.kt` - Already configured (no changes needed)

---

## 📱 Testing in Android Studio

### Run the App:
1. Click **Device Manager** icon (right sidebar)
2. Start your emulator or connect device
3. Click **Run ▶️** button (or Shift+F10)
4. App launches!

### Test Google Sign-In:
1. Open app on emulator
2. Go to **Profile** tab
3. Tap **"Sign in with Google"**
4. Browser opens with Google account picker
5. Select account
6. Grant permissions
7. **App comes back - you're signed in!** ✅

### Test Email Registration:
1. Tap **"Register with Email"**
2. Enter email, password, name
3. Check your email inbox
4. Click verification link
5. **App opens - you're signed in!** ✅

### View Logs in Android Studio:
1. Click **Logcat** tab at bottom
2. Set filter: `MainActivity|AuthRepository`
3. Watch for success messages during sign-in

---

## 🎯 What You Need to Do

### ⚠️ CRITICAL: Configure Supabase Dashboard

**You MUST do this for OAuth to work:**

1. Go to: `https://app.supabase.com`
2. Select your TrailGuide project
3. Click: `Authentication → URL Configuration`
4. Add redirect URL: `trailguide://auth-callback`
5. Set Site URL: `trailguide://auth-callback`
6. Click: `Authentication → Providers → Email`
7. Set "Confirm email redirect URL": `trailguide://auth-callback`
8. Click **Save**

**See [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md) for screenshots descriptions.**

---

## 📊 Verify Everything Works

### Test Deep Links (from Android Studio Terminal):
```bash
# Terminal in Android Studio
adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback" com.trailguide.android
```

App should open. If it does, deep links work! ✅

### Monitor Logs:
```bash
adb logcat | grep -i "MainActivity\|AuthRepository"
```

During sign-in, you should see:
```
MainActivity: Deep link received: trailguide://auth-callback
MainActivity: Auth callback detected
MainActivity: OAuth callback processed successfully
AuthRepository: Welcome back, [Your Name]!
```

---

## 🆘 Troubleshooting

### Issue: "Gradle sync failed"
**Solution**: 
- **File → Invalidate Caches → Invalidate and Restart**
- Or run: `./gradlew clean`

### Issue: "OAuth still not working"
**Solution**:
- Double-check Supabase redirect URLs
- Verify they match EXACTLY: `trailguide://auth-callback`
- No typos, no extra spaces
- Check [OAUTH_FIXES_SUMMARY.md](OAUTH_FIXES_SUMMARY.md#troubleshooting)

### Issue: "App crashes"
**Solution**:
```bash
# Clear app data
adb shell pm clear com.trailguide.android

# Reinstall
./gradlew installDebug
```

### Issue: "Can't find my Supabase credentials"
**Solution**:
1. Go to Supabase Dashboard
2. Click **Settings** (⚙️) → **API**
3. Copy:
   - Project URL (under "Project URL")
   - anon/public key (under "Project API keys")

---

## 📚 Full Documentation

| Document | When to Use |
|----------|-------------|
| **[README_RUN_APP.md](README_RUN_APP.md)** | 🎯 First time setup - start here! |
| [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md) | ⚡ Having issues? Quick fixes |
| [OAUTH_FIXES_SUMMARY.md](OAUTH_FIXES_SUMMARY.md) | 🔐 Understanding OAuth fixes |
| [SUPABASE_STEP_BY_STEP.md](SUPABASE_STEP_BY_STEP.md) | 🗄️ Detailed Supabase setup |
| [SUPABASE_AUTH_SETUP.md](SUPABASE_AUTH_SETUP.md) | 🔑 Complete auth guide |
| [SUPABASE_DASHBOARD_CONFIG.txt](SUPABASE_DASHBOARD_CONFIG.txt) | 📋 Copy-paste config values |
| [README.md](README.md) | 📖 Full project documentation |

---

## ✅ Checklist Before Running

- [ ] Android Studio installed and opened
- [ ] Project opened in Android Studio
- [ ] Gradle sync completed successfully
- [ ] Supabase credentials added to `build.gradle.kts`
- [ ] Redirect URL `trailguide://auth-callback` added in Supabase
- [ ] Site URL set to `trailguide://auth-callback` in Supabase
- [ ] Email redirect URL configured in Supabase
- [ ] Emulator started or device connected
- [ ] Ready to click Run ▶️!

---

## 🎉 You're All Set!

Everything is configured and ready to go. Just:

1. ✅ Open Android Studio
2. ✅ Add Supabase credentials
3. ✅ Configure redirect URLs in Supabase
4. ✅ Click Run ▶️

**The app will work perfectly!** 🚀

---

## 💡 Pro Tips

- **Use Logcat**: Always keep Logcat open to see what's happening
- **Test deep links**: Verify OAuth callbacks work before testing sign-in
- **Clear data**: If something weird happens, clear app data and reinstall
- **Check Supabase logs**: Authentication → Logs shows all auth attempts

---

## 📞 Need Help?

1. Check [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md) for common issues
2. Review [OAUTH_FIXES_SUMMARY.md](OAUTH_FIXES_SUMMARY.md) for OAuth details
3. Verify [SUPABASE_STEP_BY_STEP.md](SUPABASE_STEP_BY_STEP.md) configuration
4. Check Android Studio Logcat for error messages
5. Check Supabase Dashboard → Authentication → Logs

---

**Happy Coding!** 🚀

Made with ❤️ and lots of debugging

---

**Last Updated**: October 2025

