# Supabase Authentication Setup Guide

## Overview

TrailGuide now uses **Supabase exclusively** for authentication and database operations. Firebase has been completely removed from the project.

---

## ✅ Changes Made

### 1. **Removed Firebase Dependencies**

**Before (Firebase):**
```kotlin
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.android.gms:play-services-auth:20.7.0")
```

**After (Supabase):**
```kotlin
implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.0")
implementation("io.ktor:ktor-client-android:2.3.7")
```

### 2. **Updated Authentication Repository**

`AuthRepository.kt` now uses:
- ✅ Supabase GoTrue client for authentication
- ✅ Google OAuth via Supabase
- ✅ Email/Password authentication
- ✅ Session management
- ❌ No Firebase dependencies

### 3. **Updated Hilt Module**

`AppModule.kt` provides:
```kotlin
@Provides
@Singleton
fun provideSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Auth)    // For authentication
        install(Postgrest)  // For database queries
    }
}
```

---

## 🔧 Setup Instructions

### Step 1: Configure Supabase Project

1. **Go to your Supabase Dashboard**
   - URL: https://app.supabase.com/project/YOUR_PROJECT_ID

2. **Enable Google OAuth Provider**
   - Navigate to: `Authentication` → `Providers` → `Google`
   - Toggle **Enable Google Provider** to ON
   - Get Google OAuth credentials from Google Cloud Console
   - Add your Client ID and Client Secret
   - Save changes

3. **Configure Redirect URLs** ⚠️ **CRITICAL**
   - Go to: `Authentication` → `URL Configuration` → `Redirect URLs`
   - Add these EXACT URLs (click "Add URL" for each):
   ```
   trailguide://auth-callback
   http://localhost:3000/callback
   https://YOUR_PROJECT_ID.supabase.co/auth/v1/callback
   ```
   - Replace `YOUR_PROJECT_ID` with your actual Supabase project ID
   - Set **Site URL** to: `trailguide://auth-callback`

4. **Enable Email Authentication**
   - Navigate to: `Authentication` → `Providers` → `Email`
   - Toggle **Enable Email Provider** to ON
   - Check "Confirm email" if you want email verification
   - Set **Confirm email redirect URL** to: `trailguide://auth-callback`
   
5. **Configure Email Templates**
   - Navigate to: `Authentication` → `Email Templates`
   - Select "Confirm signup" template
   - Ensure the confirmation link redirects to `trailguide://auth-callback`
   - Default template should work, but verify it uses `{{ .ConfirmationURL }}`

### Step 2: Update Android App Configuration

1. **Verify `build.gradle.kts` has Supabase credentials:**
   ```kotlin
   buildConfigField("String", "SUPABASE_URL", "\"https://YOUR_PROJECT.supabase.co\"")
   buildConfigField("String", "SUPABASE_KEY", "\"your-anon-key\"")
   ```

2. **No `google-services.json` needed!**
   Firebase configuration file has been removed.

3. **Sync Gradle dependencies**
   ```bash
   ./gradlew sync
   ```

### Step 3: Configure Deep Links (for OAuth)

The app is already configured with the correct deep link handler in `AndroidManifest.xml`:
```xml
<activity android:name=".presentation.MainActivity"
    android:launchMode="singleTask">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="trailguide"
            android:host="auth-callback" />
    </intent-filter>
</activity>
```

**Note**: The scheme is `trailguide://auth-callback` - this MUST match what's configured in Supabase!

---

## 🚀 Using Authentication in the App

### Google Sign-In

```kotlin
// In ProfileViewModel
fun signInWithGoogle() {
    viewModelScope.launch {
        authRepository.signInWithGoogle().collect { result ->
            when (result) {
                is NetworkResult.Success -> {
                    // User signed in successfully
                    val user = result.data
                }
                is NetworkResult.Error -> {
                    // Handle error
                }
            }
        }
    }
}
```

### Email/Password Sign-In

```kotlin
// Sign in
authRepository.signInWithEmail(email, password)

// Register
authRepository.registerWithEmail(email, password, displayName)
```

### Check Authentication State

```kotlin
// Check if user is signed in
val isSignedIn = authRepository.isSignedIn()

// Get current user
val currentUser = authRepository.getCurrentUserModel()

// Sign out
authRepository.signOut()
```

---

## 🎯 Features Available

### ✅ Supported Authentication Methods

1. **Google OAuth**
   - Sign in with Google button
   - Handled entirely by Supabase
   - No Firebase required

2. **Email/Password**
   - Register new users
   - Sign in existing users
   - Email verification (optional)

3. **Session Management**
   - Automatic session refresh
   - Persistent login
   - Secure token storage

### ✅ User Information

Access user data from Supabase:
```kotlin
val user = supabaseClient.auth.currentUserOrNull()
user?.let {
    val id = it.id
    val email = it.email
    val displayName = it.userMetadata?.get("full_name")
    val photoUrl = it.userMetadata?.get("avatar_url")
}
```

