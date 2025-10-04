# TrailGuide Android - Demonstration Video Script

**Duration**: 8-10 minutes  
**Format**: Screen recording with voiceover  
**Target Audience**: Academic evaluation (POE submission)

---

## Opening (0:00 - 0:30)

### Visual
- Show app icon and splash screen
- Fade to home screen

### Script
> "Hello! Today I'll be demonstrating TrailGuide, a native Android application built with Kotlin and Jetpack Compose. This app was migrated from a React Native Expo application and represents a complete implementation of the MVVM architecture with modern Android development practices."

> "TrailGuide helps hikers discover, plan, and navigate trails with features like offline maps, GPS tracking, and multi-language support."

---

## Section 1: App Architecture Overview (0:30 - 1:30)

### Visual
- Show Android Studio project structure
- Highlight folder organization (data, presentation, di)
- Show key files: MainActivity, ViewModels, Repositories

### Script
> "Let's start with the architecture. TrailGuide follows the MVVM pattern with clean architecture principles."

> "The project is organized into three main layers:"
> "1. **Presentation Layer** - Jetpack Compose screens and ViewModels"
> "2. **Domain Layer** - Business logic and data models"
> "3. **Data Layer** - Repositories and API services"

> "We're using Hilt for dependency injection, which you can see configured in our AppModule. This ensures loose coupling and testability."

> "All network calls are handled by Retrofit with coroutines for asynchronous operations, and state is managed using StateFlow for reactive updates."

---

## Section 2: Single Sign-On (SSO) with Google (1:30 - 3:00)

### Visual
- Navigate to Profile screen
- Show "Guest" state
- Tap "Sign In with Google"
- Show Google account picker
- Complete sign-in flow
- Show authenticated state with user email

### Script
> "One of the core POE requirements is SSO authentication. I'll demonstrate Google Sign-In using Firebase Authentication."

> "Currently, the app shows me as a Guest user. When I tap 'Sign In with Google'..."

> [Tap button]

> "...the app launches the Google account picker using Firebase Auth. I'll select my account..."

> [Complete sign-in]

> "And now you can see I'm successfully authenticated. The app displays my email address and provides a 'Sign Out' option."

> "Under the hood, this uses Firebase Authentication's Google provider. The ViewModel observes the auth state and updates the UI reactively using StateFlow."

> "Let me show you the auth flow in code..."

> [Show AuthRepository.kt]

> "Here's the AuthRepository that handles sign-in. It takes the Google ID token, creates Firebase credentials, and returns a User model. All error handling is managed through our NetworkResult sealed class."

---

## Section 3: Settings Screen (3:00 - 4:00)

### Visual
- Still on Profile screen
- Toggle biometric authentication
- Toggle notifications
- Switch between languages (EN → AF → ZU)
- Show UI text updating

### Script
> "The POE requirements also include a comprehensive settings screen. TrailGuide provides several customization options."

> "First, we have biometric authentication toggle. When enabled, users can unlock the app with fingerprint or face recognition."

> [Toggle biometrics]

> "Next, notifications settings for trail reminders and safety alerts."

> [Toggle notifications]

> "And here's something unique - multi-language support. TrailGuide is the first trail app to support isiZulu, making it accessible to a broader South African audience."

> [Switch languages]

> "Watch as I switch from English to Afrikaans... and now to isiZulu. The entire interface updates immediately."

> "These preferences are stored using Jetpack DataStore, which is the modern replacement for SharedPreferences. The PreferencesRepository exposes settings as a Flow, and the UI observes changes reactively."

---

## Section 4: REST API - Browse Trails (4:00 - 5:30)

### Visual
- Navigate to Trails screen
- Show loading indicator
- Show trail cards populating
- Open Android Studio to show code
- Open API server terminal showing logs

### Script
> "Now let's demonstrate the REST API integration - a key POE requirement."

> "When the app launches, it immediately fetches trails from our Node.js REST API server."

> [Show loading]

> "You can see the loading indicator while the API call is in progress."

> [Show trails]

> "And here are the trails, loaded from the API. Each card displays trail information including name, location, difficulty, distance, elevation, and rating."

> [Show API server terminal]

> "On my development server, you can see the API logs showing the GET request to `/api/trails`. The Node.js Express server queries Supabase and returns the data."

> [Show code: TrailRepository.kt]

> "In the code, here's the TrailRepository making the API call using Retrofit. It returns a Flow that emits Loading, Success, or Error states."

> [Show code: TrailsViewModel.kt]

> "The ViewModel collects this Flow and updates the StateFlow, which triggers UI recomposition."

> "This demonstrates the complete MVVM pattern with reactive data flow from API to UI."

---

## Section 5: REST API - CRUD Operations (5:30 - 7:00)

### Visual
- Show search functionality
- Apply filters (difficulty, distance)
- Tap on a trail to view details
- Toggle favorite (POST request)
- Show Postman/Thunder Client with API calls
- Demonstrate CREATE, UPDATE, DELETE in API testing tool

### Script
> "The API supports full CRUD operations. Let me demonstrate."

> "First, **Read** - we're already viewing trails. But I can also search and filter."

> [Use search and filters]

> "The search sends a GET request to `/api/trails/search` with query parameters. The API filters results server-side and returns matching trails."

> [Tap trail to view details]

> "When I tap a trail, it loads detailed information via `/api/trails/:id`."

> [Tap favorite button]

