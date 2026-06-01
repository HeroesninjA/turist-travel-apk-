package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.ui.screens.translateDirections
import com.example.ui.screens.translateSpotName
import com.example.data.translate
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
                    "București" -> 15
                    else -> 16
                }
            } else {
                when (city) {
                    "București" -> 14
                    else -> 15
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
                            "OSM Standard", "OpenStreetMap Standard" ->
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
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
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
    isEnglish: Boolean = false
) {
    val bounds = when (city) {
        "București" -> BUCURESTI_BOUNDS
        "Brașov" -> BRASOV_BOUNDS
        else -> CLUJ_BOUNDS
    }

    // Selected spot for detail container overlay
    var selectedSpotForDetails by remember(city) { mutableStateOf<TouristSpot?>(null) }

    // Collapsible map legend state
    var isLegendExpanded by remember { mutableStateOf(false) }

    // Dismissible help card state
    var showHelpCard by remember { mutableStateOf(true) }

    // Pan & Zoom state
    var scale by remember(city) { mutableStateOf(1.0f) }
    var offset by remember(city) { mutableStateOf(Offset.Zero) }

    // Stable providers to bypass TileMapLayer recompositions entirely during pan/zoom transitions
    val currentScale = rememberUpdatedState(scale)
    val currentOffset = rememberUpdatedState(offset)
    val scaleProvider = remember { { currentScale.value } }
    val offsetProvider = remember { { currentOffset.value } }

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
            lower.contains("parc") || lower.contains("grădină") || lower.contains("bulevard") || lower.contains("lac") || lower.contains("cismigiu") || lower.contains("herăstrău") -> "🌳"
            lower.contains("muzeu") || lower.contains("artă") || lower.contains("istorie") || lower.contains("antipa") || lower.contains("antichități") || lower.contains("cultur") -> "🏛️"
            lower.contains("biserică") || lower.contains("catedrală") || lower.contains("mitropolie") || lower.contains("biserica") || lower.contains("monastir") || lower.contains("sinagogă") || lower.contains("templu") -> "⛪"
            lower.contains("castel") || lower.contains("palat") || lower.contains("cetatea") || lower.contains("cetate") || lower.contains("bastion") || lower.contains("turn") -> "🏰"
            lower.contains("teatru") || lower.contains("operă") || lower.contains("ateneu") || lower.contains("spectacol") || lower.contains("filarmonică") -> "🎭"
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                when (mapColorSchemeStyle) {
                    "Cyberpunk" -> Color(0xFF0F0C1B)
                    "Muted Gray" -> Color(0xFF1E2022)
                    "Ocean Breeze" -> Color(0xFF0A192F)
                    else -> Color(0xFF0F172A)
                }
            )
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. Dynamic Map Tiles Layer (Osm/CartoDB Dark Matter) - highly stable!
        TileMapLayer(
            city = city,
            bounds = bounds,
            scaleProvider = scaleProvider,
            offsetProvider = offsetProvider,
            mapColorSchemeStyle = mapColorSchemeStyle,
            isHighResolution = isHighResolutionMap
        )

        // Grid background drawing + bus layers + spots pins
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_map_canvas")
                .pointerInput(city) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = scale * zoom
                        if (!nextScale.isNaN() && !nextScale.isInfinite()) {
                            scale = max(0.8f, min(nextScale, 6.0f))
                        }
                        if (!pan.x.isNaN() && !pan.y.isNaN() && !pan.x.isInfinite() && !pan.y.isInfinite()) {
                            offset += pan
                        }
                    }
                }
                .pointerInput(city) {
                    detectTapGestures(
                        onTap = { localOffset ->
                            val width = size.width.toFloat()
                            val height = size.height.toFloat()

                            if (width > 0f && height > 0f && scale > 0f && !scale.isNaN()) {
                                // Convert local click to coordinate offset considering Zoom and Pan
                                // localOffset = (mapOffset * scale) + offset
                                // Therefore: mapOffset = (localOffset - offset) / scale
                                val mapCoords = (localOffset - offset) / scale
                                
                                // Check bounds
                                if (mapCoords.x in 0f..width && mapCoords.y in 0f..height) {
                                    // 1. Check if user tapped near an existing tourist spot marker
                                    val clickedSpot = spots.find { spot ->
                                        val pt = bounds.toOffset(spot.latitude, spot.longitude, width, height)
                                        val dx = mapCoords.x - pt.x
                                        val dy = mapCoords.y - pt.y
                                        (dx * dx + dy * dy) <= (28f * 28f) // 28-pixel touch target tolerance
                                    }

                                    if (clickedSpot != null) {
                                        onSpotClick(clickedSpot)
                                        selectedSpotForDetails = clickedSpot
                                    } else {
                                        val (lat, lng) = bounds.toLatLng(mapCoords, width, height)
                                        onMapTap(lat, lng)
                                    }
                                }
                            }
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw Tech grid lines
            val gridSpacing = 80f
            val gridPaint = when (mapColorSchemeStyle) {
                "Cyberpunk" -> Color(0xFF2B1C40)
                "Muted Gray" -> Color(0xFF2B2D31)
                "Ocean Breeze" -> Color(0xFF172D4D)
                else -> Color(0xFF1E293B)
            }
            for (x in 0.. (width / gridSpacing).toInt()) {
                drawLine(
                    color = gridPaint,
                    start = Offset(x * gridSpacing, 0f),
                    end = Offset(x * gridSpacing, height),
                    strokeWidth = 1f
                )
            }
            for (y in 0.. (height / gridSpacing).toInt()) {
                drawLine(
                    color = gridPaint,
                    start = Offset(0f, y * gridSpacing),
                    end = Offset(width, y * gridSpacing),
                    strokeWidth = 1f
                )
            }

            // 2. Draw Bus Lines Paths
            if (isTransitLinesVisible) {
                val activeBusLines = journey?.legs?.mapNotNull { it.busLineName }?.toSet() ?: emptySet()
                for (line in lines) {
                    if (journey != null && line.name !in activeBusLines) {
                        continue // Non-essential bus lines disappear when a route is active to focus on the selected path!
                    }
                    val path = Path()
                    var configured = false
                    val lineCol = try {
                        Color(android.graphics.Color.parseColor(line.colorHex))
                    } catch (e: Exception) {
                        Color(0xFF3B82F6) // fallback default Blue
                    }
                    
                    val pointsToDraw = transitLineRoads[line.name] ?: line.stations.map { Pair(it.latitude, it.longitude) }
                    
                    for (ptLatLng in pointsToDraw) {
                        val pt = bounds.toOffset(ptLatLng.first, ptLatLng.second, width, height)
                        if (!configured) {
                            path.moveTo(pt.x, pt.y)
                            configured = true
                        } else {
                            path.lineTo(pt.x, pt.y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = lineCol.copy(alpha = 0.85f),
                        style = Stroke(width = 6f / scale, pathEffect = PathEffect.cornerPathEffect(15f))
                    )
                }
            }

            // 3. Draw Optimized Journey legs (Connections between tourist spots with fluid motion)
            if (journey != null) {
                val fullJourneyPoints = listOfNotNull(customStartSpot ?: TransitNetwork.getStartSpot(city)) + journey.orderedSpots
                
                for (i in 0 until fullJourneyPoints.size - 1) {
                    val p1 = fullJourneyPoints[i]
                    val p2 = fullJourneyPoints[i + 1]
                    val fromPt = bounds.toOffset(p1.latitude, p1.longitude, width, height)
                    val toPt = bounds.toOffset(p2.latitude, p2.longitude, width, height)

                    val key = "${p1.latitude},${p1.longitude}_${p2.latitude},${p2.longitude}"
                    val rawRoutePoints = journeyLegRoads[key] ?: listOf(Pair(p1.latitude, p1.longitude), Pair(p2.latitude, p2.longitude))
                    val offsetPoints = rawRoutePoints.map { bounds.toOffset(it.first, it.second, width, height) }

                    // Create Compose Path
                    val legPath = Path()
                    if (offsetPoints.isNotEmpty()) {
                        legPath.moveTo(offsetPoints[0].x, offsetPoints[0].y)
                        for (k in 1 until offsetPoints.size) {
                            legPath.lineTo(offsetPoints[k].x, offsetPoints[k].y)
                        }
                    }

                    // Find matching leg type to color the visual itinerary connection!
                    val leg = journey.legs.getOrNull(i)
                    val pathColor = if (leg?.type == LegType.BUS) {
                        leg.busColorHex?.let {
                            try {
                                Color(android.graphics.Color.parseColor(it))
                            } catch (e: Exception) {
                                Color(0xFF10B981)
                            }
                        } ?: Color(0xFF10B981)
                    } else {
                        Color(0xFFE2E8F0) // White/Silver for Walking
                    }

                    // Draw connecting path line (distinctive, thick, pulsing look with animated dashes)
                    if (leg?.type == LegType.WALK) {
                        // Walking: animated dashed marching line indicating motion
                        drawPath(
                            path = legPath,
                            color = pathColor.copy(alpha = 0.85f),
                            style = Stroke(
                                width = 6f / scale,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(15f / scale, 10f / scale),
                                    phase = animatedPhase * (25f / scale)
                                )
                            )
                        )
                    } else {
                        // Bus: solid primary color line + flowing indicator line on top
                        drawPath(
                            path = legPath,
                            color = pathColor,
                            style = Stroke(
                                width = 9f / scale,
                                pathEffect = PathEffect.cornerPathEffect(10f)
                            )
                        )
                        drawPath(
                            path = legPath,
                            color = Color.White.copy(alpha = 0.4f),
                            style = Stroke(
                                width = 3f / scale,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(25f / scale, 45f / scale),
                                    phase = -animatedPhase * (30f / scale)
                                )
                            )
                        )
                    }

                    // Crawling Traveler Particle: a glowing circle flowing along each leg
                    // Animates from start to end based on active phase
                    val progress = (animatedPhase / 100f)
                    val particlePos = getPointAlongPath(offsetPoints, progress)

                    // Pulse aura
                    drawCircle(
                        color = pathColor.copy(alpha = 0.45f),
                        radius = 13f / scale,
                        center = particlePos
                    )
                    // Bright core
                    drawCircle(
                        color = Color.White,
                        radius = 5.5f / scale,
                        center = particlePos
                    )

                    // Circle marking step order direction
                    drawCircle(
                        color = pathColor,
                        radius = 16f / scale,
                        center = fromPt
                    )

                    // Order Number annotation text
                    val stepNum = (i + 1).toString()
                    val textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = (10 / scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    val textLayout = textMeasurer.measure(AnnotatedString(stepNum), style = textStyle)
                    val textOffset = Offset(fromPt.x - textLayout.size.width / 2, fromPt.y - textLayout.size.height / 2)
                    drawText(textLayout, Color(0xFF0F172A), textOffset)
                }
            }

            // 4. Draw Bus Stations
            if (isStationsVisible) {
                val outerRingCol = when(mapColorSchemeStyle) {
                    "Cyberpunk" -> Color(0xFF9D00FF)
                    "Muted Gray" -> Color(0xFF666666)
                    "Ocean Breeze" -> Color(0xFF172D4D)
                    else -> Color(0xFF334155)
                }
                val innerRingCol = when(mapColorSchemeStyle) {
                    "Cyberpunk" -> Color(0xFF00FFFF)
                    "Muted Gray" -> Color(0xFF999999)
                    "Ocean Breeze" -> Color(0xFF64FFDA)
                    else -> Color(0xFF94A3B8)
                }
                val stationBackdropCol = when(mapColorSchemeStyle) {
                    "Cyberpunk" -> Color(0xFF0F0C1B)
                    "Muted Gray" -> Color(0xFF1E2022)
                    "Ocean Breeze" -> Color(0xFF0A192F)
                    else -> Color(0xFF0F172A)
                }

                val activeStations = journey?.legs?.flatMap { listOfNotNull(it.boardingStation, it.alightingStation) }?.toSet() ?: emptySet()
                for (station in stations) {
                    if (journey != null && station.name !in activeStations) {
                        continue // Non-essential bus stations disappear when a route is active to focus on the selected path!
                    }
                    val pt = bounds.toOffset(station.latitude, station.longitude, width, height)
                    
                    // Outer ring
                    drawCircle(
                        color = outerRingCol,
                        radius = 12f / scale,
                        center = pt
                    )
                    
                    // Inner ring
                    drawCircle(
                        color = innerRingCol,
                        radius = 6f / scale,
                        center = pt
                    )

                    // Station Label with background rectangle to ensure crisp legibility
                    if (scale >= 1.5f) {
                        val labelText = station.name
                        val labelStyle = TextStyle(
                            color = innerRingCol,
                            fontSize = (8 / scale).sp,
                            fontWeight = FontWeight.Normal
                        )
                        val labelLayout = textMeasurer.measure(AnnotatedString(labelText), style = labelStyle)
                        val textPos = Offset(pt.x + (15 / scale), pt.y - (labelLayout.size.height / 2))
                        
                        // Draw mini label backdrop
                        drawRoundRect(
                            color = stationBackdropCol.copy(alpha = 0.75f),
                            topLeft = textPos + Offset(-4f, -2f),
                            size = Size(labelLayout.size.width.toFloat() + 8f, labelLayout.size.height.toFloat() + 4f),
                            cornerRadius = CornerRadius(4f)
                        )
                        drawText(labelLayout, innerRingCol, textPos)
                    }
                }
            }

            // 5. Draw Tourist Spot Pins as Gorgeous Descriptive Miniature Cards/Thumbnails
            for (spot in spots) {
                if (journey != null && !spot.isSelected) {
                    continue // Non-essential tourist spots disappear when a route is active to focus on the selected path!
                }
                val pt = bounds.toOffset(spot.latitude, spot.longitude, width, height)
                val pinColor = when {
                    !spot.isSelected -> Color(0xFF475569) // Muted gray for inactive
                    spot.isCustom -> Color(0xFFEAB308) // Yellow for custom spots
                    else -> Color(0xFF6366F1) // Indigo for active preset spots
                }

                // Smooth breathing pulsing aura glow for active markers
                if (spot.isSelected) {
                    val pulseCol = if (spot.isCustom) Color(0xFFEAB308) else Color(0xFF6366F1)
                    drawCircle(
                        color = pulseCol.copy(alpha = 0.2f * (1.5f - pulseScale)),
                        radius = (26f * pulseScale) / scale,
                        center = pt
                    )
                }

                // Visual base anchor pointer connecting thumbnail directly to the geographic point
                val r = 14f / scale
                val h = 22f / scale
                val pointerPath = Path().apply {
                    moveTo(pt.x, pt.y)
                    lineTo(pt.x - r * 0.9f, pt.y - h)
                    lineTo(pt.x + r * 0.9f, pt.y - h)
                    close()
                }
                drawPath(path = pointerPath, color = pinColor)

                // Thumbnail circle framed border
                drawCircle(
                    color = pinColor,
                    radius = r,
                    center = Offset(pt.x, pt.y - h)
                )
                // Dark background core inside the miniature
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = r * 0.85f,
                    center = Offset(pt.x, pt.y - h)
                )

                // Render descriptive categorized emoji as high-quality thumbnail image centered inside
                val categoryEmoji = getSpotCategoryEmoji(spot.name)
                val emojiStyle = TextStyle(
                    fontSize = (11 / scale).sp
                )
                val emojiLayout = textMeasurer.measure(AnnotatedString(categoryEmoji), style = emojiStyle)
                val emojiOffset = Offset(
                    pt.x - emojiLayout.size.width / 2f,
                    pt.y - h - emojiLayout.size.height / 2f
                )
                drawText(emojiLayout, Color.Unspecified, emojiOffset)

                // Rich Descriptive Floating Card Label specifying name, category, and visit time
                val labelStyle = TextStyle(
                    color = if (spot.isSelected) Color.White else Color(0xFFCBD5E1),
                    fontSize = (9 / scale).sp,
                    fontWeight = if (spot.isSelected) FontWeight.Bold else FontWeight.Medium
                )
                val labelLayout = textMeasurer.measure(AnnotatedString(spot.name), style = labelStyle)
                val labelPos = Offset(pt.x - (labelLayout.size.width / 2f), pt.y - h - r - (14f / scale))

                // Render multi-layered glass rounded box under text
                drawRoundRect(
                    color = pinColor.copy(alpha = 0.4f),
                    topLeft = labelPos + Offset(-6f, -3f),
                    size = Size(labelLayout.size.width.toFloat() + 12f, labelLayout.size.height.toFloat() + 6f),
                    cornerRadius = CornerRadius(6f),
                    style = Stroke(width = 1f / scale)
                )
                drawRoundRect(
                    color = Color(0xFF1E293B).copy(alpha = 0.88f),
                    topLeft = labelPos + Offset(-6f, -3f),
                    size = Size(labelLayout.size.width.toFloat() + 12f, labelLayout.size.height.toFloat() + 6f),
                    cornerRadius = CornerRadius(6f)
                )
                drawText(labelLayout, if (spot.isSelected) Color.White else Color(0xFF94A3B8), labelPos)
            }

            // 6. Draw Custom Start Spot Pin as a highly attractive pink launcher capsule with a rocket emoji!
            val startSpot = customStartSpot ?: TransitNetwork.getStartSpot(city)
            val startPt = bounds.toOffset(startSpot.latitude, startSpot.longitude, width, height)
            
            val startPinColor = Color(0xFFEC4899)
            val rStart = 16f / scale
            val hStart = 24f / scale

            // Pulsing start glowing aura circle
            drawCircle(
                color = startPinColor.copy(alpha = 0.25f * (1.5f - pulseScale)),
                radius = (30f * pulseScale) / scale,
                center = startPt
            )
            
            val startPath = Path().apply {
                moveTo(startPt.x, startPt.y)
                lineTo(startPt.x - rStart * 0.9f, startPt.y - hStart)
                lineTo(startPt.x + rStart * 0.9f, startPt.y - hStart)
                close()
            }
            drawPath(path = startPath, color = startPinColor)
            
            drawCircle(
                color = startPinColor,
                radius = rStart,
                center = Offset(startPt.x, startPt.y - hStart)
            )
            drawCircle(
                color = Color(0xFF0F172A),
                radius = rStart * 0.85f,
                center = Offset(startPt.x, startPt.y - hStart)
            )

            // Starting launcher rocket emoji centered inside Custom Start spot
            val startEmojiLayout = textMeasurer.measure(AnnotatedString("🚀"), style = TextStyle(fontSize = (12 / scale).sp))
            val startEmojiOffset = Offset(
                startPt.x - startEmojiLayout.size.width / 2f,
                startPt.y - hStart - startEmojiLayout.size.height / 2f
            )
            drawText(startEmojiLayout, Color.Unspecified, startEmojiOffset)

            val startLabelStyle = TextStyle(
                color = Color(0xFFF472B6),
                fontSize = (10 / scale).sp,
                fontWeight = FontWeight.ExtraBold
            )
            val displayName = if (isEnglish) "START: ${translateSpotName(startSpot.name, true)}" else "START: ${startSpot.name}"
            val startLabelLayout = textMeasurer.measure(AnnotatedString(displayName), style = startLabelStyle)
            val startLabelPos = Offset(startPt.x - (startLabelLayout.size.width / 2f), startPt.y + (12f / scale))
            
            drawRoundRect(
                color = Color(0xFFEC4899).copy(alpha = 0.4f),
                topLeft = startLabelPos + Offset(-6f, -3f),
                size = Size(startLabelLayout.size.width.toFloat() + 12f, startLabelLayout.size.height.toFloat() + 6f),
                cornerRadius = CornerRadius(6f),
                style = Stroke(width = 1f / scale)
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.92f),
                topLeft = startLabelPos + Offset(-6f, -3f),
                size = Size(startLabelLayout.size.width.toFloat() + 12f, startLabelLayout.size.height.toFloat() + 6f),
                cornerRadius = CornerRadius(6f)
            )
            drawText(startLabelLayout, Color(0xFFF472B6), startLabelPos)

            // 7. Draw Live User GPS Location Indicator!
            if (userGpsLocation != null) {
                val gpsPt = bounds.toOffset(userGpsLocation.first, userGpsLocation.second, width, height)
                
                // Pulsing GPS radar aura ring
                drawCircle(
                    color = Color(0xFF06B6D4).copy(alpha = 0.25f * (1.5f - pulseScale)),
                    radius = (32f * pulseScale) / scale,
                    center = gpsPt
                )
                // Cyan background halo
                drawCircle(
                    color = Color(0xFF06B6D4).copy(alpha = 0.4f),
                    radius = 16f / scale,
                    center = gpsPt
                )
                // White rim
                drawCircle(
                    color = Color.White,
                    radius = 9f / scale,
                    center = gpsPt
                )
                // Cyan solid core
                drawCircle(
                    color = Color(0xFF0891B2),
                    radius = 6f / scale,
                    center = gpsPt
                )

                // Render "TU" (YOU) mini text label above GPS location
                val gpsLabelStyle = TextStyle(
                    color = Color(0xFF22D3EE),
                    fontSize = (8 / scale).sp,
                    fontWeight = FontWeight.ExtraBold
                )
                val gpsLabelLayout = textMeasurer.measure(AnnotatedString(if (isEnglish) "YOU" else "TU"), style = gpsLabelStyle)
                val gpsLabelPos = Offset(gpsPt.x - (gpsLabelLayout.size.width / 2f), gpsPt.y - (24f / scale))
                
                drawRoundRect(
                    color = Color(0xFF0891B2).copy(alpha = 0.5f),
                    topLeft = gpsLabelPos + Offset(-4f, -2f),
                    size = Size(gpsLabelLayout.size.width.toFloat() + 8f, gpsLabelLayout.size.height.toFloat() + 4f),
                    cornerRadius = CornerRadius(4f)
                )
                drawText(gpsLabelLayout, Color(0xFF22D3EE), gpsLabelPos)
            }
        }

        // Overlay Navigation / Zoom Controllers
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp), // Pushed slightly up to not cover details card
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = min(scale * 1.3f, 6.0f) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(44.dp).testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = if (isEnglish) "Zoom In" else "Apropie Harta")
            }

            FloatingActionButton(
                onClick = { scale = max(scale / 1.3f, 0.8f) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(44.dp).testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = if (isEnglish) "Zoom Out" else "Depărtează Harta")
            }

            FloatingActionButton(
                onClick = { 
                    scale = 1.0f
                    offset = Offset.Zero
                    selectedSpotForDetails = null
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

        // Collapsible Legend Panel Overlay at the top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
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
                                color = Color(0xFFE2E8F0),
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
                        val isBus = activeLeg.type == LegType.BUS
                        val legColor = if (isBus) {
                            activeLeg.busColorHex?.let {
                                try {
                                    Color(android.graphics.Color.parseColor(it))
                                } catch (e: Exception) {
                                    Color(0xFF3B82F6)
                                }
                            } ?: Color(0xFF3B82F6)
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
                                    "Stage ${activeNavigationLegIndex + 1} of ${journey.legs.size}: ${if (isBus) "Bus Line ${activeLeg.busLineName ?: ""}" else "Walk on Foot"}"
                                } else {
                                    "Etapa ${activeNavigationLegIndex + 1} din ${journey.legs.size}: ${if (isBus) "Autobuz ${activeLeg.busLineName ?: ""}" else "Deplasare Pietonală"}"
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
                                progress = navigationProgressFraction,
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        val displayDesc = spot.translate(isEnglish).description
                        Text(
                            text = if (displayDesc.isNotEmpty()) displayDesc else (if (isEnglish) "No description added for this spot." else "Nicio descriere adăugată pentru acest obiectiv."),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            maxLines = 2
                        )

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
