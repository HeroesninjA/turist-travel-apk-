package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TouristSpot
import com.example.data.SavedItinerary
import com.example.domain.LegType
import com.example.domain.OptimizedJourney
import com.example.domain.TransitNetwork
import com.example.domain.TransportApiEngine
import com.example.domain.TransportOperator
import com.example.domain.TransitAlert
import com.example.domain.LiveTransitVehicle
import com.example.ui.AppViewModel
import com.example.ui.WeatherInfo
import com.example.ui.components.InteractiveMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.IntrinsicSize
import kotlinx.coroutines.launch
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.TestingLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCity by viewModel.selectedCity.collectAsState()
    val citySpots by viewModel.citySpots.collectAsState()
    val openaiApiKey by viewModel.openaiApiKey.collectAsState()
    val openaiBaseUrl by viewModel.openaiBaseUrl.collectAsState()
    val openaiModel by viewModel.openaiModel.collectAsState()
    val aiProvider by viewModel.aiProvider.collectAsState()
    val openrouterApiKey by viewModel.openrouterApiKey.collectAsState()
    val openrouterModel by viewModel.openrouterModel.collectAsState()
    val customStartSpot by viewModel.customStartSpot.collectAsState()
    val savedItineraries by viewModel.savedItineraries.collectAsState()
    val optimizedJourney by viewModel.optimizedJourney.collectAsState()
    val routingPreference by viewModel.routingPreference.collectAsState()
    val enabledTransitModes by viewModel.enabledTransitModes.collectAsState()
    val aiRecommendation by viewModel.aiRecommendation.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val currentWeather by viewModel.currentWeather.collectAsState()
    val selectedWeatherPreset by viewModel.selectedWeatherPreset.collectAsState()
    val availableCities by viewModel.availableCities.collectAsState()
    val isIndexingCity by viewModel.isIndexingCity.collectAsState()
    val indexingStatus by viewModel.indexingStatus.collectAsState()
    val isLandmarkSearchInProgress by viewModel.landmarkSearchInProgress.collectAsState()
    val landmarkSearchError by viewModel.landmarkSearchError.collectAsState()

    // Overlay add spot dialog state
    var showAddSpotDialog by remember { mutableStateOf(false) }
    var clickedLat by remember { mutableStateOf(0.0) }
    var clickedLng by remember { mutableStateOf(0.0) }

    // Dialog form state
    var newSpotName by remember { mutableStateOf("") }
    var newSpotDesc by remember { mutableStateOf("") }
    var newSpotDuration by remember { mutableStateOf("60") } // string for easy editing

    // Active bottom card tab state
    var selectedDashboardTab by remember { mutableStateOf(0) } // 0: Checklist, 1: Timeline, 2: Live Testing, 3: AI Guide, 4: Saved

    var isDashboardVisible by remember { mutableStateOf(true) }
    var isDashboardExpanded by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- SETTINGS STATES ---
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isStationsVisible by rememberSaveable { mutableStateOf(true) }
    var isTransitLinesVisible by rememberSaveable { mutableStateOf(true) }
    var simulationSpeedMultiplier by rememberSaveable { mutableStateOf(1.0f) }
    var mapColorSchemeStyle by rememberSaveable { mutableStateOf("OSM Standard") }
    var isHighResolutionMap by rememberSaveable { mutableStateOf(true) }
    var isSoundAlertEnabled by rememberSaveable { mutableStateOf(true) }
    var isEnglishLanguage by rememberSaveable { mutableStateOf(false) }
    var appSkin by rememberSaveable { mutableStateOf("MODERN") } // "MODERN" vs "VINTAGE_RPG"

    // --- LIVE GPS LOCATION & ROUTE NAVIGATION SIMULATION ---
    var userGpsLocation by remember(selectedCity) { mutableStateOf<Pair<Double, Double>?>(null) }
    var isSimulatingNavigation by remember { mutableStateOf(false) }

    val mapSpots = remember(isSimulatingNavigation, citySpots) {
        if (isSimulatingNavigation) citySpots.filter { it.isSelected } else citySpots
    }

    var activeNavigationLegIndex by remember { mutableStateOf(0) }
    var navigationProgressFraction by remember { mutableStateOf(0f) }

    // Central state for live transit vehicles shared with the map overlay
    var liveTransitVehiclesGlobal by remember(selectedCity) {
        mutableStateOf(TransportApiEngine.simulateLiveVehicles(selectedCity))
    }

    var focusedTransitVehicle by remember(selectedCity) { mutableStateOf<com.example.domain.LiveTransitVehicle?>(null) }

    // Live update loop for transit vehicle positions (refreshes every 6 seconds)
    LaunchedEffect(selectedCity) {
        while (true) {
            kotlinx.coroutines.delay(6000)
            liveTransitVehiclesGlobal = TransportApiEngine.simulateLiveVehicles(selectedCity)
        }
    }

    var useRealDeviceGps by rememberSaveable { mutableStateOf(false) }
    var mapActionMode by rememberSaveable { mutableStateOf("ADD_SPOT") } // "ADD_SPOT" or "SET_GPS"
    val context = LocalContext.current
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val testingLogs by viewModel.testingLogs.collectAsState()

    // --- ADAPTIVE CORE PERMISSIONS HUB STATES & CHECKERS ---
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var showPermissionsSetupDialog by rememberSaveable { mutableStateOf(false) }

    val checkAndRefreshPermissions = remember(context) {
        {
            hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }

    LaunchedEffect(Unit) {
        checkAndRefreshPermissions()
        // If any critical permissions are missing, show the setup wizard dialog on startup to guide the user!
        if (!hasLocationPermission || !hasNotificationPermission || !hasOverlayPermission) {
            showPermissionsSetupDialog = true
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkAndRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        checkAndRefreshPermissions()
        scope.launch {
            val msg = if (granted) {
                if (isEnglishLanguage) "🔔 Notifications granted! Real-time alerts are now fully active." else "🔔 Notificări acordate! Alertele în timp real sunt acum complet active."
            } else {
                if (isEnglishLanguage) "❌ Notifications denied. You won't receive live transit alarms." else "❌ Notificări refuzate. Nu vei primi alerte sonore de tranzit."
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    val requestOverlayPermissionAction = {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (isEnglishLanguage) "Floating overlay is already active!" else "Sistemul permite deja widget-urile suprapuse!"
                )
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        checkAndRefreshPermissions()
        if (fineGranted || coarseGranted) {
            useRealDeviceGps = true
            scope.launch {
                val msg = if (isEnglishLanguage) "🛰️ Real GPS Active! Maps will track your physical location via satellite." else "🛰️ GPS Real Activ! Harta îți va urmări poziția fizică din satelit."
                snackbarHostState.showSnackbar(msg)
            }
        } else {
            useRealDeviceGps = false
            scope.launch {
                val msg = if (isEnglishLanguage) "❌ GPS permission denied. Satellite Mode requires permissions." else "❌ Permisiunea GPS refuzată. Modul Satelit are nevoie de permisiuni."
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    DisposableEffect(useRealDeviceGps) {
        var listener: android.location.LocationListener? = null
        if (useRealDeviceGps) {
            try {
                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasFine || hasCoarse) {
                    val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        LocationManager.GPS_PROVIDER
                    } else {
                        LocationManager.NETWORK_PROVIDER
                    }
                    
                    val lastKnown = locationManager.getLastKnownLocation(provider)
                    if (lastKnown != null) {
                        userGpsLocation = Pair(lastKnown.latitude, lastKnown.longitude)
                    }
                    
                    listener = object : android.location.LocationListener {
                        override fun onLocationChanged(location: android.location.Location) {
                            userGpsLocation = Pair(location.latitude, location.longitude)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    
                    locationManager.requestLocationUpdates(
                        provider,
                        1000L,
                        0.5f,
                        listener
                    )
                }
            } catch (e: SecurityException) {
                // ignore
            } catch (e: Exception) {
                // ignore
            }
        }
        onDispose {
            listener?.let {
                locationManager.removeUpdates(it)
            }
        }
    }

    // Dynamic crawler loop tracking user progress across route segments in real-time
    LaunchedEffect(isSimulatingNavigation, activeNavigationLegIndex, simulationSpeedMultiplier) {
        if (isSimulatingNavigation) {
            navigationProgressFraction = 0f
            
            // Prefetch mapped snapped points once per activeNavigationLegIndex to avoid query fatigue in high-frequency loop
            val route = optimizedJourney
            var snappedPoints: List<Pair<Double, Double>>? = null
            if (route != null && activeNavigationLegIndex < route.legs.size) {
                val fullSpotsList = listOfNotNull(customStartSpot ?: TransitNetwork.getStartSpot(selectedCity)) + route.orderedSpots
                val fromSpot = fullSpotsList.getOrNull(activeNavigationLegIndex)
                val toSpot = fullSpotsList.getOrNull(activeNavigationLegIndex + 1)
                if (fromSpot != null && toSpot != null) {
                    snappedPoints = com.example.domain.OSMRoutingService.getStreetSnappedRoute(
                        fromSpot.latitude, fromSpot.longitude,
                        toSpot.latitude, toSpot.longitude
                    )
                }
            }

            while (navigationProgressFraction < 1f) {
                kotlinx.coroutines.delay(100)
                val stepSize = 0.015f * simulationSpeedMultiplier
                navigationProgressFraction = (navigationProgressFraction + stepSize).coerceAtMost(1f)
                
                // Dynamically update coordinates along the active leg route lines
                snappedPoints?.let { points ->
                    userGpsLocation = com.example.domain.OSMRoutingService.interpolateRouteProgress(
                        points,
                        navigationProgressFraction
                    )
                }
            }
        }
    }

    val currentColorScheme = getAppColorScheme(appSkin)
    val currentTypography = getAppTypography(appSkin)

    MaterialTheme(
        colorScheme = currentColorScheme,
        typography = currentTypography
    ) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        topBar = {
            MainTopAppBar(
                isEnglishLanguage = isEnglishLanguage,
                selectedCity = selectedCity,
                isDashboardVisible = isDashboardVisible,
                onToggleDashboard = { isDashboardVisible = !isDashboardVisible },
                appSkin = appSkin,
                onAppSkinToggle = {
                    appSkin = if (appSkin == "MODERN") "VINTAGE_RPG" else "MODERN"
                    if (appSkin == "VINTAGE_RPG") {
                        mapColorSchemeStyle = "Vintage Parchment"
                    } else {
                        mapColorSchemeStyle = "Slate Neon"
                    }
                },
                onSettingsClick = { showSettingsDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Responsive split: Map is compact (weight 0.9f), Dashboard has more height (weight 1.5f) to prevent clipping.
            MapContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f),
                isDashboardVisible = isDashboardVisible,
                selectedCity = selectedCity,
                mapSpots = mapSpots,
                customStartSpot = customStartSpot,
                optimizedJourney = optimizedJourney,
                mapActionMode = mapActionMode,
                userGpsLocation = userGpsLocation,
                onUserGpsLocationChange = { userGpsLocation = it },
                isEnglishLanguage = isEnglishLanguage,
                scope = scope,
                snackbarHostState = snackbarHostState,
                onMapTapForSpot = { lat, lng ->
                    clickedLat = lat
                    clickedLng = lng
                    newSpotName = ""
                    newSpotDesc = ""
                    newSpotDuration = "60"
                    showAddSpotDialog = true
                },
                onToggleSpotSelection = { id, selected -> viewModel.toggleSpotSelection(id, selected) },
                onUpdateCustomStartSpot = { lat, lng, name -> viewModel.updateCustomStartSpot(lat, lng, name) },
                onDeleteSpot = { id -> viewModel.deleteSpot(id) },
                onToggleDashboard = { isDashboardVisible = !isDashboardVisible },
                isSimulatingNavigation = isSimulatingNavigation,
                onSimulatingNavigationChange = { isSimulatingNavigation = it },
                activeNavigationLegIndex = activeNavigationLegIndex,
                onActiveNavigationLegIndexChange = { activeNavigationLegIndex = it },
                navigationProgressFraction = navigationProgressFraction,
                onNavigationProgressFractionChange = { navigationProgressFraction = it },
                isSoundAlertEnabled = isSoundAlertEnabled,
                isStationsVisible = isStationsVisible,
                isTransitLinesVisible = isTransitLinesVisible,
                mapColorSchemeStyle = mapColorSchemeStyle,
                isHighResolutionMap = isHighResolutionMap,
                liveTransitVehiclesGlobal = liveTransitVehiclesGlobal,
                focusedTransitVehicle = focusedTransitVehicle,
                onFocusedVehicleChange = { focusedTransitVehicle = it }
            )
            // Dashboard Card with Tabs or Collapsed summary bar
            if (isDashboardVisible) {
                DashboardPanel(
                    isDashboardExpanded = isDashboardExpanded,
                    onToggleDashboardExpanded = { isDashboardExpanded = it },
                    onHideDashboardCompletely = { isDashboardVisible = false },
                    appSkin = appSkin,
                    selectedDashboardTab = selectedDashboardTab,
                    onTabSelected = { selectedDashboardTab = it },
                    isEnglishLanguage = isEnglishLanguage,
                    selectedCity = selectedCity,
                    mapSpots = mapSpots,
                    customStartSpot = customStartSpot,
                    onToggleSpot = { id, selected -> viewModel.toggleSpotSelection(id, selected) },
                    onDeselectAllSpots = { viewModel.deselectAllSpots() },
                    onDeleteSpot = { id -> viewModel.deleteSpot(id) },
                    onClearCustomSpots = { viewModel.clearAllCustomCurrentCity() },
                    onSetStartSpot = { spot ->
                        viewModel.updateCustomStartSpot(spot.latitude, spot.longitude, spot.name)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isEnglishLanguage) "Starting point changed to: ${translateSpotName(spot.name, true)}"
                                else "Punct de pornire schimbat la: ${spot.name}"
                            )
                        }
                    },
                    currentWeather = currentWeather,
                    selectedWeatherPreset = selectedWeatherPreset,
                    onSelectWeatherPreset = { preset -> viewModel.setWeatherPreset(preset) },
                    routingPreference = routingPreference,
                    onPrefChange = { viewModel.updateRoutingPreference(it) },
                    enabledTransitModes = enabledTransitModes,
                    onTransitModeToggle = { viewModel.toggleTransitMode(it) },
                    optimizedJourney = optimizedJourney,
                    isSimulatingNavigation = isSimulatingNavigation,
                    activeNavigationLegIndex = activeNavigationLegIndex,
                    onStartSimulationClick = {
                        if (userGpsLocation == null) {
                            val seedCoords = when (selectedCity) {
                                "București" -> Pair(44.4411, 26.0973)
                                "Brașov" -> Pair(45.6540, 25.6030)
                                "Câmpina" -> Pair(45.1265, 25.7345)
                                "Cluj-Napoca" -> Pair(46.7684, 23.5862)
                                else -> {
                                    val start = com.example.domain.TransitNetwork.getStartSpot(selectedCity)
                                    Pair(start.latitude, start.longitude)
                                }
                            }
                            userGpsLocation = seedCoords
                        }
                        isSimulatingNavigation = true
                        activeNavigationLegIndex = 0
                        navigationProgressFraction = 0f
                        scope.launch {
                            snackbarHostState.showSnackbar(if (isEnglishLanguage) "🚀 Route guidance started! Follow the map." else "🚀 Ghidajul traseului a pornit! Urmărește harta.")
                        }
                    },
                    onStopSimulationClick = {
                        isSimulatingNavigation = false
                        activeNavigationLegIndex = 0
                        navigationProgressFraction = 0f
                        scope.launch {
                            snackbarHostState.showSnackbar(if (isEnglishLanguage) "Guidance stopped." else "Ghidaj oprit.")
                        }
                    },
                    liveTransitVehiclesGlobal = liveTransitVehiclesGlobal,
                    onVehicleClick = { vehicle ->
                        focusedTransitVehicle = vehicle
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isEnglishLanguage) "⚡ Radar Lock: Auto-focusing on Transit line ${vehicle.lineName}"
                                else "⚡ Radar localizat: Focalizare automată pe linia ${vehicle.lineName}"
                            )
                        }
                    },
                    testingLogs = testingLogs,
                    useRealDeviceGps = useRealDeviceGps,
                    onToggleRealGps = { enabled ->
                        if (enabled) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                             )
                        } else {
                            useRealDeviceGps = false
                        }
                    },
                    mapActionMode = mapActionMode,
                    onSetMapActionMode = { mapActionMode = it },
                    onSaveLog = { placeName, note, observerName ->
                        viewModel.addTestingLog(placeName, note, observerName)
                    },
                    onDeleteLog = { id -> viewModel.deleteTestingLog(id) },
                    onClearLogs = { viewModel.clearAllTestingLogsCurrentCity() },
                    aiRecommendation = aiRecommendation,
                    isAiLoading = isAiLoading,
                    onCallAi = { viewModel.askGeminiForItinerary(isEnglishLanguage) },
                    aiProvider = aiProvider,
                    savedItineraries = savedItineraries,
                    onDeleteSavedItinerary = { id -> viewModel.deleteSavedItinerary(id) },
                    onClearAllSavedItineraries = { viewModel.clearAllSavedItineraries() },
                    onSaveItineraryClick = {
                        optimizedJourney?.let {
                            viewModel.saveCurrentItinerary(it)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isEnglishLanguage) "The route has been saved offline!" 
                                    else "Traseul a fost salvat offline!"
                                )
                            }
                        }
                    },
                    onShareItineraryClick = {
                        optimizedJourney?.let {
                            val names = it.orderedSpots.joinToString(" ➔ ") { s -> translateSpotName(s.name, isEnglishLanguage) }
                            val startName = translateSpotName(customStartSpot?.name ?: com.example.domain.TransitNetwork.getStartSpot(selectedCity).name, isEnglishLanguage)
                            val details = (if (isEnglishLanguage) "Start: $startName\n" else "Pornire: $startName\n") +
                                    (if (isEnglishLanguage) "Route: $names\n\n" else "Traseu: $names\n\n") +
                                    (if (isEnglishLanguage) "Transport stages:\n" else "Etape de transport:\n") +
                                    it.legs.joinToString("\n") { leg ->
                                        "- ${translateDirections(leg.directions, isEnglishLanguage)} (${leg.durationMinutes} min)"
                                    }
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, 
                                    if (isEnglishLanguage) "Here is my itinerary for $selectedCity:\n\n$details" 
                                    else "Aici este itinerarul meu pentru $selectedCity:\n\n$details"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                        }
                    },
                    isLandmarkSearchInProgress = isLandmarkSearchInProgress,
                    landmarkSearchError = landmarkSearchError,
                    onAutoSearchLandmark = { query ->
                        viewModel.autoSearchAndAddLandmark(query, isEnglishLanguage) { addedName ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isEnglishLanguage) "Successfully discovered and injected '$addedName'!"
                                    else "S-a descoperit și adăugat cu succes obiectivul '$addedName'!"
                                )
                            }
                        }
                    },
                    onClearLandmarkSearchError = { viewModel.clearLandmarkSearchError() },
                    hasLocationPermission = hasLocationPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    onManagePermissionsClick = { showPermissionsSetupDialog = true }
                )
            }
        }
    }

    // Add Custom Spot Dialog
    AddCustomSpotDialog(
        show = showAddSpotDialog,
        onDismiss = { showAddSpotDialog = false },
        clickedLat = clickedLat,
        clickedLng = clickedLng,
        spotName = newSpotName,
        onSpotNameChange = { newSpotName = it },
        spotDesc = newSpotDesc,
        onSpotDescChange = { newSpotDesc = it },
        spotDuration = newSpotDuration,
        onSpotDurationChange = { newSpotDuration = it },
        onSave = {
            val durationVal = newSpotDuration.toIntOrNull() ?: 60
            if (newSpotName.isNotBlank()) {
                viewModel.addCustomTouristSpot(
                    name = newSpotName.trim(),
                    description = newSpotDesc.trim(),
                    lat = clickedLat,
                    lng = clickedLng,
                    duration = durationVal
                )
                showAddSpotDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isEnglishLanguage) "Added: $newSpotName" else "S-a adăugat: $newSpotName"
                    )
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isEnglishLanguage) "Attraction name is required!" else "Numele atracției este obligatoriu!"
                    )
                }
            }
        },
        isEnglish = isEnglishLanguage,
        appSkin = appSkin
    )

    SettingsDialog(
        show = showSettingsDialog,
        onDismiss = { showSettingsDialog = false },
        selectedCity = selectedCity,
        onCityChange = { viewModel.selectCity(it) },
        availableCities = availableCities,
        isIndexingCity = isIndexingCity,
        indexingStatus = indexingStatus,
        onIndexCity = { cityName ->
            viewModel.indexNewCity(cityName, isEnglishLanguage) { resultMsg ->
                scope.launch {
                    snackbarHostState.showSnackbar(resultMsg)
                }
            }
        },
        isStationsVisible = isStationsVisible,
        onStationsToggle = { isStationsVisible = it },
        isTransitLinesVisible = isTransitLinesVisible,
        onTransitLinesToggle = { isTransitLinesVisible = it },
        simulationSpeed = simulationSpeedMultiplier,
        onSimulationSpeedChange = { simulationSpeedMultiplier = it },
        mapColorScheme = mapColorSchemeStyle,
        onMapColorSchemeChange = { mapColorSchemeStyle = it },
        isHighResolution = isHighResolutionMap,
        onHighResolutionToggle = { isHighResolutionMap = it },
        isSoundEnabled = isSoundAlertEnabled,
        onSoundEnabledToggle = { isSoundAlertEnabled = it },
        isEnglish = isEnglishLanguage,
        onLanguageToggle = { isEnglishLanguage = it },
        openaiApiKey = openaiApiKey,
        onOpenaiApiKeyChange = { viewModel.updateOpenAiApiKey(it) },
        openaiBaseUrl = openaiBaseUrl,
        onOpenaiBaseUrlChange = { viewModel.updateOpenAiBaseUrl(it) },
        openaiModel = openaiModel,
        onOpenaiModelChange = { viewModel.updateOpenAiModel(it) },
        aiProvider = aiProvider,
        onAiProviderChange = { viewModel.updateAiProvider(it) },
        openrouterApiKey = openrouterApiKey,
        onOpenrouterApiKeyChange = { viewModel.updateOpenRouterApiKey(it) },
        openrouterModel = openrouterModel,
        onOpenrouterModelChange = { viewModel.updateOpenRouterModel(it) },
        appSkin = appSkin,
        onAppSkinChange = { appSkin = it },
        onManagePermissionsClick = { showPermissionsSetupDialog = true },
        onFactoryReset = {
            viewModel.factoryResetData()
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (isEnglishLanguage) "All user data has been cleared!" else "Toate datele au fost șterse!"
                )
            }
        }
    )

    PermissionsDialog(
        show = showPermissionsSetupDialog,
        onDismiss = { showPermissionsSetupDialog = false },
        hasLocationPermission = hasLocationPermission,
        hasNotificationPermission = hasNotificationPermission,
        hasOverlayPermission = hasOverlayPermission,
        onRequestLocation = {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onRequestNotification = {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isEnglishLanguage) "Notifications already allowed by system!" else "Sistemul permite deja notificările!"
                    )
                }
            }
        },
        onRequestOverlay = {
            requestOverlayPermissionAction()
        },
        isEnglish = isEnglishLanguage,
        appSkin = appSkin
    )
    }
}
