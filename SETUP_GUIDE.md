# TrailGuide Setup Guide

This guide ensures all features are properly configured and working.

## ✅ Completed Features

### 1. SSO (Single Sign-On) ✅
- **Status**: Implemented and working
- **Implementation**: Supabase Auth with Google OAuth
- **Location**: `AuthRepository.kt`, `AuthViewModel.kt`
- **How to test**: 
  - Click "Sign in with Google" button
  - Complete OAuth flow in browser
  - App will handle callback via deep link

### 2. User Settings ✅
- **Status**: Implemented and working
- **Implementation**: Settings screen with DataStore for preferences
- **Location**: `SettingsScreen.kt`, `SettingsViewModel.kt`, `UserPreferences.kt`
- **Features**:
  - Language selection (English, Afrikaans, isiZulu)
  - Biometric authentication toggle
  - Notification preferences
  - Theme selection
  - Manual sync trigger

### 3. REST API + Database ✅
- **Status**: Implemented and working
- **Implementation**: Node.js/Express API server connected to Supabase PostgreSQL
- **Location**: `api-proxy/server.js`, `api-proxy/database-setup.sql`
- **Endpoints**:
  - `/api/register` - User registration
  - `/api/login` - User login
  - `/api/trails` - Trail CRUD operations
  - `/api/users/:id/favourites` - Favorites management
  - `/api/sync` - Offline data sync
  - `/api/users/fcm-token` - FCM token registration

### 4. Offline Mode with Sync ✅
- **Status**: Implemented and working
- **Implementation**: RoomDB with WorkManager sync worker
- **Location**: `SyncWorker.kt`, `SyncScheduler.kt`, `SyncApiService.kt`
- **Features**:
  - Offline-first architecture
  - Automatic background sync every 6 hours
  - Manual sync from Settings
  - Syncs favorites, reviews, and trail progress
  - Conflict resolution and retry logic

### 5. Push Notifications ⚠️
- **Status**: Implemented, requires Firebase setup
- **Implementation**: Firebase Cloud Messaging (FCM)
- **Location**: `TrailGuideMessagingService.kt`, `NotificationUtil.kt`
- **Setup Required**:
  1. Create a Firebase project at https://console.firebase.google.com
  2. Add Android app with package name: `com.trailguide.android`
  3. Download `google-services.json` and place it in `app/` directory
  4. Configure Firebase Cloud Messaging in Firebase Console
  5. Set `FIREBASE_SERVICE_ACCOUNT_KEY` environment variable in API server
- **Template**: See `app/google-services.json.template`

### 6. Multi-language Support ✅
- **Status**: Implemented and working
- **Implementation**: Android resource localization with runtime locale switching
- **Location**: 
  - `values/strings.xml` (English)
  - `values-af/strings.xml` (Afrikaans)
  - `values-zu/strings.xml` (isiZulu)
  - `LocaleHelper.kt`, `MainActivity.kt`
- **Features**:
  - English (default)
  - Afrikaans (Afrikaans)
  - isiZulu (isiZulu)
  - Runtime language switching
  - Persistent language preference

### 7. App Icon ✅
- **Status**: Implemented
- **Location**: 
  - `app/src/main/res/mipmap-*/ic_launcher.png`
  - `app/src/main/res/mipmap-*/ic_launcher_round.png`
  - `app/src/main/res/drawable/ic_launcher.xml`
  - `app/src/main/res/drawable/ic_launcher_round.xml`
- **Sizes**: All required densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)

## 🔧 Configuration Required

### Environment Variables (API Server)

Create `.env` file in `api-proxy/` directory:

```env
SUPABASE_URL=your_supabase_url
SUPABASE_ANON_KEY=your_supabase_anon_key
JWT_SECRET=your_jwt_secret
PORT=3000
NOTIFICATION_ENABLED=true
FIREBASE_SERVICE_ACCOUNT_KEY=your_firebase_service_account_json
OPENWEATHER_API_KEY=your_openweather_api_key
```

### Android local.properties

Create `local.properties` in project root:

```properties
SUPABASE_URL=your_supabase_url
SUPABASE_KEY=your_supabase_key
OPENWEATHER_API_KEY=your_openweather_api_key
GOOGLE_MAPS_API_KEY=your_google_maps_api_key
OPENROUTE_API_KEY=your_openroute_api_key
GOOGLE_TRANSLATE_API_KEY=your_google_translate_api_key
```

### Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project or select existing
3. Add Android app:
   - Package name: `com.trailguide.android`
   - Download `google-services.json`
   - Place in `app/` directory
4. Enable Cloud Messaging
5. Get service account key:
   - Project Settings → Service Accounts
   - Generate new private key
   - Add to API server `.env` as `FIREBASE_SERVICE_ACCOUNT_KEY`

### Database Setup

Run `api-proxy/database-setup.sql` in your Supabase SQL Editor to create required tables.

## 🚀 Running the App

1. **Start API Server**:
   ```bash
   cd api-proxy
   npm install
   npm start
   ```

2. **Build Android App**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on Device**:
   ```bash
   ./gradlew installDebug
   ```

## 📝 Testing Checklist

- [ ] SSO: Register and login with Google
- [ ] SSO: Register and login with email/password
- [ ] Settings: Change language to Afrikaans
- [ ] Settings: Change language to isiZulu
- [ ] Settings: Toggle biometric authentication
- [ ] Settings: Toggle notification preferences
- [ ] Settings: Trigger manual sync
- [ ] Offline: Add favorite while offline
- [ ] Offline: Submit review while offline
- [ ] Offline: Complete trail while offline
- [ ] Sync: Verify data syncs when online
- [ ] Notifications: Receive test notification (requires Firebase setup)
- [ ] API: Verify all endpoints respond correctly

## 🐛 Troubleshooting

### Sync not working
- Check API server is running
- Verify auth token is set in `ApiClient.authToken`
- Check network connectivity
- Review logs in `SyncWorker`

### Language not changing
- Verify `values-af/` and `values-zu/` folders exist
- Check DataStore permissions
- Restart app after language change

### Push notifications not working
- Verify `google-services.json` is in `app/` directory
- Check Firebase project configuration
- Verify FCM token is registered in backend
- Check notification permissions on device

### SSO not working
- Verify Supabase project has Google OAuth enabled
- Check deep link configuration in `AndroidManifest.xml`
- Verify redirect URI matches Supabase configuration

