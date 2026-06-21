package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.TouristSpot
import com.example.data.SavedItinerary
import com.example.data.TestingLog
import com.example.domain.LegType
import com.example.domain.OptimizedJourney
import com.example.domain.LiveTransitVehicle
import com.example.ui.WeatherInfo

@Composable
fun DashboardTabsWrapper(
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
    isLandmarkSearchInProgress: Boolean = false,
    landmarkSearchError: String? = null,
    onAutoSearchLandmark: (String) -> Unit = {},
    onClearLandmarkSearchError: () -> Unit = {},
    hasLocationPermission: Boolean = false,
    hasNotificationPermission: Boolean = false,
    hasOverlayPermission: Boolean = false,
    onManagePermissionsClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DashboardTabRow(
            selectedDashboardTab = selectedDashboardTab,
            onTabSelected = onTabSelected,
            appSkin = appSkin,
            isEnglishLanguage = isEnglishLanguage
        )

        androidx.compose.material3.HorizontalDivider(
            color = if (appSkin == "VINTAGE_RPG") androidx.compose.ui.graphics.Color(0xFF4C3322) else androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // Tab Contents Switching
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = selectedDashboardTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()) togetherWith 
                                (slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()) togetherWith 
                                (slideOutHorizontally { width -> width } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "TabTransition",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    0 -> SpotsChecklistTab(
                        city = selectedCity,
                        spots = mapSpots,
                        customStartSpot = customStartSpot,
                        onToggle = onToggleSpot,
                        onDeselectAll = onDeselectAllSpots,
                        onDelete = onDeleteSpot,
                        onClearCustom = onClearCustomSpots,
                        onSetStartClick = onSetStartSpot,
                        isEnglish = isEnglishLanguage,
                        weather = currentWeather,
                        selectedPreset = selectedWeatherPreset,
                        onSelectPreset = onSelectWeatherPreset,
                        routingPreference = routingPreference,
                        onPrefChange = onPrefChange,
                        enabledTransitModes = enabledTransitModes,
                        onTransitModeToggle = onTransitModeToggle,
                        appSkin = appSkin,
                        isLandmarkSearchInProgress = isLandmarkSearchInProgress,
                        landmarkSearchError = landmarkSearchError,
                        onAutoSearchLandmark = onAutoSearchLandmark,
                        onClearLandmarkSearchError = onClearLandmarkSearchError
                    )
                    1 -> TimelineTab(
                        isEnglish = isEnglishLanguage,
                        journey = optimizedJourney,
                        startLabel = customStartSpot?.let { translateSpotName(it.name, isEnglishLanguage) } ?: translateSpotName(com.example.domain.TransitNetwork.getStartSpot(selectedCity).name, isEnglishLanguage),
                        startHour = "09:00",
                        city = selectedCity,
                        isSimulatingNavigation = isSimulatingNavigation,
                        activeLegIndex = activeNavigationLegIndex,
                        onStartSimulationClick = onStartSimulationClick,
                        onStopSimulationClick = onStopSimulationClick,
                        liveTransitVehicles = liveTransitVehiclesGlobal,
                        onVehicleClick = onVehicleClick,
                        appSkin = appSkin
                    )
                    2 -> LiveTestingTab(
                        isEnglish = isEnglishLanguage,
                        city = selectedCity,
                        journey = optimizedJourney,
                        testingLogs = testingLogs,
                        useRealDeviceGps = useRealDeviceGps,
                        onToggleRealGps = onToggleRealGps,
                        mapActionMode = mapActionMode,
                        onSetMapActionMode = onSetMapActionMode,
                        onSaveLog = onSaveLog,
                        onDeleteLog = onDeleteLog,
                        onClearLogs = onClearLogs,
                        appSkin = appSkin,
                        hasLocationPermission = hasLocationPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        hasOverlayPermission = hasOverlayPermission,
                        onManagePermissionsClick = onManagePermissionsClick
                    )
                    3 -> AiGuideTab(
                        recommendationText = aiRecommendation,
                        isLoading = isAiLoading,
                        onCallAi = onCallAi,
                        isEnglish = isEnglishLanguage,
                        aiProvider = aiProvider,
                        appSkin = appSkin
                    )
                    4 -> SavedItinerariesTab(
                        isEnglish = isEnglishLanguage,
                        itineraries = savedItineraries,
                        onDelete = onDeleteSavedItinerary,
                        onClearAll = onClearAllSavedItineraries,
                        appSkin = appSkin
                    )
                }
            }
        }
    }
}
