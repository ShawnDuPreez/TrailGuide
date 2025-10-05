package com.trailguide.android.data.osm

import android.util.Log
import com.trailguide.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for interacting with the Overpass API to fetch OpenStreetMap data.
 * Handles the complex Overpass QL queries for hiking trails in South Africa.
 */
class OverpassApiService {
    
    companion object {
        private const val TAG = "OverpassApiService"
        private const val OVERPASS_BASE_URL = "https://overpass-api.de/api/interpreter"
        private const val TIMEOUT_SECONDS = 120L // Extended timeout for large queries
        
        // South Africa bounding box
        private const val SA_LAT_MIN = -35.0
        private const val SA_LON_MIN = 16.0
        private const val SA_LAT_MAX = -22.0
        private const val SA_LON_MAX = 33.0
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    /**
     * Test the Overpass API connection with a simple query.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing Overpass API connection...")
            
            val testQuery = """
                [out:json][timeout:10];
                node["amenity"="restaurant"](around:1000,-33.9249,18.4241);
                out;
            """.trimIndent()
            
            val response = executeQuery(testQuery)
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Test query successful, response length: ${responseBody?.length ?: 0}")
                Result.success("Connection test successful")
            } else {
                Log.e(TAG, "Test query failed: ${response.code} - ${response.message}")
                Result.failure(Exception("Test query failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing Overpass API connection", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fetch all hiking trails in South Africa from OpenStreetMap.
     * Uses Overpass QL to query for paths, foot routes, and hiking routes.
     */
    suspend fun fetchHikingTrails(): Result<List<HikingTrail>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Overpass API query for hiking trails in South Africa...")
            
            val query = buildOverpassQuery()
            val response = executeQuery(query)
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Log.d(TAG, "Response body length: ${responseBody.length}")
                    val trails = parseOverpassResponse(responseBody)
                    Log.d(TAG, "Successfully fetched ${trails.size} hiking trails from OSM")
                    Result.success(trails)
                } else {
                    Log.e(TAG, "Empty response body from Overpass API")
                    Result.failure(Exception("Empty response from Overpass API"))
                }
            } else {
                Log.e(TAG, "Overpass API request failed: ${response.code} - ${response.message}")
                Result.failure(Exception("Overpass API request failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hiking trails from Overpass API", e)
            Result.failure(e)
        }
    }
    
    /**
     * Build the Overpass QL query for hiking trails in South Africa.
     * Using a very small area around Table Mountain to test first.
     * Only query for ways (lines), not relations (areas).
     */
    private fun buildOverpassQuery(): String {
        // Start with a very small area around Table Mountain, Cape Town
        // Only query for ways (line features), not relations (area features)
        return """
            [out:json][timeout:30];
            (
              way["highway"="path"](-33.97, 18.40, -33.90, 18.45);
              way["highway"="footway"](-33.97, 18.40, -33.90, 18.45);
              way["route"="hiking"](-33.97, 18.40, -33.90, 18.45);
            );
            out geom;
        """.trimIndent()
    }
    
    /**
     * Execute the Overpass query via HTTP POST.
     */
    private suspend fun executeQuery(query: String): Response = withContext(Dispatchers.IO) {
        Log.d(TAG, "Executing Overpass query: $query")
        
        val requestBody = query.toRequestBody("text/plain".toMediaType())
        val request = Request.Builder()
            .url(OVERPASS_BASE_URL)
            .post(requestBody)
            .addHeader("User-Agent", "TrailGuide-Android/${BuildConfig.VERSION_NAME}")
            .build()
        
        Log.d(TAG, "Sending request to Overpass API...")
        val response = client.newCall(request).execute()
        Log.d(TAG, "Received response: ${response.code} - ${response.message}")
        
        response
    }
    
    /**
     * Parse the JSON response from Overpass API into HikingTrail objects.
     */
    private fun parseOverpassResponse(jsonResponse: String): List<HikingTrail> {
        val trails = mutableListOf<HikingTrail>()
        val nodeMap = mutableMapOf<Long, OsmNode>()
        
        try {
            val jsonObject = JSONObject(jsonResponse)
            val elements = jsonObject.getJSONArray("elements")
            
            // First pass: collect all nodes
            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                if (element.getString("type") == "node") {
                    val node = parseOsmNode(element)
                    nodeMap[node.id] = node
                }
            }
            
            // Second pass: process only ways (lines), skip relations (areas)
            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                when (element.getString("type")) {
                    "way" -> {
                        val way = parseOsmWay(element, nodeMap)
                        val trail = convertWayToTrail(way)
                        if (trail != null) {
                            trails.add(trail)
                        }
                    }
                    // Skip relations to avoid area/polygon data
                    "relation" -> {
                        Log.d(TAG, "Skipping relation ${element.getLong("id")} to avoid area data")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Overpass API response", e)
        }
        
        return trails.distinctBy { it.id } // Remove duplicates
    }
    
    /**
     * Parse an OSM node from JSON.
     */
    private fun parseOsmNode(jsonObject: JSONObject): OsmNode {
        val tags = if (jsonObject.has("tags")) {
            val tagsJson = jsonObject.getJSONObject("tags")
            val tagsMap = mutableMapOf<String, String>()
            tagsJson.keys().forEach { key ->
                tagsMap[key] = tagsJson.getString(key)
            }
            tagsMap
        } else null
        
        return OsmNode(
            type = jsonObject.getString("type"),
            id = jsonObject.getLong("id"),
            lat = jsonObject.getDouble("lat"),
            lon = jsonObject.getDouble("lon"),
            tags = tags
        )
    }
    
    /**
     * Parse an OSM way from JSON.
     */
    private fun parseOsmWay(jsonObject: JSONObject, nodeMap: Map<Long, OsmNode>): OsmWay {
        val tags = if (jsonObject.has("tags")) {
            val tagsJson = jsonObject.getJSONObject("tags")
            val tagsMap = mutableMapOf<String, String>()
            tagsJson.keys().forEach { key ->
                tagsMap[key] = tagsJson.getString(key)
            }
            tagsMap
        } else null
        
        val nodes = mutableListOf<Long>()
        if (jsonObject.has("nodes")) {
            val nodesArray = jsonObject.getJSONArray("nodes")
            for (i in 0 until nodesArray.length()) {
                nodes.add(nodesArray.getLong(i))
            }
        }
        
        val geometry = mutableListOf<OsmNode>()
        if (jsonObject.has("geometry")) {
            val geometryArray = jsonObject.getJSONArray("geometry")
            for (i in 0 until geometryArray.length()) {
                val geomNode = geometryArray.getJSONObject(i)
                geometry.add(
                    OsmNode(
                        type = "node",
                        id = geomNode.getLong("id"),
                        lat = geomNode.getDouble("lat"),
                        lon = geomNode.getDouble("lon")
                    )
                )
            }
        }
        
        return OsmWay(
            type = jsonObject.getString("type"),
            id = jsonObject.getLong("id"),
            nodes = nodes,
            geometry = geometry,
            tags = tags
        )
    }
    
    /**
     * Parse an OSM relation from JSON.
     */
    private fun parseOsmRelation(jsonObject: JSONObject, nodeMap: Map<Long, OsmNode>): OsmRelation {
        val tags = if (jsonObject.has("tags")) {
            val tagsJson = jsonObject.getJSONObject("tags")
            val tagsMap = mutableMapOf<String, String>()
            tagsJson.keys().forEach { key ->
                tagsMap[key] = tagsJson.getString(key)
            }
            tagsMap
        } else null
        
        val members = mutableListOf<OsmMember>()
        if (jsonObject.has("members")) {
            val membersArray = jsonObject.getJSONArray("members")
            for (i in 0 until membersArray.length()) {
                val memberJson = membersArray.getJSONObject(i)
                val geometry = mutableListOf<OsmNode>()
                
                if (memberJson.has("geometry")) {
                    val geometryArray = memberJson.getJSONArray("geometry")
                    for (j in 0 until geometryArray.length()) {
                        val geomNode = geometryArray.getJSONObject(j)
                        geometry.add(
                            OsmNode(
                                type = "node",
                                id = geomNode.getLong("id"),
                                lat = geomNode.getDouble("lat"),
                                lon = geomNode.getDouble("lon")
                            )
                        )
                    }
                }
                
                members.add(
                    OsmMember(
                        type = memberJson.getString("type"),
                        ref = memberJson.getLong("ref"),
                        role = memberJson.getString("role"),
                        geometry = geometry
                    )
                )
            }
        }
        
        return OsmRelation(
            type = jsonObject.getString("type"),
            id = jsonObject.getLong("id"),
            members = members,
            tags = tags
        )
    }
    
    /**
     * Convert an OSM way to a HikingTrail.
     */
    private fun convertWayToTrail(way: OsmWay): HikingTrail? {
        val coordinates = way.geometry?.map { it.toLatLng() } ?: return null
        
        // Filter out very short trails (less than 2 points or very short distance)
        if (coordinates.size < 2) return null
        
        return HikingTrail(
            id = "way_${way.id}",
            name = way.tags.getTrailName(),
            coordinates = coordinates,
            difficulty = way.tags.getTrailDifficulty(),
            surface = way.tags.getTrailSurface(),
            description = way.tags.getTrailDescription()
        )
    }
    
    /**
     * Convert an OSM relation to multiple HikingTrails.
     */
    private fun convertRelationToTrails(relation: OsmRelation): List<HikingTrail> {
        val trails = mutableListOf<HikingTrail>()
        
        relation.members.forEach { member ->
            if (member.geometry != null && member.geometry.size >= 2) {
                val coordinates = member.geometry.map { it.toLatLng() }
                trails.add(
                    HikingTrail(
                        id = "relation_${relation.id}_member_${member.ref}",
                        name = relation.tags.getTrailName() ?: member.role,
                        coordinates = coordinates,
                        difficulty = relation.tags.getTrailDifficulty(),
                        surface = relation.tags.getTrailSurface(),
                        description = relation.tags.getTrailDescription()
                    )
                )
            }
        }
        
        return trails
    }
}
