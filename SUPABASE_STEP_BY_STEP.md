# Step-by-Step Supabase Configuration

## 🎯 Your App Is Ready - Just Configure Supabase!

The Android app is now installed on your emulator with all fixes. You just need to configure your Supabase dashboard (takes 2 minutes).

---

## 📍 Step 1: Open Supabase Dashboard

1. Go to: **https://app.supabase.com**
2. Sign in to your account
3. Click on your **TrailGuide** project (or whatever you named it)

---

## 📍 Step 2: Configure Redirect URLs

### Navigate to URL Configuration:
- On the left sidebar, click: **Authentication** (shield icon)
- Then click: **URL Configuration**

### Add Redirect URLs:
You'll see a section called **"Redirect URLs"** with a list of URLs and an "Add URL" button.

**Click "Add URL"** and type:
```
trailguide://auth-callback
```

Click the checkmark or press Enter to save it.

**Click "Add URL"** again and add:
```
http://localhost:3000/callback
```

---

## 📍 Step 3: Set Site URL

In the same **URL Configuration** page, scroll down to find **"Site URL"**.

Change it to:
```
trailguide://auth-callback
```

**Click "Save"** at the bottom of the page.

---

## 📍 Step 4: Configure Email Provider

### Navigate to Email Provider:
- Still in **Authentication**, find **Providers** in the left sidebar
- Click on **Email**

### Configure Settings:
1. Make sure **"Enable Email provider"** is toggled ON (should be green)
2. Find the setting called **"Confirm email redirect URL"**
3. Set it to:
   ```
   trailguide://auth-callback
   ```
4. **Click "Save"**

---

## 📍 Step 5: Configure Google Provider (If Using)

### Navigate to Google Provider:
- In **Authentication** → **Providers**
- Click on **Google**

### Configure Settings:
1. Toggle **"Enable Google provider"** to ON
2. Add your **Client ID** and **Client Secret** from Google Cloud Console
3. **Click "Save"**

**Need Google credentials?** You need to:
- Go to Google Cloud Console
- Create OAuth 2.0 credentials
- Copy the Client ID and Secret

---

## ✅ Step 6: Verify Configuration

### Quick Check:
Go back to **Authentication** → **URL Configuration** and verify:
- ✅ `trailguide://auth-callback` is in the Redirect URLs list
- ✅ Site URL is set to `trailguide://auth-callback`

Go to **Authentication** → **Providers** → **Email** and verify:
- ✅ Email provider is enabled
- ✅ Confirm email redirect URL is `trailguide://auth-callback`

---

## 🧪 Step 7: Test the App!

### Test Google Sign-In:
1. Open the app on your emulator (it's already running)
2. Navigate to the **Profile** tab at the bottom
3. Tap **"Sign in with Google"**
4. Select your Google account
5. ✅ You should be redirected back to the app and signed in!

### Test Email Registration:
1. Tap **"Register with Email"** 
2. Fill in email, password, and name
3. Check your email inbox
4. Click the confirmation link
5. ✅ The app should open and you should be signed in!

---

## 🐛 If It Still Doesn't Work

### Check Supabase Logs:
- Go to **Authentication** → **Logs** in your Supabase dashboard
- Look for any error messages related to redirect URLs

### Verify URLs Match:
- The URL in Supabase MUST be **exactly**: `trailguide://auth-callback`
- No typos, no extra spaces
- No `https://`, no `www.`

### Try This Test:
On your computer, run:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback" com.trailguide.android
```

This should open the app. If it does, your deep linking is working correctly, and the issue is with Supabase configuration.

---

## 📸 What You Should See in Supabase

### URL Configuration Page:
```
Redirect URLs:
• trailguide://auth-callback
• http://localhost:3000/callback

Site URL:
trailguide://auth-callback
```

### Email Provider Page:
```
Enable Email provider: ✅ ON
Confirm email: ✅ ON
Confirm email redirect URL: trailguide://auth-callback
```

---

## 🎉 Success Indicators

After configuring Supabase, when you test authentication:

✅ **Google Sign-In**: Browser opens → select account → redirects to app → you're signed in

✅ **Email Verification**: Register → check email → click link → app opens → you're signed in

❌ **If it fails**: Check Supabase logs and verify URLs exactly match `trailguide://auth-callback`

---

## 💡 Pro Tips

1. **Use incognito browser** for testing to avoid cached Google accounts
2. **Check spam folder** for verification emails
3. **Wait 30 seconds** after changing Supabase settings for them to take effect
4. **Clear app data** if you had previous failed login attempts:
   ```bash
   adb shell pm clear com.trailguide.android
   ```

---

## 🆘 Still Stuck?

Run these commands and share the output:

```bash
# Check if deep links work
adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback" com.trailguide.android

# Monitor logs during sign-in
adb logcat -c && adb logcat | grep -i "MainActivity\|SupabaseAuth\|OAuth"
```

Then try signing in and watch for error messages in the logs.

---

**You're almost done! Just configure Supabase and you're good to go!** 🚀

