package com.trailguide.android.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trailguide.android.data.model.RoutePoint

/**
 * Type converters for Room database.
 * Handles complex data types like Lists.
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromRoutePointList(value: List<RoutePoint>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toRoutePointList(value: String): List<RoutePoint> {
        val listType = object : TypeToken<List<RoutePoint>>() {}.type
        return gson.fromJson(value, listType)
    }
}

