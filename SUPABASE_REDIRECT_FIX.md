# Supabase Authentication Redirect URL Configuration

## Issues and Fixes

This document addresses two critical authentication issues:

1. **Google OAuth cancels and signs user out**
2. **Email verification shows blank URL**

---

## ✅ Required Configuration

### 1. Configure Supabase Redirect URLs

You **MUST** add the following redirect URLs in your Supabase dashboard:

#### Step 1: Go to Supabase Dashboard
Navigate to: **Authentication** → **URL Configuration** → **Redirect URLs**

#### Step 2: Add These EXACT URLs

Add the following redirect URLs (click "Add URL" for each):

```
trailguide://auth-callback
http://localhost:3000/callback
https://YOUR_PROJECT_ID.supabase.co/auth/v1/callback
```

**Important**: Replace `YOUR_PROJECT_ID` with your actual Supabase project ID.

### 2. Configure Site URL

In the same URL Configuration section, set your **Site URL** to:

```
trailguide://auth-callback
```

### 3. Configure Email Templates

For email verification to work properly:

#### Go to: **Authentication** → **Email Templates**

#### Update "Confirm signup" template:

Replace the default confirmation link with:

```html
<a href="{{ .ConfirmationURL }}">Confirm your email</a>
```

Make sure the `ConfirmationURL` variable is used correctly.

#### Configure Email Settings:

In **Authentication** → **Providers** → **Email**:

- ✅ Enable Email provider
- ✅ Check "Confirm email" (if you want email verification)
- Set "Confirm email redirect URL" to: `trailguide://auth-callback`

---

## 🔧 Android App Configuration (Already Done)

The following configurations are already in place in the Android app:

### AndroidManifest.xml
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="trailguide"
        android:host="auth-callback" />
</intent-filter>
```

### AppModule.kt
```kotlin
install(Auth) {
    scheme = "trailguide"
    host = "auth-callback"
}
```

### MainActivity.kt
Now includes `onNewIntent()` to properly handle OAuth callbacks when the app is already running.

---

## 🧪 Testing

### Test Google OAuth:

1. Open the app
2. Go to Profile screen
3. Tap "Sign in with Google"
4. Select your Google account
5. Grant permissions
6. **The app should redirect back and you should be signed in**

If it still cancels:
- Check Supabase logs: **Authentication** → **Logs**
- Look for redirect URL errors
- Verify `trailguide://auth-callback` is in your allowed redirect URLs list

### Test Email Registration:

1. Open the app
2. Go to Profile screen
3. Tap "Register with Email"
4. Enter email, password, and name
5. Check your email inbox
6. **Click the confirmation link**
7. The app should open and you should be signed in

If you see a blank page:
- Check that email templates are configured correctly
- Verify the confirmation redirect URL is `trailguide://auth-callback`
- Check Supabase email settings

---

## 🐛 Troubleshooting

### Issue: Google OAuth still canceling

**Solution**:
1. In Supabase dashboard, go to **Authentication** → **Providers** → **Google**
2. Verify your Google OAuth credentials are correct
3. Make sure `trailguide://auth-callback` is in redirect URLs
4. Check Android logs for errors:
   ```bash
   adb logcat | grep -i "MainActivity\|SupabaseAuth"
   ```

### Issue: Email verification still shows blank page

**Solution**:
1. In Supabase, go to **Authentication** → **Email Templates**
2. Check the "Confirm signup" template
3. The confirmation URL should redirect to `trailguide://auth-callback`
4. Test by registering a new user and checking the email link

### Issue: Deep links not working

**Solution**:
1. Verify `android:launchMode="singleTask"` is set in AndroidManifest (already done)
2. Test deep link manually:
   ```bash
   adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback?access_token=test" com.trailguide.android
   ```
3. Check that MainActivity receives the intent in logs

---

## 📋 Checklist

Before testing, ensure:

- [ ] Added `trailguide://auth-callback` to Supabase redirect URLs
- [ ] Set Site URL in Supabase to `trailguide://auth-callback`
- [ ] Configured email templates to use `trailguide://auth-callback`
- [ ] Google OAuth provider is enabled with correct credentials
- [ ] Email provider is enabled with email confirmation settings
- [ ] Built and installed the updated app with MainActivity changes

---

## 🎯 Expected Behavior After Fix

### Google Sign-In:
1. User taps "Sign in with Google"
2. Browser/Google account picker opens
3. User selects account and grants permissions
4. Browser redirects to `trailguide://auth-callback?access_token=...`
5. Android opens the app via deep link
6. MainActivity handles the deep link
7. Supabase SDK processes the OAuth callback
8. User is signed in ✅

### Email Verification:
1. User registers with email
2. Supabase sends confirmation email
3. User clicks link in email
4. Browser opens with confirmation URL
5. Supabase processes confirmation
6. Browser redirects to `trailguide://auth-callback?...`
7. Android opens the app via deep link
8. User is signed in ✅

---

## 📖 Additional Resources

- [Supabase Auth Deep Linking Guide](https://supabase.com/docs/guides/auth/native-mobile-deep-linking)
- [Android Deep Links Documentation](https://developer.android.com/training/app-links/deep-linking)
- [Supabase Auth Configuration](https://supabase.com/docs/guides/auth/redirect-urls)

---

## 🔍 Verification

To verify your Supabase configuration is correct, you can use the Supabase API:

```bash
curl -X GET 'https://YOUR_PROJECT_ID.supabase.co/auth/v1/settings' \
  -H "apikey: YOUR_ANON_KEY"
```

Look for `external` → `redirect_url` and ensure it includes `trailguide://auth-callback`.

---

**Last Updated**: October 2025

