package com.trailguide.android.data.osm

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for OpenStreetMap Overpass API
 * Used to fetch real hiking trails with accurate geometry
 * 
 * Overpass API Documentation: https://wiki.openstreetmap.org/wiki/Overpass_API
 */
interface OverpassApiService {
    
    companion object {
        // Public Overpass API endpoints (rate-limited but free)
        const val BASE_URL = "https://overpass-api.de/api/"
        
        // Alternative endpoints for failover
        val ALTERNATIVE_ENDPOINTS = listOf(
            "https://overpass.kumi.systems/api/",
            "https://overpass-api.de/api/",
            "https://z.overpass-api.de/api/"
        )
        
        /**
         * Build Overpass QL query for hiking trails
         * 
         * @param lat Center latitude
         * @param lng Center longitude
         * @param radius Search radius in meters (default 5000m = 5km)
         * @return Overpass QL query string
         */
        fun buildHikingTrailsQuery(lat: Double, lng: Double, radius: Int = 5000): String {
            // Balanced query: hiking trails and nature paths, excluding urban footways
            // More flexible to work across different regions and tagging conventions
            return """
                [out:json][timeout:15];
                (
                  way[highway="path"](around:$radius,$lat,$lng);
                  way[highway="track"][tracktype~"grade[2-5]"](around:$radius,$lat,$lng);
                  way[route="hiking"](around:$radius,$lat,$lng);
                  relation[route="hiking"](around:$radius,$lat,$lng);
                );
                out geom;
            """.trimIndent()
        }
        
        /**
         * Build query for a specific trail by ID
         */
        fun buildTrailByIdQuery(osmId: Long): String {
            return """
                [out:json];
                way($osmId);
                out geom;
            """.trimIndent()
        }
        
        /**
         * Build query for trails by name search
         */
        fun buildTrailSearchQuery(
            searchQuery: String,
            lat: Double,
            lng: Double,
            radius: Int = 10000
        ): String {
            return """
                [out:json][timeout:25];
                (
                  way[highway~"^(path|footway|track|bridleway)$"]["name"~"$searchQuery",i](around:$radius,$lat,$lng);
                );
                out geom;
            """.trimIndent()
        }
        
        /**
         * Build advanced query with difficulty filters
         */
        fun buildAdvancedTrailsQuery(
            lat: Double,
            lng: Double,
            radius: Int = 5000,
            minDifficulty: String? = null,
            trailTypes: List<String>? = null
        ): String {
            val typesFilter = trailTypes?.joinToString("|") { "^$it$" } 
                ?: "^(path|footway|track|bridleway)$"
            
            val difficultyFilter = when (minDifficulty) {
                "easy" -> "[sac_scale~\"hiking|mountain_hiking\"]"
                "moderate" -> "[sac_scale~\"demanding_mountain_hiking\"]"
                "difficult" -> "[sac_scale~\"alpine_hiking|demanding_alpine_hiking\"]"
                else -> ""
            }
            
            return """
                [out:json][timeout:25];
                (
                  way[highway~"$typesFilter"]$difficultyFilter(around:$radius,$lat,$lng);
                );
                out geom;
            """.trimIndent()
        }
        
        /**
         * Build query for hiking trails within a specific boundary (OSM relation/way)
         * This is the KEY query for getting trails inside nature reserves
         * 
         * @param osmId OSM ID of the boundary (relation or way)
         * @param osmType Type of OSM object ("relation" or "way")
         * @return Overpass QL query string
         */
        fun buildBoundaryTrailsQuery(osmId: Long, osmType: String = "relation"): String {
            // Convert OSM ID to area ID based on type
            val areaId = when (osmType) {
                "relation" -> osmId + 3600000000
                "way" -> osmId + 2400000000
                else -> osmId
            }
            
            return """
                [out:json][timeout:30];
                area($areaId) -> .searchArea;
                (
                  way["highway"~"path|footway|track|bridleway"](area.searchArea);
                  way["route"="hiking"](area.searchArea);
                );
                out geom;
            """.trimIndent()
        }
        
        /**
         * Alternative boundary query using bounding box
         * Fallback when area query fails
         */
        fun buildBBoxTrailsQuery(
            south: Double, 
            west: Double, 
            north: Double, 
            east: Double
        ): String {
            return """
                [out:json][timeout:25];
                (
                  way["highway"~"path|footway|track|bridleway"]($south,$west,$north,$east);
                  way["route"="hiking"]($south,$west,$north,$east);
                );
                out geom;
            """.trimIndent()
        }
    }
    
    /**
     * Execute Overpass QL query
     * 
     * @param query Overpass QL query string
     * @return Response containing OSM elements
     */
    @GET("interpreter")
    suspend fun executeQuery(
        @Query("data") query: String
    ): Response<OverpassResponse>
}
