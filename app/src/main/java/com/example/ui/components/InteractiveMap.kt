package com.example.ui.components

import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TouristSpot
import com.example.domain.BusLine
import com.example.domain.BusStation
import com.example.domain.LegType
import com.example.domain.OptimizedJourney
import com.example.domain.TransitNetwork
import com.example.domain.OSMRoutingService
import com.example.domain.LiveTransitVehicle
import com.example.domain.TransportOperator
import com.example.domain.TransportApiEngine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.ui.screens.translateDirections
import com.example.ui.screens.translateSpotName
import com.example.ui.screens.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

// Math helper functions for OSM/CartoDB Slippy Map Tiles
fun getTileX(lng: Double, zoom: Int): Double {
    return (lng + 180.0) / 360.0 * (1 shl zoom)
}

fun getTileY(lat: Double, zoom: Int): Double {
    val latRad = lat * kotlin.math.PI / 180.0
    return (1.0 - (kotlin.math.ln(kotlin.math.tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / kotlin.math.PI)) / 2.0 * (1 shl zoom)
}

fun tileToLng(x: Int, zoom: Int): Double {
    return x.toDouble() / (1 shl zoom) * 360.0 - 180.0
}

fun tileToLat(y: Int, zoom: Int): Double {
    val n = kotlin.math.PI - (2.0 * kotlin.math.PI * y.toDouble()) / (1 shl zoom)
    return kotlin.math.atan(kotlin.math.sinh(n)) * 180.0 / kotlin.math.PI
}

// Helper to interpolate smooth particle travel along road bends
fun getPointAlongPath(points: List<Offset>, progress: Float): Offset {
    if (points.isEmpty()) return Offset.Zero
    if (points.size == 1) return points[0]
    
    val segmentLengths = FloatArray(points.size - 1)
    var totalLength = 0f
    for (i in 0 until points.size - 1) {
        val dx = points[i+1].x - points[i].x
        val dy = points[i+1].y - points[i].y
        val len = sqrt(dx * dx + dy * dy)
        segmentLengths[i] = len
        totalLength += len
    }
    
    if (totalLength == 0f) return points[0]
    
    val targetDist = totalLength * progress.coerceIn(0f, 1f)
    var accumulatedDist = 0f
    
    for (i in 0 until points.size - 1) {
        val nextDist = accumulatedDist + segmentLengths[i]
        if (targetDist <= nextDist) {
            val segmentProgress = if (segmentLengths[i] > 0f) {
                (targetDist - accumulatedDist) / segmentLengths[i]
            } else {
                0f
            }
            val start = points[i]
            val end = points[i+1]
            return Offset(
                start.x + (end.x - start.x) * segmentProgress,
                start.y + (end.y - start.y) * segmentProgress
            )
        }
        accumulatedDist = nextDist
    }
    
    return points.last()
}

// Map Bounds Configuration
data class MapBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double
) {
    fun toOffset(lat: Double, lng: Double, width: Float, height: Float): Offset {
        val x = ((lng - minLng) / (maxLng - minLng)).toFloat() * width
        val y = (1f - ((lat - minLat) / (maxLat - minLat)).toFloat()) * height
        return Offset(x, y)
    }

    fun toLatLng(offset: Offset, width: Float, height: Float): Pair<Double, Double> {
        val pctX = offset.x / width
        val pctY = 1f - (offset.y / height)
        val lng = minLng + pctX * (maxLng - minLng)
        val lat = minLat + pctY * (maxLat - minLat)
        return Pair(lat, lng)
    }
}

val BUCURESTI_BOUNDS = MapBounds(44.4100, 44.4900, 26.0500, 26.1300)
val CLUJ_BOUNDS = MapBounds(46.7450, 46.7950, 23.5500, 23.6300)
val BRASOV_BOUNDS = MapBounds(45.6250, 45.6750, 25.5700, 25.6200)

