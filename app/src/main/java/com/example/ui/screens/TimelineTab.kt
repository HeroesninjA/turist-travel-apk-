package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TouristSpot
import com.example.domain.LegType
import com.example.domain.OptimizedJourney
import com.example.domain.LiveTransitVehicle
import com.example.domain.TransportApiEngine

@Composable
fun TimelineTab(
    journey: OptimizedJourney?,
    startLabel: String,
    startHour: String,
    city: String,
    isSimulatingNavigation: Boolean = false,
    onStartSimulationClick: () -> Unit = {},
    onStopSimulationClick: () -> Unit = {},
    isEnglish: Boolean = false,
    activeLegIndex: Int = 0,
    liveTransitVehicles: List<LiveTransitVehicle> = emptyList(),
    onVehicleClick: ((LiveTransitVehicle) -> Unit)? = null,
    appSkin: String = "MODERN"
) {
    // Dynamic timetable hour calculation
    val startMin = 540 // 09:00 AM
    var rollingMin = startMin
    val timelineTimes = remember(journey) {
        val list = mutableListOf<Pair<Int, Int>>() // Arrival, Departure
        val legsCount = journey?.legs?.size ?: 0
        for (i in 0 until legsCount) {
            val leg = journey?.legs?.getOrNull(i) ?: continue
            val arrival = rollingMin + leg.durationMinutes
            val spot = journey.orderedSpots.getOrNull(i)
            val visitDuration = spot?.visitDurationMinutes ?: 0
            val departure = arrival + visitDuration
            list.add(Pair(arrival, departure))
            rollingMin = departure
        }
        list
    }

    fun formatMinutesToTime(totalMin: Int): String {
        val totalHrs = (totalMin / 60) % 24
        val remainingMin = totalMin % 60
        return String.format("%02d:%02d", totalHrs, remainingMin)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ALWAYS show live operator transit banner at the top
        item {
            LiveOperatorApiPanel(
                city = city,
                isEnglish = isEnglish,
                liveVehiclesPassed = liveTransitVehicles,
                onVehicleClick = onVehicleClick,
                appSkin = appSkin
            )
        }

        if (journey != null && journey.orderedSpots.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("simulation_trigger_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") {
                            if (isSimulatingNavigation) Color(0xFF1C1A14) else Color(0xFF261910)
                        } else {
                            if (isSimulatingNavigation) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                        }
                    ),
                    border = if (appSkin == "VINTAGE_RPG") {
                        if (isSimulatingNavigation) BorderStroke(2.dp, Color(0xFF10B981))
                        else BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                    } else {
                        BorderStroke(1.5.dp, if (isSimulatingNavigation) Color(0xFF10B981) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSimulatingNavigation) {
                                    if (isEnglish) "🧭 Navigation Active" else "🧭 Navigație Activă"
                                } else {
                                    if (isEnglish) "🗺️ Start Route" else "🗺️ Pornește în Traseu"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSimulatingNavigation) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSimulatingNavigation) {
                                    if (isEnglish) "Monitor the live GPS tracker moving through the route." else "Urmărește trackerul GPS live cum traversează traseul."
                                } else {
                                    if (isEnglish) "Launch interactive step-by-step guidance for this itinerary." else "Pornește ghidul interactiv pas cu pas pentru acest itinerar."
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Button(
                            onClick = {
                                if (isSimulatingNavigation) {
                                    onStopSimulationClick()
                                } else {
                                    onStartSimulationClick()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSimulatingNavigation) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        ) {
                            Icon(
                                imageVector = if (isSimulatingNavigation) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSimulatingNavigation) {
                                    if (isEnglish) "Stop" else "Oprește"
                                } else {
                                    "Start"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (journey == null || journey.orderedSpots.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = if (appSkin == "VINTAGE_RPG") {
                        BorderStroke(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                    } else null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (isEnglish) "No route generated yet" else "Niciun traseu generat încă",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEnglish) "Select tourist spots from the 'Attractions' tab to automatically calculate an optimized route with transit steps!" else "Alege puncte turistice din tab-ul 'Atracții' pentru a calcula automat un traseu optimizat cu etapele de tranzit!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Chronological Schedule: START PLACE NODE
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF2E1B26) else Color(0xFFEC4899).copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFEC4899).copy(alpha = if (appSkin == "VINTAGE_RPG") 0.8f else 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFEC4899).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "START: ${startLabel}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEnglish) "Initial departure point" else "Punct inițial de plecare",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEC4899), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Departure: $startHour" else "Plecare: $startHour",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Stepper segments (Leg + Destination Spot)
            items(journey.legs.size) { index ->
                val leg = journey.legs[index]
                val arrivalDeparture = timelineTimes.getOrNull(index)
                val arrivalStr = arrivalDeparture?.let { formatMinutesToTime(it.first) } ?: "09:00"
                val departureStr = arrivalDeparture?.let { formatMinutesToTime(it.second) } ?: "10:00"

                val isActiveStep = isSimulatingNavigation && index == activeLegIndex
                val isDestinationTargetNext = isSimulatingNavigation && index == activeLegIndex

                val legColor = if (leg.type == LegType.BUS) {
                    leg.busColorHex?.let {
                        try {
                            Color(android.graphics.Color.parseColor(it))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                    } ?: MaterialTheme.colorScheme.primary
                } else {
                    Color(0xFF94A3B8)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Connection segment with intrinsic minimum height
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Upgraded High-visibility Stepper Timeline Rail
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight()
                        ) {
                            // Top connector dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isActiveStep) Color(0xFF10B981) else legColor, CircleShape)
                            )
                            // Rail line segment top
                            Box(
                                modifier = Modifier
                                    .width(if (leg.type == LegType.BUS) 4.dp else 2.dp)
                                    .weight(1f)
                                    .background(
                                        color = (if (isActiveStep) Color(0xFF10B981) else legColor).copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            // Mode identifier emoji
                            Text(
                                text = if (leg.type == LegType.BUS) "🚌" else "🚶",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            // Rail line segment bottom
                            Box(
                                modifier = Modifier
                                    .width(if (leg.type == LegType.BUS) 4.dp else 2.dp)
                                    .weight(1f)
                                    .background(
                                        color = (if (isActiveStep) Color(0xFF10B981) else legColor).copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            // Bottom connector dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isActiveStep) Color(0xFF10B981) else legColor, CircleShape)
                            )
                        }

                        // Details block
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (appSkin == "VINTAGE_RPG") {
                                    if (isActiveStep) Color(0xFF1E2822) else Color(0xFF261910)
                                } else {
                                    if (isActiveStep) {
                                        Color(0xFF10B981).copy(alpha = 0.08f)
                                    } else if (leg.type == LegType.BUS) {
                                        legColor.copy(alpha = 0.08f) 
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    }
                                }
                            ),
                            border = if (appSkin == "VINTAGE_RPG") {
                                if (isActiveStep) {
                                    BorderStroke(2.dp, Color(0xFF10B981))
                                } else if (leg.type == LegType.BUS) {
                                    BorderStroke(1.5.dp, legColor)
                                } else {
                                    BorderStroke(1.2.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                                }
                            } else {
                                BorderStroke(
                                    width = if (isActiveStep) 2.dp else 1.dp,
                                    color = if (isActiveStep) {
                                        Color(0xFF10B981)
                                    } else if (leg.type == LegType.BUS) {
                                        legColor.copy(alpha = 0.35f) 
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (isActiveStep) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF10B981), CircleShape)
                                        )
                                        Text(
                                            text = if (isEnglish) "REAL-TIME NAVIGATION ACTIVE" else "NAVIGAȚIE ÎN TIMP REAL",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF10B981),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (leg.type == LegType.BUS) Icons.Default.LocationOn else Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = if (isActiveStep) Color(0xFF10B981) else legColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (leg.type == LegType.BUS) (leg.busLineName ?: (if (isEnglish) "Bus" else "Autobuz")) else (if (isEnglish) "Walking" else "Plimbare pe Jos"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isActiveStep) Color(0xFF10B981) else if (leg.type == LegType.BUS) legColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background((if (isActiveStep) Color(0xFF10B981) else legColor).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${leg.durationMinutes} min",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActiveStep) Color(0xFF10B981) else if (leg.type == LegType.BUS) legColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = translateDirections(leg.directions, isEnglish),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // INJECT REAL-TIME LIVE GPS ARRIVAL TIME COUNTDOWN FOR BUS LEGS
                                if (leg.type == LegType.BUS) {
                                    val arrivals = remember(leg) {
                                        TransportApiEngine.queryStationArrivals(
                                            leg.boardingStation ?: leg.fromPlaceName,
                                            leg.busLineName ?: "Autobuz"
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFFEF4444), CircleShape)
                                        )
                                        Text(
                                            text = if (isEnglish) "LIVE: ARRIVING IN ${arrivals.first()} MINS (Next: ${arrivals.drop(1).joinToString(", ")} mins)" else "LIVE: SOSIRE ÎN ${arrivals.first()} MIN (Următoarele: ${arrivals.drop(1).joinToString(", ")} min)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF87171)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // The destination place visit card
                    val destinationIndex = index
                    if (destinationIndex < journey.orderedSpots.size) {
                        val spot = journey.orderedSpots[destinationIndex].translate(isEnglish)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDestinationTargetNext) {
                                    if (appSkin == "VINTAGE_RPG") Color(0xFF1E2822) else Color(0xFF10B981).copy(alpha = 0.05f)
                                } else {
                                    if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (appSkin == "VINTAGE_RPG") {
                                if (isDestinationTargetNext) {
                                    BorderStroke(2.dp, Color(0xFF10B981))
                                } else {
                                    BorderStroke(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                                }
                            } else {
                                BorderStroke(
                                    width = if (isDestinationTargetNext) 2.dp else 1.dp,
                                    color = if (isDestinationTargetNext) {
                                        Color(0xFF10B981)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    }
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 🎨 THEME-COLORED PERSONALIZED MINIATURE THUMBNAIL
                                    val emoji = getSpotCategoryEmoji(spot.name)
                                    val gradientColors = when (emoji) {
                                        "🌳" -> listOf(Color(0xFF065F46), Color(0xFF047857), Color(0xFF10B981))
                                        "🏛️" -> listOf(Color(0xFF1E1E2C), Color(0xFF312E81), Color(0xFF4F46E5))
                                        "⛪" -> listOf(Color(0xFF130F40), Color(0xFF2C3A47), Color(0xFF1B1464))
                                        "🏰" -> listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFFBBF24))
                                        "🎭" -> listOf(Color(0xFF701A75), Color(0xFF9D174D), Color(0xFFF43F5E))
                                        "🔭" -> listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF581C87))
                                        "🏟️" -> listOf(Color(0xFF111827), Color(0xFF1E3A8A), Color(0xFF3B82F6))
                                        "🌴" -> listOf(Color(0xFF065F46), Color(0xFF0D9488), Color(0xFF2DD4BF))
                                        "🪦" -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF64748B))
                                        "📖" -> listOf(Color(0xFF3B1F10), Color(0xFF6B4226), Color(0xFFB07D62))
                                        "🏮" -> listOf(Color(0xFF5B1616), Color(0xFF881337), Color(0xFFE11D48))
                                        else -> listOf(Color(0xFF1E293B), Color(0xFF475569), Color(0xFF94A3B8))
                                    }

                                    Box(
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        // Main miniature thumbnail box
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .align(Alignment.BottomStart)
                                                .background(
                                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(gradientColors),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = emoji,
                                                fontSize = 18.sp
                                            )
                                        }

                                        // Sequence badge overlay (Step Number)
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .align(Alignment.TopEnd)
                                                .background(
                                                    if (isDestinationTargetNext) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                )
                                                .border(1.dp, Color(0xFF1E293B), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (destinationIndex + 1).toString(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = translateSpotName(spot.name, isEnglish),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        // High-fidelity description snippet for tourist spots
                                        if (spot.description.isNotEmpty()) {
                                            Text(
                                                text = spot.description,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = if (isEnglish) "Visit duration: ${spot.visitDurationMinutes} mins" else "Vizită de: ${spot.visitDurationMinutes} min",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(6.dp))

                                // Visual Hour Interval Tag
                                Box(
                                    modifier = Modifier
                                        .background((if (isDestinationTargetNext) Color(0xFF10B981) else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, (if (isDestinationTargetNext) Color(0xFF10B981) else MaterialTheme.colorScheme.primary).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$arrivalStr - $departureStr",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isDestinationTargetNext) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
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
