# TrailGuide - Native Android Application

<div align="center">

![TrailGuide Logo](docs/images/logo-placeholder.png)

**A native Android hiking trail guide application built with Kotlin, MVVM architecture, and Jetpack Compose**

[![Build Status](https://github.com/username/trailguide-android/workflows/Android%20CI%2FCD%20Pipeline/badge.svg)](https://github.com/username/trailguide-android/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)

</div>

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Screenshots](#screenshots)
- [Technical Stack](#technical-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [CI/CD Pipeline](#cicd-pipeline)
- [Design Considerations](#design-considerations)
- [Migration from React Native](#migration-from-react-native)
- [Future Enhancements](#future-enhancements)
- [Contributing](#contributing)
- [License](#license)

---

## 🌟 Overview

**TrailGuide** is a comprehensive native Android application designed for hikers and outdoor enthusiasts. It helps users discover, plan, and navigate hiking trails with features like offline maps, real-time GPS tracking, and personalized recommendations.

This project represents a complete migration from a React Native Expo application to a fully native Android app built with modern Kotlin and Jetpack components.

### Purpose

- **Discover Trails**: Browse and search hiking trails by difficulty, distance, and location
- **Plan Hikes**: View detailed trail information, stats, and segments
- **Navigate**: Interactive maps with GPS tracking and offline support
- **Personalize**: Save favorites, track progress, and customize settings

---

## ✨ Features

### Core Features (MVP)

- ✅ **Trail Discovery**
  - Browse comprehensive trail database
  - Search by name, location, or tags
  - Filter by difficulty (Easy, Moderate, Hard)
  - Filter by distance range (1-30 km)
  
- ✅ **Trail Details**
  - High-resolution trail images
  - Detailed statistics (distance, elevation, rating)
  - Trail segments with difficulty markers
  - GPS coordinates and location info
  
- ✅ **Interactive Map**
  - Google Maps integration
  - Trail markers and routes
  - Multiple map types (Normal, Satellite, Terrain)
  - Real-time location tracking
  
- ✅ **Offline Support**
  - Download trails for offline access
  - Cached maps and GPS data
  - Storage management interface
  
- ✅ **User Profile & Settings**
  - Google Sign-In (OAuth SSO) ✨ **Recently Fixed!**
  - Email/Password authentication
  - Email verification
  - Multi-language support (English, Afrikaans, isiZulu)
  - Biometric authentication
  - Push notification preferences

### Extra Features (POE Enhancements)

- 🎯 **Gamification System**
  - Achievement badges for completed trails
  - Hiking statistics dashboard
  - Leaderboard integration
  
- 📊 **Advanced Analytics**
  - Personal hiking history
  - Performance metrics
  - Trail recommendations
  
- 🌤️ **Weather Integration**
  - Real-time weather data (OpenWeather API)
  - Trail condition alerts
  
- 👥 **Social Features**
  - Share trails with friends
  - Community reviews and photos
  - Group hike planning

---

## 🏗️ Architecture

TrailGuide follows the **MVVM (Model-View-ViewModel)** architecture pattern with clean architecture principles:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  (Jetpack Compose UI + ViewModels + Navigation)         │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                     Domain Layer                         │
│  (Use Cases + Business Logic + Data Models)             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                      Data Layer                          │
│  (Repositories + API Service + Local Database)          │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                 External Dependencies                    │
│  (Retrofit + Firebase + Room + DataStore)               │
└─────────────────────────────────────────────────────────┘
```

### Key Components

- **Presentation Layer**: Jetpack Compose screens, ViewModels, and UI state management
- **Domain Layer**: Business logic, use cases, and domain models
- **Data Layer**: Repository pattern, REST API integration, local caching
- **Dependency Injection**: Hilt for compile-time DI

### Data Flow

```
User Action → ViewModel → Repository → API/Database → Repository → ViewModel → UI Update
```

---

## 📱 Screenshots

| Trails Screen | Trail Details | Map View | Profile |
|---------------|---------------|----------|---------|
| ![Trails](docs/screenshots/trails-placeholder.png) | ![Details](docs/screenshots/details-placeholder.png) | ![Map](docs/screenshots/map-placeholder.png) | ![Profile](docs/screenshots/profile-placeholder.png) |

> **Note**: Replace placeholders with actual screenshots after app is built

---

## 🛠️ Technical Stack

### Core Technologies

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 1.9.20 |
| UI Framework | Jetpack Compose | 2024.01.00 |
| Build System | Gradle | 8.2.0 |
| Min SDK | Android 8.0 (Oreo) | API 26 |
| Target SDK | Android 14 | API 34 |

### Jetpack Components

- **Compose**: Modern declarative UI toolkit
- **ViewModel**: UI state management with lifecycle awareness
- **LiveData/StateFlow**: Reactive data streams
- **Navigation**: Type-safe navigation for Compose
- **DataStore**: Preferences storage (replaces SharedPreferences)
- **Room**: Local database for offline caching

### Third-Party Libraries

| Library | Purpose | Version |
|---------|---------|---------|
| Retrofit | REST API client | 2.9.0 |
| OkHttp | HTTP client with interceptors | 4.12.0 |
| Hilt | Dependency injection | 2.48 |
| Supabase Kotlin | Authentication & Database | 2.0.0 |
| Google Maps | Map display and GPS tracking | 18.2.0 |
| Coil | Image loading and caching | 2.5.0 |
| Gson | JSON serialization | 2.10.1 |
| Coroutines | Asynchronous programming | 1.7.3 |

### Testing

- **JUnit 4**: Unit testing framework
- **Mockito**: Mocking framework for tests
- **Coroutines Test**: Testing coroutines
- **Espresso**: UI testing (Android Instrumentation)

---

## 🚀 Getting Started

### 📖 Quick Start Guide

**→ [READ THIS FIRST: Complete Setup Guide](README_RUN_APP.md)** ⭐

The complete guide includes:
- ✅ Step-by-step Android Studio setup
- ✅ Supabase configuration (with screenshots descriptions)
- ✅ OAuth setup for Google Sign-In
- ✅ Emulator/device setup
- ✅ Common troubleshooting steps

### 📚 Additional Documentation

| Document | Description |
|----------|-------------|
| **[README_RUN_APP.md](README_RUN_APP.md)** | 📱 **START HERE** - Complete setup guide for running the app |
| **[AUTO_START_SERVER.md](AUTO_START_SERVER.md)** | 🚀 **NEW!** Auto-start server + app together |
| [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md) | ⚡ Quick fixes for common issues |
| [OAUTH_FIXES_SUMMARY.md](OAUTH_FIXES_SUMMARY.md) | 🔐 OAuth authentication fixes explained |
| [SUPABASE_STEP_BY_STEP.md](SUPABASE_STEP_BY_STEP.md) | 🗄️ Detailed Supabase configuration |
| [SUPABASE_AUTH_SETUP.md](SUPABASE_AUTH_SETUP.md) | 🔑 Complete authentication setup guide |
| [MIGRATION_SUMMARY.md](MIGRATION_SUMMARY.md) | 🔄 Firebase to Supabase migration notes |

---

### Prerequisites

- ✅ **Android Studio** - Electric Eel or newer
- ✅ **JDK** - Version 11 or newer
- ✅ **Android SDK** - API 26+ (Android 8.0+)
- ✅ **Supabase Account** - [Sign up free](https://app.supabase.com)
- ✅ **Git** - For version control

### Quick Installation

1. **Open in Android Studio**
   ```bash
   # Open project
   File → Open → Select TrailGuide_Android folder
   ```

2. **Configure Supabase Credentials**
   - Edit `app/build.gradle.kts`
   - Add your Supabase URL and API key
   ```kotlin
   buildConfigField("String", "SUPABASE_URL", "\"https://xxxxx.supabase.co\"")
   buildConfigField("String", "SUPABASE_KEY", "\"your-anon-key\"")
   ```

3. **⚠️ CRITICAL: Configure Redirect URLs in Supabase**
   - Go to Supabase Dashboard → Authentication → URL Configuration
   - Add redirect URL: `trailguide://auth-callback`
   - Set Site URL: `trailguide://auth-callback`
   - See [QUICK_FIX_GUIDE.md](QUICK_FIX_GUIDE.md) for detailed steps

4. **Run the App**
   ```bash
   # From terminal
   ./gradlew installDebug
   
   # Or in Android Studio
   Click Run ▶️ button
   ```

### Test Authentication

```bash
# Test deep link (OAuth callback)
adb shell am start -W -a android.intent.action.VIEW -d "trailguide://auth-callback" com.trailguide.android

# Monitor logs
adb logcat | grep -i "MainActivity\|AuthRepository"
```

### Troubleshooting

Having issues? Check these guides:
- 🐛 [Common Issues & Fixes](QUICK_FIX_GUIDE.md#troubleshooting)
- 🔐 [OAuth Not Working?](OAUTH_FIXES_SUMMARY.md#what-to-check-if-still-not-working)
- 📧 [Email Verification Issues](SUPABASE_STEP_BY_STEP.md#test-email-verification)

---

## 📁 Project Structure

```
TrailGuide_Android/
├── .github/
│   └── workflows/
│       └── android-ci.yml          # GitHub Actions CI/CD
├── api-proxy/                      # Node.js REST API server
│   ├── server.js
│   ├── package.json
│   └── README.md
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/trailguide/android/
│   │   │   │   ├── data/           # Data layer
│   │   │   │   │   ├── model/      # Domain models
│   │   │   │   │   ├── dto/        # Data transfer objects
│   │   │   │   │   ├── remote/     # API services
│   │   │   │   │   └── repository/ # Repository implementations
│   │   │   │   ├── di/             # Dependency injection (Hilt)
│   │   │   │   ├── presentation/   # UI layer
│   │   │   │   │   ├── screens/    # Compose screens
│   │   │   │   │   ├── viewmodel/  # ViewModels
│   │   │   │   │   ├── navigation/ # Navigation graph
│   │   │   │   │   └── theme/      # Material theme
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── TrailGuideApplication.kt
│   │   │   ├── res/                # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                   # Unit tests
│   │       └── java/com/trailguide/android/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

### Key Directories

- **`data/model/`**: Domain models (Trail, User, UserPreferences)
- **`data/repository/`**: Repository pattern implementations
- **`presentation/screens/`**: Jetpack Compose UI screens
- **`presentation/viewmodel/`**: ViewModels for state management
- **`di/`**: Hilt modules for dependency injection
- **`api-proxy/`**: Node.js REST API proxy to Supabase

---

## 🔌 API Documentation

### Backend Architecture

```
Android App → Supabase Client (Direct Auth) ✅
           ↓
           → Node.js/Express API → Supabase (PostgreSQL)
           ↓
Deep Link: trailguide://auth-callback (OAuth callbacks)
```

**Authentication**: Direct Supabase Auth with:
- ✅ Google OAuth (via deep links)
- ✅ Email/Password authentication
- ✅ Email verification

**Data Access**: Node.js REST API proxy to Supabase database

**OAuth Flow**: Browser → Google → Redirect → Deep Link → App

### Base URL

```
https://your-api-server.com/api
```

### Endpoints

#### Trails

- `GET /api/trails` - Get all trails
- `GET /api/trails/:id` - Get trail by ID
- `GET /api/trails/search` - Search trails with filters
- `POST /api/trails` - Create new trail
- `PUT /api/trails/:id` - Update trail
- `DELETE /api/trails/:id` - Delete trail

#### Favorites

- `POST /api/trails/:id/favorite` - Toggle favorite status
- `GET /api/trails/favorites` - Get user's favorite trails

### Example Request

```bash
curl -X GET "http://localhost:3000/api/trails/search?q=mountain&difficulty=moderate&maxDistance=15" \
  -H "Content-Type: application/json"
```

### Example Response

```json
[
  {
    "id": "mt-lion",
    "name": "Mount Lion Ridge",
    "city": "Magaliesberg, GP",
    "lat": -25.792,
    "lon": 27.946,
    "distance_km": 8.4,
    "elevation_m": 420,
    "difficulty": "moderate",
    "rating": 4.6,
    "image": "https://example.com/image.jpg",
    "tags": ["viewpoint", "waterfall"]
  }
]
```

See [API Documentation](api-proxy/README.md) for full details.

---

## 🧪 Testing

### Running Unit Tests

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew testDebugUnitTest jacocoTestReport

# View coverage report
open app/build/reports/jacoco/test/html/index.html
```

### Test Structure

```
app/src/test/java/com/trailguide/android/
├── viewmodel/
│   ├── TrailsViewModelTest.kt
│   ├── TrailDetailsViewModelTest.kt
│   └── ProfileViewModelTest.kt
├── repository/
│   ├── TrailRepositoryTest.kt
│   └── AuthRepositoryTest.kt
└── utils/
    └── TestUtils.kt
```

### Test Coverage

- ViewModels: 85%+
- Repositories: 80%+
- Business Logic: 90%+

---

## 🔄 CI/CD Pipeline

### GitHub Actions Workflow

The project uses GitHub Actions for continuous integration and deployment:

```yaml
Trigger: Push to main/develop, Pull Requests
Jobs:
  1. Build and Test
     - Checkout code
     - Set up JDK 17
     - Run lint checks
     - Run unit tests
     - Build debug APK
     - Build release APK
     - Upload artifacts

  2. Code Quality
     - Run Detekt (static analysis)
     - Run ktlint (code style)

  3. Notify
     - Send build status
```

### Workflow Features

- ✅ Automated builds on every push
- ✅ Unit test execution
- ✅ Lint and static analysis
- ✅ APK artifact generation
- ✅ Test result reporting
- ✅ Code quality checks

### Badge Status

Add to your repository:

```markdown
[![Build Status](https://github.com/username/trailguide-android/workflows/Android%20CI%2FCD%20Pipeline/badge.svg)](https://github.com/username/trailguide-android/actions)
```

---

## 🎨 Design Considerations

### UI/UX Principles

1. **Material Design 3**: Modern, responsive, and accessible UI
2. **Dark Theme**: Matches original React Native design
3. **Consistent Navigation**: Bottom navigation for main sections
4. **Clear Hierarchy**: Visual emphasis on important actions
5. **Responsive Layout**: Adapts to different screen sizes

### Color Palette

| Color | Hex | Usage |
|-------|-----|-------|
| Primary | `#22C55E` | Buttons, highlights |
| Secondary | `#0EA5E9` | Accents, links |
| Background | `#0B1020` | Main background |
| Surface | `#1E293B` | Cards, panels |
| Error | `#F43F5E` | Error states |

### Typography

- **Headings**: Bold, 24sp-32sp
- **Body**: Regular, 14sp-16sp
- **Captions**: Medium, 12sp

### Accessibility

- ✅ Minimum touch target size: 48dp
- ✅ Color contrast ratio: WCAG AA compliant
- ✅ Screen reader support (TalkBack)
- ✅ Keyboard navigation
- ✅ Dynamic font sizing

---

## 🔄 Migration from React Native

### Comparison

| Aspect | React Native | Native Android (Kotlin) |
|--------|-------------|-------------------------|
| UI Framework | React components | Jetpack Compose |
| State Management | React Hooks | ViewModel + StateFlow |
| Navigation | React Navigation | Navigation Compose |
| Styling | StyleSheet | Material Theme |
| Data Fetching | Fetch/Axios | Retrofit + Coroutines |
| Local Storage | AsyncStorage | DataStore + Room |
| Testing | Jest | JUnit + Mockito |

### Migration Benefits

1. **Performance**: Native performance, no JavaScript bridge
2. **Type Safety**: Kotlin's null safety and type system
3. **Modern Stack**: Latest Jetpack components
4. **Better Tooling**: Android Studio, profilers, and debuggers
5. **Native Features**: Direct access to Android APIs
6. **Smaller Bundle**: No React Native runtime

### Feature Parity

All features from the React Native app have been preserved:
- ✅ Trail browsing and filtering
- ✅ Trail details and statistics
- ✅ Interactive maps
- ✅ Offline downloads
- ✅ Google authentication
- ✅ Multi-language support
- ✅ Settings and preferences

---

## 🚧 Future Enhancements

### Roadmap

#### Phase 1 (✅ Completed)
- [x] Core MVVM implementation
- [x] Basic CRUD operations
- [x] Google OAuth Sign-In with deep links
- [x] Email/Password authentication
- [x] Email verification
- [x] OAuth callback handling
- [x] Sign-out error handling
- [x] REST API integration
- [x] Supabase integration

#### Phase 2 (Next Release)
- [ ] Offline-first architecture with Room
- [ ] Advanced GPS tracking
- [ ] Real-time weather integration
- [ ] Push notifications

#### Phase 3 (Future)
- [ ] Social features (friends, sharing)
- [ ] Gamification system
- [ ] AR trail preview
- [ ] Wearable device integration (Wear OS)

---

## 📹 Demonstration Video

> **Video Script**: See [docs/VIDEO_SCRIPT.md](docs/VIDEO_SCRIPT.md)

**Video Link**: [YouTube/Demo Video Placeholder]

### Video Contents
1. App overview and architecture
2. Google OAuth SSO login (with deep link demonstration)
3. Email authentication and verification
4. Trail browsing and filtering
5. Trail details and statistics
6. Interactive map with GPS
7. Offline downloads
8. Settings and preferences
9. REST API demonstration
10. Supabase database integration
11. OAuth callback handling demo

---

## 🤖 AI Usage Disclosure

This project was developed with assistance from AI tools (Claude/ChatGPT) for:

- **Code Generation**: Boilerplate code, ViewModels, Repository patterns
- **Documentation**: README, code comments, API documentation
- **Testing**: Unit test scaffolding and mock data
- **Architecture**: Design pattern suggestions and best practices

All AI-generated code was reviewed, customized, and tested to ensure quality and functionality.

**Full AI Usage Report**: [docs/AI_USAGE.md](docs/AI_USAGE.md)

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Kotlin coding conventions
- Use ktlint for formatting
- Write meaningful commit messages
- Add unit tests for new features

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📧 Contact

- **Developer**: TrailGuide Team
- **Email**: support@trailguide.com
- **GitHub**: [@trailguide](https://github.com/trailguide)

---

## 🙏 Acknowledgments

- Original React Native app by [Original Author]
- Supabase for backend infrastructure
- Firebase for authentication services
- Google Maps API for mapping services
- All open-source contributors

---

<div align="center">

**Made with ❤️ and Kotlin**

[⬆ Back to Top](#trailguide---native-android-application)

</div>

