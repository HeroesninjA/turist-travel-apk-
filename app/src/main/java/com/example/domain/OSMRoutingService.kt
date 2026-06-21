package com.example.domain

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.sqrt

object OSMRoutingService {
    // Memory cache for routes to avoid redundant network requests
    private val routeCache = mutableMapOf<String, List<Pair<Double, Double>>>()

    /**
     * Fetches a list of GPS coordinates (Latitude, Longitude) that snap to the streets
     * between a start and end coordinate using the OSRM API.
     */
    suspend fun getStreetSnappedRoute(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        val cacheKey = "%.5f,%.5f_%.5f,%.5f".format(fromLat, fromLng, toLat, toLng)
        synchronized(routeCache) {
            val cached = routeCache[cacheKey]
            if (cached != null) {
                return@withContext cached
            }
        }

        // Use public OSRM driving service - works extremely well for street snap routing
        val urlString = "https://router.project-osrm.org/route/v1/driving/$fromLng,$fromLat;$toLng,$toLat?overview=full&geometries=geojson"
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "MetroOptimiseApp/1.0 (Android; Jetpack Compose; iordache.samuel@gmail.com)")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { reader ->
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    sb.toString()
                }

                val jsonResponse = JSONObject(response)
                val code = jsonResponse.optString("code")
                if (code == "Ok") {
                    val routesArray = jsonResponse.optJSONArray("routes")
                    if (routesArray != null && routesArray.length() > 0) {
                        val routeObj = routesArray.getJSONObject(0)
                        val geometryObj = routeObj.optJSONObject("geometry")
                        if (geometryObj != null) {
                            val coordinatesArray = geometryObj.optJSONArray("coordinates")
                            if (coordinatesArray != null) {
                                val points = mutableListOf<Pair<Double, Double>>()
                                for (i in 0 until coordinatesArray.length()) {
                                    val pointArray = coordinatesArray.getJSONArray(i)
                                    val lng = pointArray.getDouble(0)
                                    val lat = pointArray.getDouble(1)
                                    points.add(Pair(lat, lng))
                                }
                                if (points.isNotEmpty()) {
                                    synchronized(routeCache) {
                                        routeCache[cacheKey] = points
                                    }
                                    return@withContext points
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OSMRoutingService", "Failed to fetch street coordinates from OSRM: ${e.localizedMessage}")
        }

        // Fallback to straight line if API fails or is offline
        val fallback = listOf(Pair(fromLat, fromLng), Pair(toLat, toLng))
        return@withContext fallback
    }

    /**
     * Interpolates coordinates smoothly along a multi-segment list of Lat/Lng coordinates.
     */
    fun interpolateRouteProgress(points: List<Pair<Double, Double>>, progress: Float): Pair<Double, Double> {
        if (points.isEmpty()) return Pair(0.0, 0.0)
        if (points.size == 1) return points[0]

        val segmentLengths = DoubleArray(points.size - 1)
        var totalLength = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val dLat = p2.first - p1.first
            val dLng = p2.second - p1.second
            val len = sqrt(dLat * dLat + dLng * dLng)
            segmentLengths[i] = len
            totalLength += len
        }

        if (totalLength == 0.0) return points[0]

        val targetDist = totalLength * progress.coerceIn(0f, 1f).toDouble()
        var accumulatedDist = 0.0

        for (i in 0 until points.size - 1) {
            val nextDist = accumulatedDist + segmentLengths[i]
            if (targetDist <= nextDist) {
                val segmentProgress = if (segmentLengths[i] > 0.0) {
                    (targetDist - accumulatedDist) / segmentLengths[i]
                } else {
                    0.0
                }
                val start = points[i]
                val end = points[i + 1]
                return Pair(
                    start.first + (end.first - start.first) * segmentProgress,
                    start.second + (end.second - start.second) * segmentProgress
                )
            }
            accumulatedDist = nextDist
        }

        return points.last()
    }
}
