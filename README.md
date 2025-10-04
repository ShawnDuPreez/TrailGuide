# TrailGuide 🥾

**Your ultimate hiking companion for discovering and navigating trails**

<div align="center">

[![Android](https://img.shields.io/badge/Android-8.0+-green.svg)](https://android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## 🌟 What is TrailGuide?

TrailGuide helps you discover, explore, and navigate hiking trails. Find trails near you, view detailed information, see routes on interactive maps, and download trails for offline use.

Perfect for hikers, nature lovers, and outdoor adventurers!

---

## ✨ Features

- 🔍 **Discover Trails** - Browse and search trails by difficulty, distance, and location
- 🗺️ **Interactive Maps** - View trail routes with Google Maps
- 📥 **Offline Mode** - Download trails to use without internet
- 📊 **Trail Details** - Distance, elevation, ratings, and photos
- 🌤️ **Weather Info** - Check conditions before you hike
- 👤 **User Accounts** - Sign in with Google or email to save favorites
- 🌍 **Multi-language** - English, Afrikaans, and isiZulu support

---

## 🚀 Getting Started

### For Users (Installing the App)

1. **Download the APK**
   - Get the latest APK from `app/build/outputs/apk/debug/app-debug.apk`
   - Or download from releases

2. **Install on Android Device**
   - Enable "Install from Unknown Sources" in Settings
   - Open the APK file and tap Install
   - Launch TrailGuide!

3. **First Launch**
   - Allow location permissions for GPS features
   - Sign in with Google or create an account with email
   - Start discovering trails!

---

### For Developers (Building the App)

#### Requirements

- **Android Studio** (latest version)
- **Android SDK** (API 26+)
- **Git** for cloning the repository

#### Setup Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/TrailGuide_Android.git
   cd TrailGuide_Android
   ```

2. **Configure API Keys**
   
   Create/edit `local.properties` in the project root:
   ```properties
   sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   
   # API Keys (get these from respective services)
   SUPABASE_URL=https://yourproject.supabase.co
   SUPABASE_KEY=your-supabase-anon-key
   GOOGLE_MAPS_API_KEY=your-google-maps-api-key
   OPENWEATHER_API_KEY=your-openweather-api-key
   ```

   **Where to get API keys:**
   - Supabase: [app.supabase.com](https://app.supabase.com) → Settings → API
   - Google Maps: [console.cloud.google.com](https://console.cloud.google.com) → Credentials
   - OpenWeather: [openweathermap.org/api](https://openweathermap.org/api)

3. **Set Up Supabase**
   
   In your Supabase dashboard:
   - Go to **Authentication** → **URL Configuration**
   - Add redirect URL: `trailguide://auth-callback`
   - Set site URL: `trailguide://auth-callback`
   - Enable Google provider (optional, for Google Sign-In)

4. **Build the App**
   ```bash
   # Debug build (for testing)
   ./gradlew assembleDebug
   
   # Release build (for production)
   ./gradlew assembleRelease
   ```

5. **Install on Device/Emulator**
   ```bash
   # Install debug APK
   ./gradlew installDebug
   
   # Or manually install
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

#### Quick Tips

- 📱 Find your APK: `app/build/outputs/apk/debug/` or `app/build/outputs/apk/release/`
- 🔒 **Never commit `local.properties`** - it contains your secret API keys
- 🚀 The app auto-wakes the API server on launch (60-90s first load)
- 🗺️ Maps won't work without a valid Google Maps API key

---

## 🛠️ Technology Stack

Built with modern Android technologies:
- **Kotlin** - Modern programming language for Android
- **Jetpack Compose** - Declarative UI framework
- **Google Maps SDK** - Interactive maps
- **Supabase** - Backend and authentication
- **Retrofit** - API communication
- **Hilt** - Dependency injection

---

## 📖 Troubleshooting

### Common Issues

**Maps not showing?**
- Make sure your Google Maps API key is valid
- Check that the key is properly set in `local.properties`

**App taking long to load data?**
- First load takes 60-90 seconds (server wakes up)
- Subsequent loads are faster

**Google Sign-In not working?**
- Verify Supabase redirect URL is set to `trailguide://auth-callback`
- Check Supabase dashboard has Google OAuth enabled

**Build errors?**
- Clean project: `./gradlew clean`
- Rebuild: `./gradlew build`
- Check that all API keys are set in `local.properties`

---

## 🔐 Security Note

⚠️ **Important**: This app uses `local.properties` to store API keys securely.
- Never commit `local.properties` to Git
- Use `local.properties.template` as a reference
- Regenerate any exposed API keys immediately

---

## 📄 License

This project is licensed under the MIT License.

---

## 🤝 Contributing

Contributions, bug reports, and feature requests are welcome!

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Open a Pull Request

---

<div align="center">

**Made with ❤️ for hikers and outdoor enthusiasts**

[⬆ Back to Top](#trailguide-)

</div>

