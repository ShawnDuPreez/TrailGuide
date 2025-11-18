import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

kotlin {
    jvmToolchain(21)
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.trailguide.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.trailguide.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // API Keys from local.properties (not committed to Git)
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${localProperties.getProperty("SUPABASE_KEY") ?: ""}\"")
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${localProperties.getProperty("OPENWEATHER_API_KEY") ?: ""}\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${localProperties.getProperty("GOOGLE_MAPS_API_KEY") ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEATHER_API_KEY", "\"${localProperties.getProperty("GOOGLE_WEATHER_API_KEY") ?: ""}\"")
        buildConfigField("String", "OPENROUTE_API_KEY", "\"${localProperties.getProperty("OPENROUTE_API_KEY") ?: ""}\"")
        buildConfigField("String", "GOOGLE_TRANSLATE_API_KEY", "\"${localProperties.getProperty("GOOGLE_TRANSLATE_API_KEY") ?: ""}\"")
        
        // Add Maps API key to manifest
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("GOOGLE_MAPS_API_KEY") ?: ""
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // Temporarily disabled to avoid OneDrive lock issues
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Render deployment URL - Production API server
            buildConfigField("String", "API_BASE_URL", "\"https://trailguide-api.onrender.com/\"")
        }
        debug {
            isMinifyEnabled = false
            
            // Render deployment URL - for testing distributed APKs
            buildConfigField("String", "API_BASE_URL", "\"https://trailguide-api.onrender.com/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Retrofit for REST API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Hilt for Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Supabase Authentication & Client
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.0")
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-utils:2.3.7")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Image Loading - Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Room Database (optional - for offline caching)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")  // For kotlin.test assertions
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Task to start the API server before running the app
tasks.register<Exec>("startApiServer") {
    group = "development"
    description = "Starts the Node.js API server in background"
    
    workingDir = file("${project.rootDir}/api-proxy")
    
    // Check if running on Windows
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", "start", "cmd", "/k", "npm start")
    } else {
        // Mac/Linux
        commandLine("sh", "-c", "npm start &")
    }
    
    doFirst {
        println("Starting API server from ${workingDir.absolutePath}")
        println("Server will run on http://localhost:3000")
    }
}

// Task to stop the API server
tasks.register<Exec>("stopApiServer") {
    group = "development"
    description = "Stops the Node.js API server"
    
    // Check if running on Windows
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", "taskkill", "/FI", "WindowTitle eq *npm start*", "/T", "/F")
    } else {
        // Mac/Linux
        commandLine("sh", "-c", "pkill -f 'node.*server.js'")
    }
    
    isIgnoreExitValue = true // Don't fail if server is not running
    
    doFirst {
        println("Stopping API server...")
    }
}

// Task to start server and install app in one command
tasks.register("runWithServer") {
    group = "development"
    description = "Starts API server and installs the app"
    
    dependsOn("startApiServer")
    finalizedBy("installDebug")
    
    doLast {
        println("")
        println("========================================")
        println("  Development Environment Ready!")
        println("========================================")
        println("API Server: http://localhost:3000")
        println("Android App: Installed on device/emulator")
        println("")
        println("Use './gradlew stopApiServer' to stop the server")
    }
}