---

## 📊 Architecture

### Authentication Flow

```
User → ProfileScreen → ProfileViewModel → AuthRepository → Supabase Auth
                                                              ↓
                                                         Google OAuth
                                                         Email/Password
```

### Data Flow

```
App Startup
    ↓
Check Auth State (Supabase)
    ↓
If Authenticated: Load User Data
    ↓
Sync with Supabase Session
```

---

## 🔒 Security Considerations

1. **API Keys**: Stored in `BuildConfig` (not committed to Git)
2. **Session Tokens**: Managed by Supabase client
3. **OAuth Tokens**: Handled securely by Supabase
4. **No Client Secrets**: Only public anon key in app

---

## 🧪 Testing Authentication

### Test Google Sign-In

1. Run the app
2. Navigate to Profile screen
3. Tap "Sign In" button
4. Select Google account
5. Grant permissions
6. User should be authenticated

### Test Email Sign-In

1. Navigate to Profile screen
2. Enter email and password
3. Tap "Sign In"
4. User should be authenticated

### Verify in Supabase Dashboard

1. Go to `Authentication` → `Users`
2. See newly created user
3. Check user metadata

---

## 🐛 Troubleshooting

### Issue: "OAuth redirect URL not allowed" or "Sign-in cancels"

**Solution**: Add the correct redirect URL in Supabase dashboard:
- Go to `Authentication` → `URL Configuration` → `Redirect URLs`
- Add: `trailguide://auth-callback` (NOT `com.trailguide.android://callback`)
- Set Site URL to: `trailguide://auth-callback`
- This MUST match the scheme/host in AndroidManifest.xml and AppModule.kt

### Issue: "Invalid Google credentials"

**Solution**: 
1. Verify Google OAuth setup in Google Cloud Console
2. Ensure Client ID and Secret are correct in Supabase
3. Check that SHA-1 fingerprint is registered (for Android)

### Issue: "Session expired"

**Solution**: Call `authRepository.refreshSession()` to refresh the token

### Issue: Build errors after migration

**Solution**:
1. Clean build: `./gradlew clean`
2. Sync Gradle
3. Rebuild project
4. Clear Android Studio cache if needed

---

## 📚 Key Files Changed

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Removed Firebase, added Supabase |
| `build.gradle.kts` | Removed Firebase plugin |
| `AuthRepository.kt` | Complete rewrite for Supabase |
| `AppModule.kt` | Provides SupabaseClient |
| `ProfileScreen.kt` | Updated UI for Supabase auth |
| `ProfileViewModel.kt` | Updated auth logic |
| `google-services.json` | **DELETED** |

---

## 🎓 Benefits of Supabase Auth

1. ✅ **Simpler Setup**: No Firebase console needed
2. ✅ **Unified Backend**: Auth + Database in one platform
3. ✅ **Open Source**: PostgreSQL-based
4. ✅ **Better Developer Experience**: Clean API, great docs
5. ✅ **Cost-Effective**: Generous free tier
6. ✅ **Full Control**: Self-hostable if needed

---

### Issue: Email verification shows blank page

**Solution**:
1. In Supabase, go to `Authentication` → `Providers` → `Email`
2. Set "Confirm email redirect URL" to: `trailguide://auth-callback`
3. Go to `Authentication` → `Email Templates`
4. Verify "Confirm signup" template uses correct redirect URL
5. Rebuild and reinstall the app

### Issue: Deep links not opening the app

**Solution**:
1. Verify app is installed on device
2. Test deep link manually:
   ```bash
   adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback" com.trailguide.android
   ```
3. Check AndroidManifest.xml has `android:launchMode="singleTask"`
4. Ensure MainActivity has `onNewIntent()` override (already implemented)

---

## 📖 Additional Resources

- [Supabase Auth Documentation](https://supabase.com/docs/guides/auth)
- [Supabase Deep Linking Guide](https://supabase.com/docs/guides/auth/native-mobile-deep-linking)
- [SUPABASE_REDIRECT_FIX.md](./SUPABASE_REDIRECT_FIX.md) - Detailed troubleshooting guide
- [Supabase Kotlin Client](https://github.com/supabase-community/supabase-kt)
- [Google OAuth Setup](https://supabase.com/docs/guides/auth/social-login/auth-google)

---

## ✅ Migration Checklist

- [x] Remove Firebase dependencies from Gradle
- [x] Remove `google-services.json`
- [x] Add Supabase Kotlin dependencies
- [x] Rewrite `AuthRepository` for Supabase
- [x] Update `AppModule` to provide SupabaseClient
- [x] Update `ProfileScreen` UI
- [x] Update `ProfileViewModel` logic
- [x] Configure Supabase OAuth providers
- [x] Test Google sign-in
- [x] Test email sign-in
- [x] Update documentation

---

**Status**: ✅ **Complete** - TrailGuide now uses Supabase exclusively for authentication!

**Date**: October 2025  
**Version**: 2.0 (Supabase-only)

