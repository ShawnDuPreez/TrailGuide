# Quick Fix Guide - Auth Issues

## 🔥 What I Fixed

### Issue 1: Google Sign-In Canceling
**Problem**: OAuth redirects but app doesn't handle the callback, so user gets logged out.

**Fix**: Updated `MainActivity.kt` to handle deep link callbacks with `onNewIntent()`.

### Issue 2: Email Verification Blank URL
**Problem**: Email confirmation links don't redirect back to the app.

**Fix**: Need to configure redirect URLs in Supabase dashboard (see below).

---

## ⚡ Quick Setup (Do This Now!)

### 1. Open Your Supabase Dashboard

Go to: `https://app.supabase.com/project/YOUR_PROJECT_ID`

### 2. Configure Redirect URLs

Navigate to: **Authentication** → **URL Configuration** → **Redirect URLs**

Click **"Add URL"** and add these EXACT URLs:

```
trailguide://auth-callback
http://localhost:3000/callback
```

### 3. Set Site URL

In the same **URL Configuration** section, set **Site URL** to:

```
trailguide://auth-callback
```

### 4. Configure Email Settings

Navigate to: **Authentication** → **Providers** → **Email**

- ✅ Enable Email Provider
- ✅ Check "Confirm email" (if you want verification)
- Set **"Confirm email redirect URL"** to: `trailguide://auth-callback`

Click **Save**

### 5. Check Email Template (Optional)

Navigate to: **Authentication** → **Email Templates** → **Confirm signup**

Verify the template contains:
```html
<a href="{{ .ConfirmationURL }}">Confirm your email</a>
```

---

## 🧪 Test It

### Test Google Sign-In:

1. Rebuild and install the app:
   ```bash
   ./gradlew clean
   ./gradlew installDebug
   ```

2. Open the app, go to Profile screen
3. Tap "Sign in with Google"
4. Select your Google account
5. **You should now be signed in** ✅

### Test Email Verification:

1. Register a new user with email
2. Check your email inbox
3. Click the confirmation link
4. **App should open and you should be signed in** ✅

---

## 🐛 Still Not Working?

### Check Supabase Logs

Go to: **Authentication** → **Logs** in Supabase dashboard

Look for errors related to:
- "redirect_uri_mismatch"
- "invalid_redirect_uri"
- Any OAuth errors

### Test Deep Link Manually

Connect your device and run:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback" com.trailguide.android
```

This should open your app. If it doesn't, there's an issue with the deep link configuration.

### Check Android Logs

While testing, monitor logs:
```bash
adb logcat | grep -i "MainActivity\|SupabaseAuth\|OAuth"
```

Look for:
- "Deep link received: trailguide://auth-callback"
- "Auth callback detected"

---

## ✅ Checklist

Before reporting issues, confirm:

- [ ] Added `trailguide://auth-callback` to Supabase redirect URLs
- [ ] Set Site URL to `trailguide://auth-callback`
- [ ] Set email redirect URL to `trailguide://auth-callback`
- [ ] Rebuilt and reinstalled the app with new MainActivity
- [ ] Cleared app data/cache before testing
- [ ] Tested on a real device (not emulator, if possible)

---

## 📁 Files Changed

- ✅ `MainActivity.kt` - Now handles OAuth callbacks properly
- ✅ `SUPABASE_AUTH_SETUP.md` - Updated with correct configuration
- ✅ `SUPABASE_REDIRECT_FIX.md` - Detailed troubleshooting guide

---

## 🆘 Need More Help?

1. Check `SUPABASE_REDIRECT_FIX.md` for detailed troubleshooting
2. Check `SUPABASE_AUTH_SETUP.md` for complete setup guide
3. Review Supabase authentication logs
4. Check Android logcat output during auth flow

---

**Last Updated**: October 2025

