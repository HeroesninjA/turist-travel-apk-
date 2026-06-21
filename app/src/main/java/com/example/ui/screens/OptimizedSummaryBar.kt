package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.OptimizedJourney

@Composable
fun OptimizedSummaryBar(
    city: String,
    journey: OptimizedJourney?,
    onSaveClick: () -> Unit,
    onShareClick: (() -> Unit)? = null,
    isCollapsed: Boolean = false,
    onToggleDashboard: (() -> Unit)? = null,
    isSimulatingNavigation: Boolean = false,
    onStartSimulationClick: () -> Unit = {},
    onStopSimulationClick: () -> Unit = {},
    onHideCompletely: (() -> Unit)? = null,
    isEnglish: Boolean = false
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                if (journey != null) {
                    val count = journey.orderedSpots.size
                    val totalDuration = journey.totalDurationMinutes
                    val hours = totalDuration / 60
                    val minutes = totalDuration % 60
                    val fare = journey.totalBusFareLei

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isEnglish) "OPTIMIZED ROUTE" else "RUTĂ OPTIMIZATĂ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val taxiCost = journey.totalTaxiCostLei
                    val fareText = if (taxiCost > 0) {
                        "🎫 $fare Lei + 🚖 $taxiCost Lei (Uber/Taxi)"
                    } else {
                        "🎫 $fare Lei"
                    }
                    Text(
                        text = if (isEnglish) "🏛️ $count spot${if (count != 1) "s" else ""} • 🕒 ${hours}h ${minutes}m • $fareText" else "🏛️ $count obiective • 🕒 ${hours}h ${minutes}m • $fareText",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isEnglish) "Select attractions in $city" else "Alege atracții în $city",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEnglish) "Select sights from the tab." else "Selectează obiective din tab.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (journey != null) {
                    // Start/Stop simulation button
                    IconButton(
                        onClick = {
                            if (isSimulatingNavigation) {
                                onStopSimulationClick()
                            } else {
                                onStartSimulationClick()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("summary_bar_simulation_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = if (isSimulatingNavigation) Color(0xFFEF4444) else Color(0xFF10B981),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSimulatingNavigation) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isSimulatingNavigation) {
                                    if (isEnglish) "Stop route guidance" else "Oprește ghidajul"
                                } else {
                                    if (isEnglish) "Start route guidance" else "Pornește ghidajul"
                                },
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onShareClick != null) {
                        IconButton(
                            onClick = onShareClick,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("share_itinerary_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = if (isEnglish) "Share" else "Distribuie",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Save itinerary button
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("save_itinerary_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (isEnglish) "Save offline" else "Salvează online",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (onToggleDashboard != null) {
                    // Expand/Collapse entire dashboard card toggle button
                    IconButton(
                        onClick = onToggleDashboard,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("summary_bar_collapse_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isCollapsed) {
                                    if (isEnglish) "Show dashboard panel" else "Afișează panoul"
                                } else {
                                    if (isEnglish) "Hide dashboard panel" else "Ascunde panoul"
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isCollapsed && onHideCompletely != null) {
                    // Close/Hide completely button inside collapsed state
                    IconButton(
                        onClick = onHideCompletely,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("summary_bar_hide_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isEnglish) "Hide completely" else "Ascunde complet",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
