package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.TouristSpot
import com.example.data.SavedItinerary
import com.example.data.TestingLog
import com.example.domain.LegType
import com.example.domain.OptimizedJourney
import com.example.domain.LiveTransitVehicle
import com.example.ui.WeatherInfo

@Composable
fun ColumnScope.DashboardPanel(
    isDashboardExpanded: Boolean,
    onToggleDashboardExpanded: (Boolean) -> Unit,
    onHideDashboardCompletely: () -> Unit,
    appSkin: String,
    selectedDashboardTab: Int,
    onTabSelected: (Int) -> Unit,
    isEnglishLanguage: Boolean,
    selectedCity: String,
    mapSpots: List<TouristSpot>,
    customStartSpot: TouristSpot?,
    onToggleSpot: (Long, Boolean) -> Unit,
    onDeselectAllSpots: () -> Unit,
    onDeleteSpot: (Long) -> Unit,
    onClearCustomSpots: () -> Unit,
    onSetStartSpot: (TouristSpot) -> Unit,
    currentWeather: WeatherInfo?,
    selectedWeatherPreset: String,
    onSelectWeatherPreset: (String) -> Unit,
    routingPreference: String,
    onPrefChange: (String) -> Unit,
    enabledTransitModes: Set<LegType>,
    onTransitModeToggle: (LegType) -> Unit,
    optimizedJourney: OptimizedJourney?,
    isSimulatingNavigation: Boolean,
    activeNavigationLegIndex: Int,
    onStartSimulationClick: () -> Unit,
    onStopSimulationClick: () -> Unit,
    liveTransitVehiclesGlobal: List<LiveTransitVehicle>,
    onVehicleClick: (LiveTransitVehicle) -> Unit,
    testingLogs: List<TestingLog>,
    useRealDeviceGps: Boolean,
    onToggleRealGps: (Boolean) -> Unit,
    mapActionMode: String,
    onSetMapActionMode: (String) -> Unit,
    onSaveLog: (String, String, String) -> Unit,
    onDeleteLog: (Long) -> Unit,
    onClearLogs: () -> Unit,
    aiRecommendation: String,
    isAiLoading: Boolean,
    onCallAi: () -> Unit,
    aiProvider: String,
    savedItineraries: List<SavedItinerary>,
    onDeleteSavedItinerary: (Long) -> Unit,
    onClearAllSavedItineraries: () -> Unit,
    onSaveItineraryClick: () -> Unit,
    onShareItineraryClick: () -> Unit,
    isLandmarkSearchInProgress: Boolean = false,
    landmarkSearchError: String? = null,
    onAutoSearchLandmark: (String) -> Unit = {},
    onClearLandmarkSearchError: () -> Unit = {},
    hasLocationPermission: Boolean = false,
    hasNotificationPermission: Boolean = false,
    hasOverlayPermission: Boolean = false,
    onManagePermissionsClick: () -> Unit = {}
) {
    if (isDashboardExpanded) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .run {
                    if (appSkin == "VINTAGE_RPG") {
                        this.border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))),
                            shape = RoundedCornerShape(20.dp)
                        )
                    } else this
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OptimizedSummaryBar(
                    city = selectedCity,
                    journey = optimizedJourney,
                    onSaveClick = onSaveItineraryClick,
                    onShareClick = onShareItineraryClick,
                    isCollapsed = false,
                    onToggleDashboard = { onToggleDashboardExpanded(false) },
                    onHideCompletely = onHideDashboardCompletely,
                    isSimulatingNavigation = isSimulatingNavigation,
                    isEnglish = isEnglishLanguage,
                    onStartSimulationClick = onStartSimulationClick,
                    onStopSimulationClick = onStopSimulationClick
                )

                DashboardTabsWrapper(
                    appSkin = appSkin,
                    selectedDashboardTab = selectedDashboardTab,
                    onTabSelected = onTabSelected,
                    isEnglishLanguage = isEnglishLanguage,
                    selectedCity = selectedCity,
                    mapSpots = mapSpots,
                    customStartSpot = customStartSpot,
                    onToggleSpot = onToggleSpot,
                    onDeselectAllSpots = onDeselectAllSpots,
                    onDeleteSpot = onDeleteSpot,
                    onClearCustomSpots = onClearCustomSpots,
                    onSetStartSpot = onSetStartSpot,
                    currentWeather = currentWeather,
                    selectedWeatherPreset = selectedWeatherPreset,
                    onSelectWeatherPreset = onSelectWeatherPreset,
                    routingPreference = routingPreference,
                    onPrefChange = onPrefChange,
                    enabledTransitModes = enabledTransitModes,
                    onTransitModeToggle = onTransitModeToggle,
                    optimizedJourney = optimizedJourney,
                    isSimulatingNavigation = isSimulatingNavigation,
                    activeNavigationLegIndex = activeNavigationLegIndex,
                    onStartSimulationClick = onStartSimulationClick,
                    onStopSimulationClick = onStopSimulationClick,
                    liveTransitVehiclesGlobal = liveTransitVehiclesGlobal,
                    onVehicleClick = onVehicleClick,
                    testingLogs = testingLogs,
                    useRealDeviceGps = useRealDeviceGps,
                    onToggleRealGps = onToggleRealGps,
                    mapActionMode = mapActionMode,
                    onSetMapActionMode = onSetMapActionMode,
                    onSaveLog = onSaveLog,
                    onDeleteLog = onDeleteLog,
                    onClearLogs = onClearLogs,
                    aiRecommendation = aiRecommendation,
                    isAiLoading = isAiLoading,
                    onCallAi = onCallAi,
                    aiProvider = aiProvider,
                    savedItineraries = savedItineraries,
                    onDeleteSavedItinerary = onDeleteSavedItinerary,
                    onClearAllSavedItineraries = onClearAllSavedItineraries,
                    isLandmarkSearchInProgress = isLandmarkSearchInProgress,
                    landmarkSearchError = landmarkSearchError,
                    onAutoSearchLandmark = onAutoSearchLandmark,
                    onClearLandmarkSearchError = onClearLandmarkSearchError,
                    hasLocationPermission = hasLocationPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    onManagePermissionsClick = onManagePermissionsClick
                )
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .run {
                    if (appSkin == "VINTAGE_RPG") {
                        this.border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else this
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            OptimizedSummaryBar(
                city = selectedCity,
                journey = optimizedJourney,
                onSaveClick = onSaveItineraryClick,
                onShareClick = onShareItineraryClick,
                isCollapsed = true,
                onToggleDashboard = { onToggleDashboardExpanded(true) },
                onHideCompletely = onHideDashboardCompletely,
                isSimulatingNavigation = isSimulatingNavigation,
                isEnglish = isEnglishLanguage,
                onStartSimulationClick = onStartSimulationClick,
                onStopSimulationClick = onStopSimulationClick
            )
        }
    }
}
