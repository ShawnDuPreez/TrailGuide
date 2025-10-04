package com.trailguide.android.data.model

/**
 * User data model for authentication and profile management.
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val provider: AuthProvider = AuthProvider.GOOGLE
)

/**
 * Authentication providers supported by the app.
 */
enum class AuthProvider {
    GOOGLE,
    EMAIL,
    ANONYMOUS
}

/**
 * User preferences for settings screen.
 */
data class UserPreferences(
    val language: Language = Language.ENGLISH,
    val biometricsEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val offlineMapsEnabled: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM
)

/**
 * Supported languages in the app.
 */
enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    AFRIKAANS("af", "Afrikaans"),
    ZULU("zu", "isiZulu");

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}

/**
 * App theme options.
 */
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

