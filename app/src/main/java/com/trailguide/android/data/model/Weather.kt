package com.trailguide.android.data.model

/**
 * Weather data model for trail conditions.
 */
data class Weather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val description: String,
    val icon: String,
    val condition: WeatherCondition
)

/**
 * Weather forecast for multiple days.
 */
data class WeatherForecast(
    val current: Weather,
    val forecast: List<DayForecast>,
    val trailSafetyRating: SafetyRating
)

/**
 * Daily forecast data.
 */
data class DayForecast(
    val date: String,
    val tempMin: Double,
    val tempMax: Double,
    val description: String,
    val icon: String,
    val precipitationChance: Int
)

/**
 * Weather conditions for icon mapping.
 */
enum class WeatherCondition(val displayName: String, val icon: String) {
    CLEAR("Clear", "☀️"),
    PARTLY_CLOUDY("Partly Cloudy", "⛅"),
    CLOUDY("Cloudy", "☁️"),
    RAIN("Rain", "🌧️"),
    HEAVY_RAIN("Heavy Rain", "⛈️"),
    SNOW("Snow", "❄️"),
    THUNDERSTORM("Thunderstorm", "⚡"),
    FOG("Fog", "🌫️"),
    WIND("Windy", "💨");

    companion object {
        fun fromDescription(description: String): WeatherCondition {
            return when {
                description.contains("clear", ignoreCase = true) -> CLEAR
                description.contains("clouds", ignoreCase = true) && 
                    description.contains("few", ignoreCase = true) -> PARTLY_CLOUDY
                description.contains("cloud", ignoreCase = true) -> CLOUDY
                description.contains("rain", ignoreCase = true) && 
                    description.contains("heavy", ignoreCase = true) -> HEAVY_RAIN
                description.contains("rain", ignoreCase = true) -> RAIN
                description.contains("snow", ignoreCase = true) -> SNOW
                description.contains("thunderstorm", ignoreCase = true) -> THUNDERSTORM
                description.contains("fog", ignoreCase = true) || 
                    description.contains("mist", ignoreCase = true) -> FOG
                description.contains("wind", ignoreCase = true) -> WIND
                else -> CLEAR
            }
        }
        
        /**
         * Map Google Weather API condition type to WeatherCondition enum.
         */
        fun fromGoogleType(type: String): WeatherCondition {
            return when (type.uppercase()) {
                "CLEAR" -> CLEAR
                "PARTLY_CLOUDY" -> PARTLY_CLOUDY
                "CLOUDY" -> CLOUDY
                "RAIN", "LIGHT_RAIN", "RAIN_SHOWERS", "SCATTERED_SHOWERS" -> RAIN
                "HEAVY_RAIN" -> HEAVY_RAIN
                "THUNDERSTORM" -> THUNDERSTORM
                "SNOW" -> SNOW
                "FOG" -> FOG
                else -> CLEAR // Default fallback
            }
        }
    }
}

/**
 * Trail safety rating based on weather conditions.
 */
enum class SafetyRating(val displayName: String, val color: Long) {
    EXCELLENT("Excellent", 0xFF22C55E),
    GOOD("Good", 0xFF84CC16),
    MODERATE("Moderate", 0xFFFACC15),
    POOR("Poor", 0xFFF97316),
    DANGEROUS("Dangerous", 0xFFEF4444);

    companion object {
        fun fromWeather(weather: Weather): SafetyRating {
            return when {
                weather.windSpeed > 50 || weather.condition == WeatherCondition.THUNDERSTORM -> DANGEROUS
                weather.windSpeed > 35 || weather.condition == WeatherCondition.HEAVY_RAIN -> POOR
                weather.windSpeed > 25 || weather.condition == WeatherCondition.RAIN -> MODERATE
                weather.condition == WeatherCondition.CLOUDY -> GOOD
                else -> EXCELLENT
            }
        }
    }
}


