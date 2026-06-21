package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TouristSpot
import com.example.domain.BusLine
import com.example.domain.BusStation
import com.example.domain.LegType
import com.example.domain.LiveTransitVehicle
import com.example.domain.OptimizedJourney
import com.example.domain.TransitNetwork
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.screens.translateSpotName
import com.example.ui.components.MapBounds


@Composable
fun CustomCanvasMap(
    city: String,
    bounds: MapBounds,
    spots: List<TouristSpot>,
    customStartSpot: TouristSpot?,
    stations: List<BusStation>,
    lines: List<BusLine>,
    journey: OptimizedJourney?,
    scaleProvider: () -> Float,
    offsetProvider: () -> Offset,
    scale: Float,
    offset: Offset,
    pulseScale: Float,
    animatedPhase: Float,
    isStationsVisible: Boolean,
    isTransitLinesVisible: Boolean,
    mapColorSchemeStyle: String,
    isHighResolutionMap: Boolean,
    isSimulatingNavigation: Boolean,
    activeNavigationLegIndex: Int,
    navigationProgressFraction: Float,
    userGpsLocation: Pair<Double, Double>?,
    liveTransitVehicles: List<LiveTransitVehicle>,
    activeTrackingVehicle: LiveTransitVehicle?,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onStationClick: (BusStation) -> Unit,
    onSpotClick: (TouristSpot) -> Unit,
    onMapClick: (Double, Double) -> Unit,
    isEnglish: Boolean,
    transitLineRoads: Map<String, List<Pair<Double, Double>>>,
    journeyLegRoads: Map<String, List<Pair<Double, Double>>>,
    getSpotCategoryEmoji: (String) -> String,
    textMeasurer: TextMeasurer
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val currentOnScaleChange by rememberUpdatedState(onScaleChange)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)
    
            // 1. Dynamic Map Tiles Layer (Osm/CartoDB Dark Matter) - highly stable!
            TileMapLayer(
                city = city,
                bounds = bounds,
                scaleProvider = scaleProvider,
                offsetProvider = offsetProvider,
                mapColorSchemeStyle = mapColorSchemeStyle,
                isHighResolution = isHighResolutionMap
            )

            Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_map_canvas")
                .pointerInput(city) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val currentScale = scaleProvider()
                        val currentOffset = offsetProvider()
                        
                        val nextScale = kotlin.math.max(0.8f, kotlin.math.min(currentScale * zoom, 6.0f))
                        val nextOffset = centroid + pan - (centroid - currentOffset) / currentScale * nextScale
                        
                        // We must always invoke scale and offset changes when moving or zooming!
                        if (!nextScale.isNaN() && !nextScale.isInfinite()) {
                            currentOnScaleChange(nextScale)
                        }
                        if (!nextOffset.x.isNaN() && !nextOffset.y.isNaN() && !nextOffset.x.isInfinite() && !nextOffset.y.isInfinite()) {
                            currentOnOffsetChange(nextOffset)
                        }
                    }
                }
                .pointerInput(city) {
                    detectTapGestures(
                        onTap = { localOffset ->
                            val width = size.width.toFloat()
                            val height = size.height.toFloat()
                            
                            val currentScale = scaleProvider()
                            val currentOffset = offsetProvider()

                            if (width > 0f && height > 0f && currentScale > 0f && !currentScale.isNaN()) {
                                // Convert local click to coordinate offset considering Zoom and Pan
                                // localOffset = (mapOffset * scale) + offset
                                // Therefore: mapOffset = (localOffset - offset) / scale
                                val mapCoords = (localOffset - currentOffset) / currentScale
                                
                                // Check bounds
                                if (mapCoords.x in 0f..width && mapCoords.y in 0f..height) {
                                    // A. Check if tapped near an active Bus Station first
                                    val stationTolerance = with(density) { 24.dp.toPx() } / currentScale
                                    val clickedStation = if (isStationsVisible) {
                                        val activeStations = journey?.legs?.flatMap { listOfNotNull(it.boardingStation, it.alightingStation) }?.toSet() ?: emptySet()
                                        stations.find { station ->
                                            if (journey != null && station.name !in activeStations) {
                                                false
                                            } else {
                                                val pt = bounds.toOffset(station.latitude, station.longitude, width, height)
                                                val dx = mapCoords.x - pt.x
                                                val dy = mapCoords.y - pt.y
                                                (dx * dx + dy * dy) <= (stationTolerance * stationTolerance)
                                            }
                                        }
                                    } else null

                                    if (clickedStation != null) {
                                        onStationClick(clickedStation)
                                        
                                    } else {
                                        // 1. Check if user tapped near an existing tourist spot marker
                                        val clickTolerance = with(density) { 34.dp.toPx() } / currentScale
                                        val clickedSpot = spots.find { spot ->
                                            val pt = bounds.toOffset(spot.latitude, spot.longitude, width, height)
                                            val dx = mapCoords.x - pt.x
                                            val dy = mapCoords.y - pt.y
                                            (dx * dx + dy * dy) <= (clickTolerance * clickTolerance)
                                        }

                                        if (clickedSpot != null) {
                                            onSpotClick(clickedSpot)
                                            onSpotClick(clickedSpot)
                                            
                                        } else {
                                            val latLng = bounds.toLatLng(mapCoords, width, height)
                                            val lat = latLng.first
                                            val lng = latLng.second
                                            onMapClick(lat, lng)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                .graphicsLayer {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    val currentScale = scaleProvider()
                    val currentOffset = offsetProvider()
                    scaleX = currentScale
                    scaleY = currentScale
                    translationX = currentOffset.x
                    translationY = currentOffset.y
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
                "OsmAnd (HD maps + relief)" -> Color(0xFFE2E8F0)
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
                    val pathColor = when (leg?.type) {
                        LegType.BUS, LegType.METRO, LegType.TROLLEY, LegType.TRAIN -> {
                            leg.busColorHex?.let {
                                try {
                                    Color(android.graphics.Color.parseColor(it))
                                } catch (e: Exception) {
                                    Color(0xFF10B981)
                                }
                            } ?: when (leg.type) {
                                LegType.METRO -> Color(0xFF3B82F6)
                                LegType.TROLLEY -> Color(0xFF14B8A6)
                                LegType.TRAIN -> Color(0xFF8B5CF6)
                                else -> Color(0xFF10B981)
                            }
                        }
                        LegType.TAXI -> Color(0xFFEAB308)
                        else -> Color(0xFFF97316) // WALK
                    }

                    // Draw connecting path line (distinctive, thick, pulsing look with animated dashes)
                    if (leg?.type == LegType.WALK) {
                        // Walking: dark solid underlay silhouette for perfect high-contrast outline on light map grids
                        drawPath(
                            path = legPath,
                            color = Color(0xFF1E293B).copy(alpha = 0.5f),
                            style = Stroke(
                                width = with(density) { 9.dp.toPx() } / scale,
                                pathEffect = PathEffect.cornerPathEffect(10f)
                            )
                        )
                        // Walking: semi-transparent thick outer aura trail
                        drawPath(
                            path = legPath,
                            color = pathColor.copy(alpha = 0.3f),
                            style = Stroke(
                                width = with(density) { 8.dp.toPx() } / scale,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(15f / scale, 12f / scale),
                                    phase = animatedPhase * (25f / scale)
                                )
                            )
                        )
                        // Walking: crisp dashed marching line indicating active path motion
                        drawPath(
                            path = legPath,
                            color = pathColor,
                            style = Stroke(
                                width = with(density) { 4.5.dp.toPx() } / scale,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(16f / scale, 12f / scale),
                                    phase = animatedPhase * (25f / scale)
                                )
                            )
                        )
                    } else {
                        // Bus: solid dark outline contour underlay for extreme premium separation from standard road lanes
                        drawPath(
                            path = legPath,
                            color = Color(0xFF0F172A).copy(alpha = 0.6f),
                            style = Stroke(
                                width = with(density) { 13.dp.toPx() } / scale,
                                pathEffect = PathEffect.cornerPathEffect(10f)
                            )
                        )
                        // Bus: ultra-visible neon outline/aura first to separate from roads
                        drawPath(
                            path = legPath,
                            color = pathColor.copy(alpha = 0.4f),
                            style = Stroke(
                                width = with(density) { 15.dp.toPx() } / scale,
                                pathEffect = PathEffect.cornerPathEffect(10f)
                            )
                        )
                        // Bus: solid primary color line
                        drawPath(
                            path = legPath,
                            color = pathColor,
                            style = Stroke(
                                width = with(density) { 8.dp.toPx() } / scale,
                                pathEffect = PathEffect.cornerPathEffect(10f)
                            )
                        )
                        // Bus: central high-contrast flowing visual animation track (dark slate)
                        drawPath(
                            path = legPath,
                            color = Color(0xFF0F172A).copy(alpha = 0.7f),
                            style = Stroke(
                                width = with(density) { 2.5.dp.toPx() } / scale,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(25f / scale, 45f / scale),
                                    phase = -animatedPhase * (32f / scale)
                                )
                            )
                        )
                    }

                    // Draw Directional Arrows showing the path sequence beautifully
                    val arrowSize = with(density) { 5.5.dp.toPx() } / scale
                    val arrowPositions = listOf(0.25f, 0.5f, 0.75f)
                    arrowPositions.forEach { fraction ->
                        val pA = getPointAlongPath(offsetPoints, fraction)
                        val pB = getPointAlongPath(offsetPoints, fraction + 0.03f)
                        val dx = pB.x - pA.x
                        val dy = pB.y - pA.y
                        val len = sqrt(dx * dx + dy * dy)
                        if (len > 0.01f) {
                            val ux = dx / len
                            val uy = dy / len
                            val tx = -uy
                            val ty = ux
                            
                            val tip = pA + Offset(ux * arrowSize * 1.3f, uy * arrowSize * 1.3f)
                            val left = pA - Offset(ux * arrowSize, uy * arrowSize) + Offset(tx * arrowSize * 0.7f, ty * arrowSize * 0.7f)
                            val right = pA - Offset(ux * arrowSize, uy * arrowSize) - Offset(tx * arrowSize * 0.7f, ty * arrowSize * 0.7f)
                            
                            val arrowPath = Path().apply {
                                moveTo(tip.x, tip.y)
                                lineTo(left.x, left.y)
                                lineTo(right.x, right.y)
                                close()
                            }
                            drawPath(
                                path = arrowPath,
                                color = Color(0xFF0F172A).copy(alpha = 0.9f)
                            )
                        }
                    }

                    // Crawling Traveler Particle: a gorgeous glowing circle flowing along each leg
                    // Animates from start to end based on active phase
                    val progress = (animatedPhase / 100f)
                    val particlePos = getPointAlongPath(offsetPoints, progress)

                    // Pulse aura
                    drawCircle(
                        color = pathColor.copy(alpha = 0.5f),
                        radius = with(density) { 10.dp.toPx() } / scale,
                        center = particlePos
                    )
                    // Bright core (high contrast dark core)
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = with(density) { 4.5.dp.toPx() } / scale,
                        center = particlePos
                    )

                    // Circle marking step order origin direction
                    val stepRadius = with(density) { 11.5.dp.toPx() } / scale
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f),
                        radius = stepRadius + (2f / scale),
                        center = fromPt
                    )
                    drawCircle(
                        color = pathColor,
                        radius = stepRadius,
                        center = fromPt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = stepRadius - (2.5f / scale),
                        center = fromPt
                    )

                    // Order Number annotation text inside stop bubble
                    val stepNum = (i + 1).toString()
                    val textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = (11 / scale).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val textLayout = textMeasurer.measure(AnnotatedString(stepNum), style = textStyle)
                    val textOffset = Offset(fromPt.x - textLayout.size.width / 2, fromPt.y - textLayout.size.height / 2)
                    drawText(textLayout, Color(0xFF0F172A), textOffset)

                    // If this is the last leg, draw a beautiful finish 🏁 bubble at the final point
                    if (i == fullJourneyPoints.size - 2) {
                        val finalPt = bounds.toOffset(p2.latitude, p2.longitude, width, height)
                        val finalRadius = with(density) { 11.5.dp.toPx() } / scale
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.45f),
                            radius = finalRadius + (2f / scale),
                            center = finalPt
                        )
                        drawCircle(
                            color = Color(0xFF10B981), // Emerald finish glow
                            radius = finalRadius,
                            center = finalPt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = finalRadius - (2.5f / scale),
                            center = finalPt
                        )
                        val finishLayout = textMeasurer.measure(
                            AnnotatedString("🏁"),
                            style = TextStyle(fontSize = (11 / scale).sp)
                        )
                        val finishOffset = Offset(
                            finalPt.x - finishLayout.size.width / 2f,
                            finalPt.y - finishLayout.size.height / 2f
                        )
                        drawText(finishLayout, Color.Unspecified, finishOffset)
                    }
                }
            }

            // 4. Draw Bus Stations
            if (isStationsVisible) {
                val outerRingCol = when(mapColorSchemeStyle) {
                    "Cyberpunk" -> Color(0xFF9D00FF)
                    "Muted Gray" -> Color(0xFF666666)
                    "Ocean Breeze" -> Color(0xFF172D4D)
                    "OsmAnd (HD maps + relief)" -> Color(0xFF0F766E)
                    else -> Color(0xFF334155)
                }
                val innerRingCol = when(mapColorSchemeStyle) {
                    "Cyberpunk" -> Color(0xFF00FFFF)
                    "Muted Gray" -> Color(0xFF999999)
                    "Ocean Breeze" -> Color(0xFF64FFDA)
                    "OsmAnd (HD maps + relief)" -> Color(0xFF2DD4BF)
                    else -> Color(0xFF94A3B8)
                }
                val stationBackdropCol = when(mapColorSchemeStyle) {
                    "Cyberpunk" -> Color(0xFF0F0C1B)
                    "Muted Gray" -> Color(0xFF1E2022)
                    "Ocean Breeze" -> Color(0xFF0A192F)
                    "OsmAnd (HD maps + relief)" -> Color(0xFFF1F5F9)
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

            // 4b. Draw Live Transit Vehicles on offline custom Canvas map
            if (isTransitLinesVisible) {
                for (vehicle in liveTransitVehicles) {
                    val pt = bounds.toOffset(vehicle.latitude, vehicle.longitude, width, height)
                    val isFocused = vehicle.id == activeTrackingVehicle?.id
                    
                    // Draw outer breathing pulsing glow for the real-time vehicle (Blue theme!)
                    val vehiclePulseRadius = with(density) { (15.dp.toPx() * pulseScale) } / scale
                    drawCircle(
                        color = (if (isFocused) Color(0xFFF43F5E) else Color(0xFF3B82F6)).copy(alpha = 0.25f * (1.5f - pulseScale)),
                        radius = vehiclePulseRadius,
                        center = pt
                    )
                    
                    if (isFocused) {
                        // Extra neon magenta highlight ring
                        val targetRingRadius = with(density) { 24.dp.toPx() } / scale
                        drawCircle(
                            color = Color(0xFFF43F5E).copy(alpha = 0.45f * (1.5f - pulseScale)),
                            radius = targetRingRadius,
                            center = pt,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f / scale)
                        )
                    }

                    // Outer ring
                    drawCircle(
                        color = if (isFocused) Color(0xFFBE123C) else Color(0xFF1D4ED8),
                        radius = 10f / scale,
                        center = pt
                    )
                    
                    // Filled background
                    drawCircle(
                        color = if (isFocused) Color(0xFFF43F5E) else Color(0xFF3B82F6),
                        radius = 7.5f / scale,
                        center = pt
                    )
                    
                    // Draw the line label (like "335" or "L1") above the vehicle bubble
                    val labelText = vehicle.lineName
                    val labelStyle = TextStyle(
                        color = Color.White,
                        fontSize = (8 / scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    val labelLayout = textMeasurer.measure(AnnotatedString(labelText), style = labelStyle)
                    val labelPos = Offset(pt.x - (labelLayout.size.width / 2f), pt.y - (18f / scale) - (labelLayout.size.height / 2f))
                    
                    // Backing rectangular badge for label readability
                    drawRoundRect(
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        topLeft = labelPos + Offset(-4f, -2f),
                        size = Size(labelLayout.size.width.toFloat() + 8f, labelLayout.size.height.toFloat() + 4f),
                        cornerRadius = CornerRadius(4f)
                    )
                    drawText(labelLayout, if (isFocused) Color(0xFFFDA4AF) else Color(0xFF38BDF8), labelPos)
                }
            }

            // 5. Draw Tourist Spot Pins as Gorgeous Descriptive Miniature Cards/Thumbnails
            for (spot in spots) {
                if (journey != null && !spot.isSelected) {
                    continue // Non-essential tourist spots disappear when a route is active to focus on the selected path!
                }
                val pt = bounds.toOffset(spot.latitude, spot.longitude, width, height)
                val pinColor = when {
                    !spot.isSelected -> Color(0xFF64748B) // Premium Slate Gray for inactive
                    spot.isCustom -> Color(0xFFF59E0B) // Amber for custom user spots
                    else -> Color(0xFF6366F1) // Indigo Accent for planned/preset spots
                }

                // Smooth breathing pulsing soft aura glow for active markers
                if (spot.isSelected) {
                    val pulseCol = if (spot.isCustom) Color(0xFFF59E0B) else Color(0xFF6366F1)
                    val pulseRadius = with(density) { (24.dp.toPx() * pulseScale) } / scale
                    drawCircle(
                        color = pulseCol.copy(alpha = 0.22f * (1.5f - pulseScale)),
                        radius = pulseRadius,
                        center = pt
                    )
                }

                // Convert physical target physical dimensions using local screen density
                val r = with(density) { 15.dp.toPx() } / scale
                val h = with(density) { 25.dp.toPx() } / scale

                // Realistic soft underlay drop-shadow for 3D depth pop-out
                val shadowShift = 2.5f / scale
                val shadowPointer = Path().apply {
                    moveTo(pt.x + shadowShift, pt.y + shadowShift)
                    lineTo(pt.x - r * 0.9f + shadowShift, pt.y - h + shadowShift)
                    lineTo(pt.x + r * 0.9f + shadowShift, pt.y - h + shadowShift)
                    close()
                }
                drawPath(path = shadowPointer, color = Color.Black.copy(alpha = 0.35f))
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = r,
                    center = Offset(pt.x + shadowShift, pt.y - h + shadowShift)
                )

                // Visual base anchor pointer connecting thumbnail directly to the geographic point
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
                    radius = r * 0.84f,
                    center = Offset(pt.x, pt.y - h)
                )

                // Render descriptive categorized emoji as high-quality thumbnail image centered inside
                val categoryEmoji = getSpotCategoryEmoji(spot.name)
                val emojiStyle = TextStyle(
                    fontSize = (14 / scale).sp
                )
                val emojiLayout = textMeasurer.measure(AnnotatedString(categoryEmoji), style = emojiStyle)
                val emojiOffset = Offset(
                    pt.x - emojiLayout.size.width / 2f,
                    pt.y - h - emojiLayout.size.height / 2f
                )
                drawText(emojiLayout, Color.Unspecified, emojiOffset)

                // Rich Descriptive Floating Card Label specifying name, category, and visit time
                val labelStyle = TextStyle(
                    color = if (spot.isSelected) Color.White else Color(0xFFE2E8F0),
                    fontSize = (11 / scale).sp,
                    fontWeight = if (spot.isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
                val labelLayout = textMeasurer.measure(AnnotatedString(translateSpotName(spot.name, isEnglish)), style = labelStyle)
                val labelPos = Offset(pt.x - (labelLayout.size.width / 2f), pt.y - h - r - (16f / scale))

                // Render multi-layered glass rounded box under text label (Glassmorphic)
                drawRoundRect(
                    color = pinColor.copy(alpha = 0.5f),
                    topLeft = labelPos + Offset(-8f, -4f),
                    size = Size(labelLayout.size.width.toFloat() + 16f, labelLayout.size.height.toFloat() + 8f),
                    cornerRadius = CornerRadius(6f),
                    style = Stroke(width = 1.2f / scale)
                )
                drawRoundRect(
                    color = Color(0xFF1E293B).copy(alpha = 0.92f),
                    topLeft = labelPos + Offset(-8f, -4f),
                    size = Size(labelLayout.size.width.toFloat() + 16f, labelLayout.size.height.toFloat() + 8f),
                    cornerRadius = CornerRadius(6f)
                )
                drawText(labelLayout, if (spot.isSelected) Color.White else Color(0xFF94A3B8), labelPos)
            }

            // 6. Draw Custom Start Spot Pin as a highly attractive pink launcher capsule with a rocket emoji!
            val startSpot = customStartSpot ?: TransitNetwork.getStartSpot(city)
            val startPt = bounds.toOffset(startSpot.latitude, startSpot.longitude, width, height)
            
            val startPinColor = Color(0xFFEC4899)
            val rStart = with(density) { 17.dp.toPx() } / scale
            val hStart = with(density) { 28.dp.toPx() } / scale

            // Pulsing start glowing aura circle
            drawCircle(
                color = startPinColor.copy(alpha = 0.28f * (1.5f - pulseScale)),
                radius = with(density) { (28.dp.toPx() * pulseScale) } / scale,
                center = startPt
            )
            
            // Start shadow
            val startShadowShift = 2.5f / scale
            val startShadowPath = Path().apply {
                moveTo(startPt.x + startShadowShift, startPt.y + startShadowShift)
                lineTo(startPt.x - rStart * 0.9f + startShadowShift, startPt.y - hStart + startShadowShift)
                lineTo(startPt.x + rStart * 0.9f + startShadowShift, startPt.y - hStart + startShadowShift)
                close()
            }
            drawPath(path = startShadowPath, color = Color.Black.copy(alpha = 0.35f))
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = rStart,
                center = Offset(startPt.x + startShadowShift, startPt.y - hStart + startShadowShift)
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
                radius = rStart * 0.84f,
                center = Offset(startPt.x, startPt.y - hStart)
            )

            // Starting launcher rocket emoji centered inside Custom Start spot
            val startEmojiLayout = textMeasurer.measure(AnnotatedString("🚀"), style = TextStyle(fontSize = (15 / scale).sp))
            val startEmojiOffset = Offset(
                startPt.x - startEmojiLayout.size.width / 2f,
                startPt.y - hStart - startEmojiLayout.size.height / 2f
            )
            drawText(startEmojiLayout, Color.Unspecified, startEmojiOffset)

            val startLabelStyle = TextStyle(
                color = Color(0xFFF472B6),
                fontSize = (11 / scale).sp,
                fontWeight = FontWeight.Black
            )
            val displayName = if (isEnglish) "START: ${translateSpotName(startSpot.name, true)}" else "START: ${startSpot.name}"
            val startLabelLayout = textMeasurer.measure(AnnotatedString(displayName), style = startLabelStyle)
            val startLabelPos = Offset(startPt.x - (startLabelLayout.size.width / 2f), startPt.y + (16f / scale))
            
            drawRoundRect(
                color = Color(0xFFEC4899).copy(alpha = 0.5f),
                topLeft = startLabelPos + Offset(-8f, -4f),
                size = Size(startLabelLayout.size.width.toFloat() + 16f, startLabelLayout.size.height.toFloat() + 8f),
                cornerRadius = CornerRadius(6f),
                style = Stroke(width = 1.2f / scale)
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.94f),
                topLeft = startLabelPos + Offset(-8f, -4f),
                size = Size(startLabelLayout.size.width.toFloat() + 16f, startLabelLayout.size.height.toFloat() + 8f),
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
}
