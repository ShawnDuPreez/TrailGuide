package com.trailguide.android.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng

object NavigationServiceManager {

    fun startNavigation(
        context: Context,
        trailId: String,
        trailName: String,
        totalDistanceMeters: Double,
        routePolyline: ArrayList<LatLng>? = null
    ) {
        val intent = Intent(context, NavigationService::class.java).apply {
            action = NavigationService.ACTION_START
            putExtra(NavigationService.EXTRA_TRAIL_ID, trailId)
            putExtra(NavigationService.EXTRA_TRAIL_NAME, trailName)
            putExtra(NavigationService.EXTRA_TOTAL_DISTANCE_METERS, totalDistanceMeters)
            putParcelableArrayListExtra(NavigationService.EXTRA_ROUTE_POLYLINE, routePolyline)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun pauseNavigation(context: Context) {
        sendAction(context, NavigationService.ACTION_PAUSE)
    }

    fun resumeNavigation(context: Context) {
        sendAction(context, NavigationService.ACTION_RESUME)
    }

    fun stopNavigation(context: Context) {
        sendAction(context, NavigationService.ACTION_STOP)
    }

    private fun sendAction(context: Context, action: String) {
        val intent = Intent(context, NavigationService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }
}