@Composable
fun TileMapLayer(
    city: String,
    bounds: MapBounds,
    scaleProvider: () -> Float,
    offsetProvider: () -> Offset,
    mapColorSchemeStyle: String,
    isHighResolution: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    val s = scaleProvider()
                    val o = offsetProvider()
                    scaleX = s
                    scaleY = s
                    translationX = o.x
                    translationY = o.y
                }
        ) {
            val zoom = if (isHighResolution) {
                when (city) {
                    "București" -> if (mapColorSchemeStyle == "OsmAnd (HD maps + relief)") 16 else 15
                    else -> if (mapColorSchemeStyle == "OsmAnd (HD maps + relief)") 17 else 16
                }
            } else {
                when (city) {
                    "București" -> if (mapColorSchemeStyle == "OsmAnd (HD maps + relief)") 15 else 14
                    else -> if (mapColorSchemeStyle == "OsmAnd (HD maps + relief)") 16 else 15
                }
            }
            val minX = kotlin.math.floor(getTileX(bounds.minLng, zoom)).toInt()
            val maxX = kotlin.math.ceil(getTileX(bounds.maxLng, zoom)).toInt() - 1
            val minY = kotlin.math.floor(getTileY(bounds.maxLat, zoom)).toInt()
            val maxY = kotlin.math.ceil(getTileY(bounds.minLat, zoom)).toInt() - 1

            for (tileX in minX..maxX) {
                for (tileY in minY..maxY) {
                    val tileTopLeftLat = tileToLat(tileY, zoom)
                    val tileTopLeftLng = tileToLng(tileX, zoom)
                    val tileBottomRightLat = tileToLat(tileY + 1, zoom)
                    val tileBottomRightLng = tileToLng(tileX + 1, zoom)

                    val topLeftOffset = bounds.toOffset(tileTopLeftLat, tileTopLeftLng, widthPx, heightPx)
                    val bottomRightOffset = bounds.toOffset(tileBottomRightLat, tileBottomRightLng, widthPx, heightPx)

                    val tileWidthPx = bottomRightOffset.x - topLeftOffset.x
                    val tileHeightPx = bottomRightOffset.y - topLeftOffset.y

                    if (tileWidthPx > 0f && tileHeightPx > 0f) {
                        val tileUrl = when (mapColorSchemeStyle) {
                            "OsmAnd (HD maps + relief)" -> {
                                val servers = listOf("a", "b", "c")
                                val server = servers[kotlin.math.abs(tileX + tileY) % 3]
                                "https://$server.tile.opentopomap.org/$zoom/$tileX/$tileY.png"
                            }
                            "OSM Standard", "OpenStreetMap Standard", "Vintage Parchment", "Harta Veche" ->
                                "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"
                            "OSM Humanitarian", "Humanitarian" ->
                                "https://a.tile.openstreetmap.fr/hot/$zoom/$tileX/$tileY.png"
                            "OSM French", "French Style" ->
                                "https://a.tile.openstreetmap.fr/osmfr/$zoom/$tileX/$tileY.png"
                            "Ocean Breeze" ->
                                if (isHighResolution) "https://basemaps.cartocdn.com/rastertiles/voyager/$zoom/$tileX/$tileY@2x.png"
                                else "https://basemaps.cartocdn.com/rastertiles/voyager/$zoom/$tileX/$tileY.png"
                            "Muted Gray" ->
                                if (isHighResolution) "https://basemaps.cartocdn.com/light_all/$zoom/$tileX/$tileY@2x.png"
                                else "https://basemaps.cartocdn.com/light_all/$zoom/$tileX/$tileY.png"
                            else ->
                                if (isHighResolution) "https://basemaps.cartocdn.com/dark_all/$zoom/$tileX/$tileY@2x.png"
                                else "https://basemaps.cartocdn.com/dark_all/$zoom/$tileX/$tileY.png"
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(tileUrl)
                                .addHeader("User-Agent", "MetroOptimiseApp/1.0 (Android; Jetpack Compose; iordache.samuel@gmail.com)")
                                .crossfade(true)
                                .allowHardware(true)
                                .precision(coil.size.Precision.EXACT)
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            colorFilter = if (mapColorSchemeStyle == "Vintage Parchment" || mapColorSchemeStyle == "Harta Veche") {
                                ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                                    0.450f, 0.700f, 0.150f, 0f, 55f,  // Warm parchment red
                                    0.350f, 0.600f, 0.150f, 0f, 25f,  // Muted mossy green
                                    0.200f, 0.400f, 0.100f, 0f, -15f, // Suppressed blue for antique sepia gold
                                    0f,     0f,     0f,     1f, 0f
                                )))
                            } else null,
                            modifier = Modifier
                                .offset(
                                    x = with(LocalDensity.current) { topLeftOffset.x.toDp() },
                                    y = with(LocalDensity.current) { topLeftOffset.y.toDp() }
                                )
                                .size(
                                    width = with(LocalDensity.current) { tileWidthPx.toDp() },
                                    height = with(LocalDensity.current) { tileHeightPx.toDp() }
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun InteractiveMap(
    city: String,
    spots: List<TouristSpot>,
    customStartSpot: TouristSpot?,
    stations: List<BusStation>,
    lines: List<BusLine>,
    journey: OptimizedJourney?,
    onMapTap: (Double, Double) -> Unit,
    onSpotClick: (TouristSpot) -> Unit,
    modifier: Modifier = Modifier,
    onSetStartSpot: ((TouristSpot) -> Unit)? = null,
    onDeleteSpot: ((Long) -> Unit)? = null,
    isDashboardVisible: Boolean = true,
    onToggleDashboard: (() -> Unit)? = null,
    userGpsLocation: Pair<Double, Double>? = null,
    onEnableGpsClicked: (() -> Unit)? = null,
    onUseGpsAsStart: ((Double, Double) -> Unit)? = null,
    isSimulatingNavigation: Boolean = false,
    activeNavigationLegIndex: Int = 0,
    navigationProgressFraction: Float = 0f,
    onStepCompletedClick: (() -> Unit)? = null,
    onStopSimulationClick: (() -> Unit)? = null,
    isStationsVisible: Boolean = true,
    isTransitLinesVisible: Boolean = true,
    mapColorSchemeStyle: String = "Slate Neon",
    isHighResolutionMap: Boolean = true,
    isEnglish: Boolean = false,
    liveTransitVehicles: List<LiveTransitVehicle> = emptyList(),
    focusedTransitVehicle: LiveTransitVehicle? = null,
    onFocusedVehicleDismiss: (() -> Unit)? = null,
    onVehicleClick: ((LiveTransitVehicle) -> Unit)? = null
) {
    val cityBounds = TransitNetwork.getBoundsForCity(city)
    val bounds = MapBounds(
        minLat = cityBounds.minLat,
        maxLat = cityBounds.maxLat,
        minLng = cityBounds.minLng,
        maxLng = cityBounds.maxLng
    )

    val activeTrackingVehicle = remember(focusedTransitVehicle, liveTransitVehicles) {
        liveTransitVehicles.find { it.id == focusedTransitVehicle?.id } ?: focusedTransitVehicle
    }

    // Selected spot for detail container overlay
    var selectedSpotForDetails by remember(city) { mutableStateOf<TouristSpot?>(null) }

    // Selected bus station for detail container overlay showing live timetables/arrivals
    var selectedStationForDetails by remember(city) { mutableStateOf<BusStation?>(null) }

    // Collapsible map legend state
    var isLegendExpanded by remember { mutableStateOf(false) }

    // Dismissible help card state
    var showHelpCard by remember { mutableStateOf(true) }

    // Pan & Zoom state
    val scaleState = remember(city) { mutableStateOf(1.0f) }
    val offsetState = remember(city) { mutableStateOf(Offset.Zero) }
    var scale by scaleState
    var offset by offsetState

    // Coroutine scope and camera state for Google Map
    val scope = rememberCoroutineScope()
    val centerLat = (bounds.minLat + bounds.maxLat) / 2.0
    val centerLng = (bounds.minLng + bounds.maxLng) / 2.0
    val initialCameraPosition = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 13.5f)
    val cameraPositionState = rememberCameraPositionState {
        position = initialCameraPosition
    }
    LaunchedEffect(city) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 13.5f)
    }

    // Stable providers to bypass TileMapLayer recompositions entirely during pan/zoom transitions
    val scaleProvider = remember(scaleState) { { scaleState.value } }
    val offsetProvider = remember(offsetState) { { offsetState.value } }

    // Infinite transitions for dynamic high-fidelity fluid animations
    val infiniteTransition = rememberInfiniteTransition(label = "MapAnimations")

    // Smooth breathing pulsing for glowing pins/markers
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Flowing phase value for continuous path crawling and traveler particles
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animatedPhase"
    )

    // Category emoji helper for descriptive miniatures
    fun getSpotCategoryEmoji(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("observator") || lower.contains("astronomic") -> "🔭"
            lower.contains("therme") || lower.contains("wellness") -> "🌴"
            lower.contains("stadion") || lower.contains("arena națională") || lower.contains("cluj arena") || lower.contains("bt arena") -> "🏟️"
            lower.contains("librărie") || lower.contains("cărturești") -> "📖"
            lower.contains("teatru") || lower.contains("operă") || lower.contains("ateneu") || lower.contains("spectacol") || lower.contains("filarmonică") -> "🎭"
            lower.contains("cimitir") -> "🪦"
            lower.contains("pasaj") -> "🏮"
            lower.contains("parc") || lower.contains("grădină") || lower.contains("bulevard") || lower.contains("lac") || lower.contains("cismigiu") || lower.contains("herăstrău") -> "🌳"
            lower.contains("muzeu") || lower.contains("artă") || lower.contains("istorie") || lower.contains("antipa") || lower.contains("antichități") || lower.contains("cultur") || lower.contains("literatur") -> "🏛️"
            lower.contains("biserică") || lower.contains("catedrală") || lower.contains("mitropolie") || lower.contains("biserica") || lower.contains("monastir") || lower.contains("sinagogă") || lower.contains("templu") || lower.contains("patriarh") -> "⛪"
            lower.contains("castel") || lower.contains("palat") || lower.contains("cetatea") || lower.contains("cetate") || lower.contains("bastion") || lower.contains("turn") -> "🏰"
            lower.contains("mall") || lower.contains("unirea") || lower.contains("afi") || lower.contains("bazar") || lower.contains("piața") || lower.contains("comercial") || lower.contains("plazza") -> "🛍️"
            lower.contains("restaurant") || lower.contains("cafenea") || lower.contains("ceainărie") || lower.contains("pub") || lower.contains("bere") -> "🍽️"
            lower.contains("grădina zoologică") || lower.contains("zoo") || lower.contains("animale") -> "🦁"
            lower.contains("bibliotecă") || lower.contains("universitate") || lower.contains("coală") -> "📚"
            lower.contains("parcuri") || lower.contains("parcul") -> "🌳"
            else -> "📍"
        }
    }

    // Text Measurer for labels
    val textMeasurer = rememberTextMeasurer()

    // Store snapped street coordinates for transit lines: key = "lineName", value = List of Lat/Lng coordinates Pairs
    var transitLineRoads by remember(lines) { mutableStateOf<Map<String, List<Pair<Double, Double>>>>(emptyMap()) }
    // Store snapped street coordinates for journey legs: key = "fromLat,fromLng_toLat,toLng", value = List of Lat/Lng coordinates Pairs
    var journeyLegRoads by remember(journey) { mutableStateOf<Map<String, List<Pair<Double, Double>>>>(emptyMap()) }

    LaunchedEffect(lines) {
        val updatedRoads = mutableMapOf<String, List<Pair<Double, Double>>>()
        for (line in lines) {
            val allPoints = mutableListOf<Pair<Double, Double>>()
            for (j in 0 until line.stations.size - 1) {
                val st1 = line.stations[j]
                val st2 = line.stations[j + 1]
                val pathSegment = OSMRoutingService.getStreetSnappedRoute(
                    st1.latitude, st1.longitude,
                    st2.latitude, st2.longitude
                )
                allPoints.addAll(pathSegment)
            }
            if (allPoints.isNotEmpty()) {
                updatedRoads[line.name] = allPoints
            }
        }
        transitLineRoads = updatedRoads
    }

    LaunchedEffect(journey, customStartSpot, city) {
        if (journey != null) {
            val updatedJourneyRoads = mutableMapOf<String, List<Pair<Double, Double>>>()
            val startSpot = customStartSpot ?: TransitNetwork.getStartSpot(city)
            val fullJourneyPoints = listOf(startSpot) + journey.orderedSpots
            for (i in 0 until fullJourneyPoints.size - 1) {
                val p1 = fullJourneyPoints[i]
                val p2 = fullJourneyPoints[i + 1]
                val key = "${p1.latitude},${p1.longitude}_${p2.latitude},${p2.longitude}"
                val pathSegment = OSMRoutingService.getStreetSnappedRoute(
                    p1.latitude, p1.longitude,
                    p2.latitude, p2.longitude
                )
                updatedJourneyRoads[key] = pathSegment
            }
            journeyLegRoads = updatedJourneyRoads
        } else {
            journeyLegRoads = emptyMap()
        }
    }

    LaunchedEffect(userGpsLocation) {
        if (userGpsLocation != null && mapColorSchemeStyle == "Google Maps") {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(userGpsLocation.first, userGpsLocation.second),
                    14.5f
                )
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                when (mapColorSchemeStyle) {
                    "Cyberpunk" -> Color(0xFF0F0C1B)
                    "Muted Gray" -> Color(0xFF1E2022)
                    "Ocean Breeze" -> Color(0xFF0A192F)
                    "OsmAnd (HD maps + relief)" -> Color(0xFFF1F5F9)
                    else -> Color(0xFF0F172A)
                }
            )
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Keep track of the last vehicle we zoomed in on so we don't reset user's zoom
        var lastJumpedVehicleId by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(activeTrackingVehicle, widthPx, heightPx) {
            if (activeTrackingVehicle != null && widthPx > 0f && heightPx > 0f) {
                val v = activeTrackingVehicle
                val pt = bounds.toOffset(v.latitude, v.longitude, widthPx, heightPx)
                
                // Only force zoom level to 2.0f the first time we focus on this vehicle
                if (lastJumpedVehicleId != v.id) {
                    scale = 2.0f
                    lastJumpedVehicleId = v.id
                }
                
                // Keep it centered at the CURRENT scale the user has chosen
                offset = Offset(widthPx / 2f - pt.x * scale, heightPx / 2f - pt.y * scale)

                // Centering coordinate for GoogleMap
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(v.latitude, v.longitude), 15.5f),
                    1000
                )
            }
        }

        if (mapColorSchemeStyle == "Google Maps") {
            GoogleMapLayer(
                cameraPositionState = cameraPositionState,
                city = city,
                spots = spots,
                customStartSpot = customStartSpot,
                stations = stations,
                lines = lines,
                journey = journey,
                isStationsVisible = isStationsVisible,
                isTransitLinesVisible = isTransitLinesVisible,
                mapColorSchemeStyle = mapColorSchemeStyle,
                isSimulatingNavigation = isSimulatingNavigation,
                activeNavigationLegIndex = activeNavigationLegIndex,
                navigationProgressFraction = navigationProgressFraction,
                userGpsLocation = userGpsLocation,
                liveTransitVehicles = liveTransitVehicles,
                activeTrackingVehicle = activeTrackingVehicle,
                onStationClick = { selectedStationForDetails = it; selectedSpotForDetails = null },
                onSpotClick = { 
                    onSpotClick(it)
                    selectedSpotForDetails = it
                    selectedStationForDetails = null
                },
                onMapClick = { lat, lng -> onMapTap(lat, lng) },
                isEnglish = isEnglish,
                transitLineRoads = transitLineRoads,
                journeyLegRoads = journeyLegRoads,
                getSpotCategoryEmoji = { getSpotCategoryEmoji(it) }
            )
        } else {
            CustomCanvasMap(
                city = city,
                bounds = bounds,
                spots = spots,
                customStartSpot = customStartSpot,
                stations = stations,
                lines = lines,
                journey = journey,
                scaleProvider = scaleProvider,
                offsetProvider = offsetProvider,
                scale = scale,
                offset = offset,
                pulseScale = pulseScale,
                animatedPhase = animatedPhase,
                isStationsVisible = isStationsVisible,
                isTransitLinesVisible = isTransitLinesVisible,
                mapColorSchemeStyle = mapColorSchemeStyle,
                isHighResolutionMap = isHighResolutionMap,
                isSimulatingNavigation = isSimulatingNavigation,
                activeNavigationLegIndex = activeNavigationLegIndex,
                navigationProgressFraction = navigationProgressFraction,
                userGpsLocation = userGpsLocation,
                liveTransitVehicles = liveTransitVehicles,
                activeTrackingVehicle = activeTrackingVehicle,
                onScaleChange = { scale = it },
                onOffsetChange = { offset = it },
                onStationClick = { selectedStationForDetails = it; selectedSpotForDetails = null },
                onSpotClick = { 
                    onSpotClick(it)
                    selectedSpotForDetails = it
                    selectedStationForDetails = null
                },
                onMapClick = { lat, lng -> onMapTap(lat, lng) },
                isEnglish = isEnglish,
                transitLineRoads = transitLineRoads,
                journeyLegRoads = journeyLegRoads,
                getSpotCategoryEmoji = { getSpotCategoryEmoji(it) },
                textMeasurer = textMeasurer
            )
        }

        // Overlay Navigation / Zoom Controllers
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp), // Pushed slightly up to not cover details card
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    if (mapColorSchemeStyle == "Google Maps") {
                        scope.launch {
                            val currentZoom = cameraPositionState.position.zoom
                            cameraPositionState.animate(CameraUpdateFactory.zoomTo(currentZoom + 0.6f))
                        }
                    } else {
                        val oldScale = scale
                        val newScale = min(oldScale * 1.3f, 6.0f)
                        val centroid = Offset(widthPx / 2f, heightPx / 2f)
                        offset = centroid - (centroid - offset) / oldScale * newScale
                        scale = newScale
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(44.dp).testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = if (isEnglish) "Zoom In" else "Apropie Harta")
            }

            FloatingActionButton(
                onClick = {
                    if (mapColorSchemeStyle == "Google Maps") {
                        scope.launch {
                            val currentZoom = cameraPositionState.position.zoom
                            cameraPositionState.animate(CameraUpdateFactory.zoomTo(currentZoom - 0.6f))
                        }
                    } else {
                        val oldScale = scale
                        val newScale = max(oldScale / 1.3f, 0.8f)
                        val centroid = Offset(widthPx / 2f, heightPx / 2f)
                        offset = centroid - (centroid - offset) / oldScale * newScale
                        scale = newScale
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(44.dp).testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = if (isEnglish) "Zoom Out" else "Depărtează Harta")
            }

            FloatingActionButton(
                onClick = { 
                    if (mapColorSchemeStyle == "Google Maps") {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(centerLat, centerLng),
                                    13.5f
                                )
                            )
                        }
                    } else {
                        scale = 1.0f
                        offset = Offset.Zero
                    }
                    
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(44.dp).testTag("reset_zoom_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = if (isEnglish) "Reset Zoom / View" else "Restabilește vizualizarea", modifier = Modifier.size(18.dp))
            }

            // GPS Locator Button
            FloatingActionButton(
                onClick = {
                    if (onEnableGpsClicked != null) {
                        onEnableGpsClicked()
                    }
                },
                containerColor = if (userGpsLocation != null) Color(0xFF06B6D4) else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (userGpsLocation != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(44.dp).testTag("gps_locator_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = if (isEnglish) "Simulate Live GPS Location" else "Simulează Live Locație GPS",
                    modifier = Modifier.size(20.dp),
                    tint = if (userGpsLocation != null) Color.White else Color(0xFF06B6D4)
                )
            }

            if (onToggleDashboard != null) {
                FloatingActionButton(
                    onClick = onToggleDashboard,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp).testTag("floating_toggle_dashboard")
                ) {
                    Icon(
                        imageVector = if (isDashboardVisible) Icons.Default.KeyboardArrowDown else Icons.Default.Menu,
                        contentDescription = if (isDashboardVisible) {
                            if (isEnglish) "Hide dashboard" else "Ascunde panou"
                        } else {
                            if (isEnglish) "Show dashboard" else "Afișează panou"
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Collapsible Legend Panel Overlay at the top-left corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 60.dp, end = 12.dp) // Pushed slightly below top layout
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.animateContentSize()
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { isLegendExpanded = !isLegendExpanded }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isEnglish) "Legend" else "Legendă",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (isLegendExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isLegendExpanded) {
                        HorizontalDivider(color = Color(0xFF334155))
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LegendItem(
                                color = Color(0xFFEC4899),
                                text = if (isEnglish) "Starting Point (START)" else "Punct Pornire (START)"
                            )
                            LegendItem(
                                color = Color(0xFF6366F1),
                                text = if (isEnglish) "Selected Attraction" else "Atracție Selectată"
                            )
                            LegendItem(
                                color = Color(0xFF475569),
                                text = if (isEnglish) "Unselected Attraction" else "Atracție Neselectată"
                            )
                            LegendItem(
                                color = Color(0xFFEAB308),
                                text = if (isEnglish) "Custom Location" else "Atracție Personalizată"
                            )
                            LegendItem(
                                color = Color(0xFF0284C7),
                                text = if (isEnglish) "Transit Bus Line" else "Linie Autobuz Tranzit",
                                isLine = true
                            )
                            LegendItem(
                                color = Color(0xFFF97316),
                                text = if (isEnglish) "Pedestrian Segment" else "Segment Pietonal",
                                isDashed = true
                            )
                            LegendItem(
                                color = Color(0xFF94A3B8),
                                text = if (isEnglish) "Bus Station" else "Stație Autobuz",
                                isStation = true
                            )
                        }
                    }
                }
            }
        }

        // Helper label overlay at top of Map
        if (showHelpCard) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .fillMaxWidth(0.65f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isEnglish) "Tap to add custom point! Click pin for details." else "Atinge pentru punct personalizat! Click pe pin pentru detalii.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }
                    IconButton(
                        onClick = { showHelpCard = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isEnglish) "Close" else "Închide",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // GPS Offer overlay card
        if (userGpsLocation != null && customStartSpot?.name?.contains("Gps") != true && customStartSpot?.name?.contains("Google") != true) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 120.dp, end = 12.dp)
                    .fillMaxWidth(0.65f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, Color(0xFF0891B2)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "🛰️ GPS Symbol Connected" else "🛰️ Semnal GPS Conectat",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22D3EE)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEnglish) "Start your route from your exact GPS location on the map?" else "Vrei să pornești de la locația ta GPS exactă pe hartă?",
                        fontSize = 9.sp,
                        color = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (onUseGpsAsStart != null) {
                                onUseGpsAsStart(userGpsLocation.first, userGpsLocation.second)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(if (isEnglish) "Start from here" else "Pornește de aici", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Active Navigation HUD Panel Overlay
        AnimatedVisibility(
            visible = isSimulatingNavigation,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFF10B981)) // glowing emerald border for active navigation
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header Status
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isEnglish) "ACTIVE NAVIGATION" else "NAVIGAȚIE ACTIVĂ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = if (isEnglish) "Real-time navigation guide on map" else "Ghidaj în timp real pe hartă",
                                    fontSize = 9.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        
                        // Stop Navigation Button
                        OutlinedButton(
                            onClick = {
                                if (onStopSimulationClick != null) {
                                    onStopSimulationClick()
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEnglish) "Stop" else "Oprește", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    if (journey != null && activeNavigationLegIndex < journey.legs.size) {
                        val activeLeg = journey.legs[activeNavigationLegIndex]
                        val isTransitMode = activeLeg.type != LegType.WALK && activeLeg.type != LegType.TAXI
                        val legColor = if (isTransitMode) {
                            activeLeg.busColorHex?.let {
                                try {
                                    Color(android.graphics.Color.parseColor(it))
                                } catch (e: Exception) {
                                    Color(0xFF3B82F6)
                                }
                            } ?: Color(0xFF3B82F6)
                        } else if (activeLeg.type == LegType.TAXI) {
                            Color(0xFFEAB308)
                        } else {
                            Color(0xFF94A3B8)
                        }
                        
                        // Step indicator
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) {
                                    val modeLabel = when (activeLeg.type) {
                                        LegType.BUS -> "Bus Line ${activeLeg.busLineName ?: ""}"
                                        LegType.METRO -> "Metro Line ${activeLeg.busLineName ?: ""}"
                                        LegType.TROLLEY -> "Trolleybus ${activeLeg.busLineName ?: ""}"
                                        LegType.TRAIN -> "Regional Train ${activeLeg.busLineName ?: ""}"
                                        LegType.TAXI -> "Taxi/Uber Ridesharing"
                                        else -> "Walk on Foot"
                                    }
                                    "Stage ${activeNavigationLegIndex + 1} of ${journey.legs.size}: $modeLabel"
                                } else {
                                    val modeLabel = when (activeLeg.type) {
                                        LegType.BUS -> "Autobuz ${activeLeg.busLineName ?: ""}"
                                        LegType.METRO -> "Metrou ${activeLeg.busLineName ?: ""}"
                                        LegType.TROLLEY -> "Troleibuz ${activeLeg.busLineName ?: ""}"
                                        LegType.TRAIN -> "Tren Regional ${activeLeg.busLineName ?: ""}"
                                        LegType.TAXI -> "Ridesharing / Taxi"
                                        else -> "Mers pe jos"
                                    }
                                    "Etapa ${activeNavigationLegIndex + 1} din ${journey.legs.size}: $modeLabel"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${activeLeg.durationMinutes} min",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = legColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Active Directions text
                        Card(
                            colors = CardDefaults.cardColors(containerColor = legColor.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, legColor.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = translateDirections(activeLeg.directions, isEnglish),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 14.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Progress Slider / Simulation Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Gps:",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { navigationProgressFraction },
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF334155),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Text(
                                text = "${(navigationProgressFraction * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Action Completer button for navigation
                        Button(
                            onClick = {
                                if (onStepCompletedClick != null) {
                                    onStepCompletedClick()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Text(
                                    text = if (activeNavigationLegIndex == journey.legs.size - 1) {
                                        if (isEnglish) "Finish Route 🏁" else "Finalizează Traseul 🏁"
                                    } else {
                                        if (isEnglish) "Next Stage ➔" else "Următoarea Etapă ➔"
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    } else {
                        // Journey finished screen inside HUD!
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isEnglish) "🎉 Route Successfully Completed!" else "🎉 Traseu Finalizat cu Succes!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = if (isEnglish) "You have visited all of your selected attractions using municipal transit!" else "Ai vizitat toate atracțiile selectate cu succes folosind transportul public!",
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    if (onStopSimulationClick != null) {
                                        onStopSimulationClick()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(if (isEnglish) "Close Guidance" else "Închide Ghidajul", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 1b. Focused Transit Vehicle HUD Panel Overlay
        AnimatedVisibility(
            visible = activeTrackingVehicle != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = if (isSimulatingNavigation) 150.dp else 12.dp)
                .widthIn(max = 500.dp)
        ) {
            activeTrackingVehicle?.let { vehicle ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFF43F5E)) // Bright Rose neon border for targeted tracking
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF43F5E).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh, // radar/tracking style
                                    contentDescription = null,
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = vehicle.lineName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFF43F5E).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "LIVE RADAR",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF43F5E)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isEnglish) {
                                        "Model: ${vehicle.vehicleModel} • Speed: ${vehicle.speedKmh} km/h"
                                    } else {
                                        "Model: ${vehicle.vehicleModel} • Viteză: ${vehicle.speedKmh} km/h"
                                    },
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = if (isEnglish) {
                                        "To: ${vehicle.direction} (Delay: +${vehicle.delayMinutes}m)"
                                    } else {
                                        "Spre: ${vehicle.direction} (Întârziere: +${vehicle.delayMinutes}m)"
                                    },
                                    fontSize = 10.sp,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = { onFocusedVehicleDismiss?.invoke() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isEnglish) "Stop tracking" else "Oprește urmărirea",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Spot Details Card Overlay inside map container
        val activeInspectedSpot = spots.find { it.id == selectedSpotForDetails?.id } ?: selectedSpotForDetails
        AnimatedVisibility(
            visible = activeInspectedSpot != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            activeInspectedSpot?.let { spot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Header Row with Title & Close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = when {
                                                spot.isCustom -> Color(0xFFEAB308).copy(alpha = 0.2f)
                                                spot.isSelected -> Color(0xFF6366F1).copy(alpha = 0.2f)
                                                else -> Color(0xFF475569).copy(alpha = 0.2f)
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (spot.isCustom) Icons.Default.Star else Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = when {
                                            spot.isCustom -> Color(0xFFEAB308)
                                            spot.isSelected -> Color(0xFF6366F1)
                                            else -> Color(0xFF94A3B8)
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = spot.translate(isEnglish).name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val badgeText = if (spot.isCustom) {
                                            if (isEnglish) "Custom" else "Personalizat"
                                        } else {
                                            if (isEnglish) "Recommended" else "Recomandat"
                                        }
                                        val badgeColor = if (spot.isCustom) Color(0xFFEAB308) else Color(0xFF6366F1)
                                        Box(
                                            modifier = Modifier
                                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                fontSize = 8.sp,
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = if (isEnglish) "${spot.visitDurationMinutes} min" else "${spot.visitDurationMinutes} min",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { selectedSpotForDetails = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isEnglish) "Close details" else "Închide detalii",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 🌟 CUSTOM PERSONALIZED MINIATURE LAYOUT FOR THE ATTRACTION
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // The Personalized Visual Miniature Thumbnail
                            val emoji = getSpotCategoryEmoji(spot.name)
                            val gradientColors = when (emoji) {
                                "🌳" -> listOf(Color(0xFF065F46), Color(0xFF047857), Color(0xFF10B981)) // Forest/Teal Gradients
                                "🏛️" -> listOf(Color(0xFF1E1E2C), Color(0xFF312E81), Color(0xFF4F46E5)) // Deep Indigo/Museum Gradients
                                "⛪" -> listOf(Color(0xFF130F40), Color(0xFF2C3A47), Color(0xFF1B1464)) // Sacred Cosmic Dark Blue
                                "🏰" -> listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFFBBF24)) // Golden Royal Amber
                                "🎭" -> listOf(Color(0xFF701A75), Color(0xFF9D174D), Color(0xFFF43F5E)) // Creative Rose Magenta
                                "🔭" -> listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF581C87)) // Deep Observatory Purple
                                "🏟️" -> listOf(Color(0xFF111827), Color(0xFF1E3A8A), Color(0xFF3B82F6)) // Sport Blue
                                "🌴" -> listOf(Color(0xFF065F46), Color(0xFF0D9488), Color(0xFF2DD4BF)) // Spa/Aquatic Turquoise
                                "🪦" -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF64748B)) // Muted Memorial Slate Gray
                                "📖" -> listOf(Color(0xFF3B1F10), Color(0xFF6B4226), Color(0xFFB07D62)) // Leather Book Amber
                                "🏮" -> listOf(Color(0xFF5B1616), Color(0xFF881337), Color(0xFFE11D48)) // Vintage Lantern Rose
                                else -> listOf(Color(0xFF1E293B), Color(0xFF475569), Color(0xFF94A3B8)) // Modern Silver Gray
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(gradientColors),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Background subtle aesthetic ring
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                )
                                // Render centerpiece emoji larger & with pulse animation
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp
                                )
                            }
                            
                            // Info & Categorized Descriptions Column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val categoryTag = when (emoji) {
                                    "🌳" -> if (isEnglish) "Nature & Promenades" else "Natură, Parcuri & Promenadă"
                                    "🏛️" -> if (isEnglish) "Culture & Historical Heritage" else "Cultură și Patrimoniu"
                                    "⛪" -> if (isEnglish) "Sacred & Places of Worship" else "Lăcaș de Cult & Spiritualitate"
                                    "🏰" -> if (isEnglish) "Palace or Monumental Residence" else "Palat & Reședință Monumentală"
                                    "🎭" -> if (isEnglish) "Arts, Theater & Music Recital" else "Artă, Spectacole & Muzică"
                                    "🔭" -> if (isEnglish) "Science & Stellar Astronomy" else "Știință & Observații Astronomice"
                                    "🏟️" -> if (isEnglish) "Stadium & High Performance Sports" else "Stadion & Sport de Performanță"
                                    "🌴" -> if (isEnglish) "Geothermal Wellness & Spa" else "Wellness & Relaxare Termală"
                                    "🪦" -> if (isEnglish) "Historic Pantheon & Art Monuments" else "Panteon Istoric & Monumente de Artă"
                                    "🏮" -> if (isEnglish) "Vintage Passages & Coffee Shop" else "Pasaj Vintage & Cafenele Boeme"
                                    "📖" -> if (isEnglish) "Spectacular Literary Bookstore" else "Librărie & Bibliotecă Spectaculoasă"
                                    else -> if (isEnglish) "Tourist Interest Point" else "Punct de Interes Turistic"
                                }
                                
                                Text(
                                    text = categoryTag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        emoji == "🌳" -> Color(0xFF34D399)
                                        emoji == "🏛️" -> Color(0xFF818CF8)
                                        emoji == "⛪" -> Color(0xFF93C5FD)
                                        emoji == "🏰" -> Color(0xFFFBBF24)
                                        emoji == "🎭" -> Color(0xFFFB7185)
                                        emoji == "🌴" -> Color(0xFF2DD4BF)
                                        emoji == "🪦" -> Color(0xFF94A3B8)
                                        emoji == "✨" -> Color(0xFFF472B6)
                                        else -> Color(0xFFF472B6)
                                    }
                                )
                                
                                val displayDesc = spot.translate(isEnglish).description
                                Text(
                                    text = if (displayDesc.isNotEmpty()) displayDesc else (if (isEnglish) "No description added for this spot." else "Nicio descriere adăugată pentru acest obiectiv."),
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1),
                                    lineHeight = 15.sp,
                                    maxLines = 3
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toggle Select Button
                            Button(
                                onClick = { 
                                    onSpotClick(spot)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (spot.isSelected) Color(0xFFEF4444) else Color(0xFF10B981)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (spot.isSelected) Icons.Default.Close else Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (spot.isSelected) (if (isEnglish) "Exclude" else "Exclude") else (if (isEnglish) "Include" else "Include"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Set as Start Point Button
                            if (onSetStartSpot != null) {
                                OutlinedButton(
                                    onClick = { 
                                        onSetStartSpot(spot)
                                    },
                                    border = BorderStroke(1.dp, Color(0xFFEC4899)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEC4899)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isEnglish) "Set Start" else "Setează Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Delete custom spot option
                            if (spot.isCustom && onDeleteSpot != null) {
                                IconButton(
                                    onClick = {
                                        onDeleteSpot(spot.id)
                                        selectedSpotForDetails = null
                                    },
                                    modifier = Modifier
                                        .background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape)
                                        .size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = if (isEnglish) "Delete location" else "Șterge punct",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Bus Station Arrivals Card Overlay inside map container
            AnimatedVisibility(
                visible = selectedStationForDetails != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            ) {
                selectedStationForDetails?.let { station ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("station_details_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.96f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Title row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = station.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color(0xFF10B981), CircleShape)
                                            )
                                            Text(
                                                text = if (isEnglish) "Live Station arrivals monitoring" else "Monitorizare Sosiri Live Stație",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { selectedStationForDetails = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isEnglish) "Close station board" else "Închide panou stație",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Real-Time Arrival Board Box Layout
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF475569).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                // Find lines serving this station
                                val servingLines = lines.filter { line ->
                                    line.stations.any { it.name == station.name }
                                }

                                if (servingLines.isEmpty()) {
                                    Text(
                                        text = if (isEnglish) "No registered transit routes at this terminal." else "Nicio cursă înregistrată la acest terminal.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isEnglish) "📺 INCOMING LINES & ETA" else "📺 CURSE ÎN SOSIRE & MINUTE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            letterSpacing = 0.5.sp
                                        )

                                        servingLines.forEach { line ->
                                            // Fetch actual deterministic simulated arrival minutes
                                            val arrivals = TransportApiEngine.queryStationArrivals(station.name, line.name)
                                            val opInfo = TransportApiEngine.getOperatorUiInfo(city)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Line Color Badge
                                                    val lineCol = try {
                                                        Color(android.graphics.Color.parseColor(line.colorHex))
                                                    } catch (e: Exception) {
                                                        Color(0xFF3B82F6)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .background(lineCol.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                            .border(1.dp, lineCol, RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = line.name,
                                                            color = lineCol,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    // Operator tag
                                                    Text(
                                                        text = opInfo.shortName,
                                                        color = Color(0xFF64748B),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // ETA minutes bubbles
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    arrivals.take(2).forEachIndexed { index, min ->
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    color = if (index == 0) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF475569).copy(alpha = 0.3f),
                                                                    shape = RoundedCornerShape(6.dp)
                                                                )
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (min == 0) (if (isEnglish) "Due" else "Sosire") else "$min min",
                                                                color = if (index == 0) Color(0xFF10B981) else Color(0xFF94A3B8),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    // Crowding indication icon
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFF59E0B).copy(alpha = 0.1f), CircleShape)
                                                            .padding(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = Color(0xFFF59E0B),
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick action ticketing trigger button
                            val context = LocalContext.current
                            val opInfo = TransportApiEngine.getOperatorUiInfo(city)

                            Button(
                                onClick = {
                                    val smsConfig = TransportApiEngine.getSmsTicketDetails(city)
                                    TransportApiEngine.triggerSmsCompose(context, smsConfig)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) {
                                        "Purchase ${opInfo.shortName} Ticket via SMS"
                                    } else {
                                        "Cumpără Bilet ${opInfo.shortName} prin SMS"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    text: String,
    isLine: Boolean = false,
    isDashed: Boolean = false,
    isStation: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        if (isLine) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(4.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        } else if (isDashed) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(18.dp)) {
                Box(modifier = Modifier.width(4.dp).height(3.dp).background(color))
                Box(modifier = Modifier.width(4.dp).height(3.dp).background(color))
                Box(modifier = Modifier.width(4.dp).height(3.dp).background(color))
            }
        } else if (isStation) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF334155), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Color(0xFF94A3B8), CircleShape)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        }
        Text(
            text = text,
            color = Color(0xFFE2E8F0),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
