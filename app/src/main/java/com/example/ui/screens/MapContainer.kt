package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TouristSpot
import com.example.domain.OptimizedJourney
import com.example.domain.TransitNetwork
import com.example.domain.LiveTransitVehicle
import com.example.ui.components.InteractiveMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MapContainer(
    modifier: Modifier = Modifier,
    isDashboardVisible: Boolean,
    selectedCity: String,
    mapSpots: List<TouristSpot>,
    customStartSpot: TouristSpot?,
    optimizedJourney: OptimizedJourney?,
    mapActionMode: String,
    userGpsLocation: Pair<Double, Double>?,
    onUserGpsLocationChange: (Pair<Double, Double>?) -> Unit,
    isEnglishLanguage: Boolean,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onMapTapForSpot: (Double, Double) -> Unit,
    onToggleSpotSelection: (Long, Boolean) -> Unit,
    onUpdateCustomStartSpot: (Double, Double, String) -> Unit,
    onDeleteSpot: (Long) -> Unit,
    onToggleDashboard: () -> Unit,
    isSimulatingNavigation: Boolean,
    onSimulatingNavigationChange: (Boolean) -> Unit,
    activeNavigationLegIndex: Int,
    onActiveNavigationLegIndexChange: (Int) -> Unit,
    navigationProgressFraction: Float,
    onNavigationProgressFractionChange: (Float) -> Unit,
    isSoundAlertEnabled: Boolean,
    isStationsVisible: Boolean,
    isTransitLinesVisible: Boolean,
    mapColorSchemeStyle: String,
    isHighResolutionMap: Boolean,
    liveTransitVehiclesGlobal: List<LiveTransitVehicle>,
    focusedTransitVehicle: LiveTransitVehicle?,
    onFocusedVehicleChange: (LiveTransitVehicle?) -> Unit
) {
    val mapCornerRadius = if (isDashboardVisible) 24.dp else 0.dp
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = mapCornerRadius, bottomEnd = mapCornerRadius))
    ) {
        val stations = TransitNetwork.getStationsForCity(selectedCity)
        val lines = TransitNetwork.getLinesForCity(selectedCity)

        InteractiveMap(
            city = selectedCity,
            spots = mapSpots,
            customStartSpot = customStartSpot,
            stations = stations,
            lines = lines,
            journey = optimizedJourney,
            onMapTap = { lat, lng ->
                if (mapActionMode == "SET_GPS") {
                    onUserGpsLocationChange(Pair(lat, lng))
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (isEnglishLanguage) "🎯 Simulated GPS location moved on map!"
                            else "🎯 Locația GPS simulată a fost mutată pe hartă!"
                        )
                    }
                } else {
                    onMapTapForSpot(lat, lng)
                }
            },
            onSpotClick = { spot ->
                onToggleSpotSelection(spot.id, !spot.isSelected)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isEnglishLanguage) "Selection changed for: ${translateSpotName(spot.name, true)}"
                        else "S-a schimbat selectarea pentru: ${spot.name}"
                    )
                }
            },
            onSetStartSpot = { spot ->
                onUpdateCustomStartSpot(spot.latitude, spot.longitude, spot.name)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isEnglishLanguage) "Starting point changed to: ${translateSpotName(spot.name, true)}"
                        else "Punct de pornire schimbat la: ${spot.name}"
                    )
                }
            },
            onDeleteSpot = { id ->
                onDeleteSpot(id)
            },
            isDashboardVisible = isDashboardVisible,
            onToggleDashboard = onToggleDashboard,
            userGpsLocation = userGpsLocation,
            onEnableGpsClicked = {
                if (userGpsLocation == null) {
                    val seedCoords = when (selectedCity) {
                        "București" -> Pair(44.4411, 26.0973) // near Ateneul Român
                        "Brașov" -> Pair(45.6540, 25.6030)
                        "Câmpina" -> Pair(45.1265, 25.7345)
                        "Cluj-Napoca" -> Pair(46.7684, 23.5862) // Memorandului Cluj
                        else -> {
                            val start = TransitNetwork.getStartSpot(selectedCity)
                            Pair(start.latitude, start.longitude)
                        }
                    }
                    onUserGpsLocationChange(seedCoords)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (isEnglishLanguage) "🛰️ GPS Signal Active on Map!"
                            else "🛰️ Semnal GPS Activ pe Hartă!"
                        )
                    }
                } else {
                    onUserGpsLocationChange(null)
                    onSimulatingNavigationChange(false)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (isEnglishLanguage) "GPS Disconnected."
                            else "Gps Deconectat."
                        )
                    }
                }
            },
            onUseGpsAsStart = { lat, lng ->
                onUpdateCustomStartSpot(lat, lng, "Locație GPS Ta")
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isEnglishLanguage) "🗺️ Start set from your exact GPS location!"
                        else "🗺️ Pornire setată din locația ta GPS reală!"
                    )
                }
            },
            isSimulatingNavigation = isSimulatingNavigation,
            activeNavigationLegIndex = activeNavigationLegIndex,
            navigationProgressFraction = navigationProgressFraction,
            onStepCompletedClick = {
                val legsSize = optimizedJourney?.legs?.size ?: 0
                if (isSoundAlertEnabled) {
                    try {
                        val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
                        tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 200)
                    } catch (e: Exception) {
                        // Ignore exceptions on platform emulators
                    }
                }
                if (activeNavigationLegIndex >= legsSize - 1) {
                    // completed! Show complete success card by moving index beyond legs
                    onActiveNavigationLegIndexChange(legsSize)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (isEnglishLanguage) "🎉 Congratulations! You have successfully completed the entire trip!"
                            else "🎉 Felicitări! Ai finalizat cu succes întregul traseu!"
                        )
                    }
                } else {
                    onActiveNavigationLegIndexChange(activeNavigationLegIndex + 1)
                    onNavigationProgressFractionChange(0f)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (isEnglishLanguage) "Stage completed! Moving to the next direction..."
                            else "Etapă finalizată! Se trece la următoarea direcție..."
                        )
                    }
                }
            },
            onStopSimulationClick = {
                onSimulatingNavigationChange(false)
                onActiveNavigationLegIndexChange(0)
                onNavigationProgressFractionChange(0f)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isEnglishLanguage) "Navigation stopped."
                        else "Navigație oprită."
                    )
                }
            },
            isStationsVisible = isStationsVisible,
            isTransitLinesVisible = isTransitLinesVisible,
            mapColorSchemeStyle = mapColorSchemeStyle,
            isHighResolutionMap = isHighResolutionMap,
            isEnglish = isEnglishLanguage,
            liveTransitVehicles = liveTransitVehiclesGlobal,
            focusedTransitVehicle = focusedTransitVehicle,
            onFocusedVehicleDismiss = { onFocusedVehicleChange(null) },
            onVehicleClick = { vehicle ->
                onFocusedVehicleChange(vehicle)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isEnglishLanguage) "⚡ Radar Lock: Auto-focusing on Transit line ${vehicle.lineName}"
                        else "⚡ Radar localizat: Focalizare automată pe linia ${vehicle.lineName}"
                    )
                }
            }
        )
        
        // Starting place indicator badge overlay on map corners
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val start = customStartSpot ?: TransitNetwork.getStartSpot(selectedCity)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFF472B6), modifier = Modifier.size(12.dp))
                Text(
                    text = if (isEnglishLanguage) "Start: ${translateSpotName(start.name, true)}" else "Pornire: ${start.name}",
                    color = Color(0xFFF472B6),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