> "Toggling favorite sends a POST request to `/api/trails/:id/favorite`. You can see the heart icon updating immediately with optimistic UI updates."

> [Switch to API testing tool]

> "Now let me show the other CRUD operations using Postman."

> "**Create** - POST to `/api/trails` with trail data creates a new trail."

> [Show POST request and response]

> "**Update** - PUT to `/api/trails/:id` modifies an existing trail."

> [Show PUT request]

> "**Delete** - DELETE to `/api/trails/:id` removes a trail."

> [Show DELETE request]

> "All operations return appropriate HTTP status codes and handle errors gracefully."

---

## Section 6: Extra Features (7:00 - 8:30)

### Visual
- Show Map screen with Google Maps
- Demonstrate map type switching
- Show Downloads screen with storage management
- Navigate to trail details and show segments
- Quick tour of gamification features (if implemented)

### Script
> "Beyond the minimum requirements, TrailGuide includes several extra features."

> "**Interactive Maps** - Full Google Maps integration with trail markers, multiple map types, and GPS tracking."

> [Demonstrate map features]

> "**Offline Downloads** - Users can download trails for offline access, essential for remote areas with no connectivity."

> [Show Downloads screen]

> "The storage management interface shows space usage and allows users to manage downloaded content."

> "**Trail Segments** - Each trail is divided into segments with specific characteristics like 'Steep', 'Exposed', or 'Family-friendly'."

> [Show segments on trail details]

> "This helps hikers plan and prepare for different trail sections."

---

## Section 7: Supabase Database (8:30 - 9:00)

### Visual
- Open Supabase dashboard in browser
- Show `trails` table with data
- Show table structure
- Correlate data with app display

### Script
> "Finally, let me show you the backend database."

> [Show Supabase dashboard]

> "TrailGuide uses Supabase, which provides a PostgreSQL database with a REST API."

> "Here's the `trails` table with all our trail data. Each row corresponds to a trail card you saw in the app."

> [Show table structure]

> "The schema includes fields for name, location, GPS coordinates, difficulty, rating, and more."

> "Our Node.js API server acts as a proxy, providing a custom REST API layer over Supabase. This fulfills the requirement of 'creating my own API' while leveraging cloud infrastructure."

---

## Section 8: Testing & CI/CD (9:00 - 9:30)

### Visual
- Show test files in Android Studio
- Run unit tests (./gradlew test)
- Show GitHub Actions workflow
- Show build badges

### Script
> "TrailGuide includes comprehensive testing and CI/CD."

> "Here are our unit tests for ViewModels and Repositories, written with JUnit and Mockito."

> [Run tests]

> "All tests pass, covering business logic, data transformation, and state management."

> [Show GitHub Actions workflow]

> "We've configured GitHub Actions for continuous integration. Every push triggers automated builds, tests, and APK generation."

> "The workflow ensures code quality and catches issues early."

---

## Closing (9:30 - 10:00)

### Visual
- Return to app home screen
- Show smooth navigation between screens
- Final pan across UI

### Script
> "To summarize, TrailGuide demonstrates:"
> "- ✅ **Native Android development** with Kotlin and Jetpack Compose"
> "- ✅ **MVVM architecture** with ViewModel, Repository, and LiveData"
> "- ✅ **REST API integration** with custom Node.js server"
> "- ✅ **Firebase authentication** with Google SSO"
> "- ✅ **Comprehensive settings** with preferences storage"
> "- ✅ **CRUD operations** with full error handling"
> "- ✅ **Extra features** like maps, offline support, and multi-language"
> "- ✅ **Unit testing** with JUnit and Mockito"
> "- ✅ **CI/CD pipeline** with GitHub Actions"

> "The app is fully functional, well-architected, and ready for production deployment."

> "Thank you for watching!"

---

## Technical Notes for Recording

### Setup Checklist
- [ ] Clear app data for fresh demo
- [ ] Ensure API server is running
- [ ] Have Google account ready for sign-in
- [ ] Prepare Postman/Thunder Client with API calls
- [ ] Open Supabase dashboard in browser
- [ ] Have code files ready in Android Studio
- [ ] Check audio levels and microphone
- [ ] Use high-quality screen recording (1080p minimum)

### Recording Tips
- **Screen Resolution**: 1920x1080 or higher
- **Frame Rate**: 30 FPS minimum
- **Audio**: Clear voiceover, no background noise
- **Editing**: Add zoom effects for code sections
- **Captions**: Consider adding subtitles for clarity
- **Transitions**: Smooth transitions between sections
- **Timing**: Keep sections concise, avoid long pauses

### Files to Show
1. `MainActivity.kt` - Entry point
2. `TrailsViewModel.kt` - State management
3. `TrailRepository.kt` - Data layer
4. `AuthRepository.kt` - Authentication
5. `TrailApiService.kt` - API interface
6. `api-proxy/server.js` - REST API server
7. Test files - Unit tests

---

## Backup Script (If Technical Issues)

If live demo encounters issues, have these alternatives ready:

1. **Screenshots**: High-quality screenshots of each screen
2. **Recorded Clips**: Pre-recorded clips of each feature
3. **Code Walkthrough**: Focus more on code explanation
4. **Emulator**: Have both physical device and emulator ready

---

**Script Version**: 1.0  
**Last Updated**: October 2025  
**Estimated Recording Time**: 45-60 minutes (for 10-minute final video)

