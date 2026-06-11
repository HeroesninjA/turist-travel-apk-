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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
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
            while (navigationProgressFraction < 1f) {
                kotlinx.coroutines.delay(100)
                val stepSize = 0.015f * simulationSpeedMultiplier
                navigationProgressFraction = (navigationProgressFraction + stepSize).coerceAtMost(1f)
                
                // Dynamically update coordinates along the active leg route lines
                val route = optimizedJourney
                if (route != null && activeNavigationLegIndex < route.legs.size) {
                    val fullSpotsList = listOfNotNull(customStartSpot ?: TransitNetwork.getStartSpot(selectedCity)) + route.orderedSpots
                    val fromSpot = fullSpotsList.getOrNull(activeNavigationLegIndex)
                    val toSpot = fullSpotsList.getOrNull(activeNavigationLegIndex + 1)
                    if (fromSpot != null && toSpot != null) {
                        val snappedPoints = com.example.domain.OSMRoutingService.getStreetSnappedRoute(
                            fromSpot.latitude, fromSpot.longitude,
                            toSpot.latitude, toSpot.longitude
                        )
                        userGpsLocation = com.example.domain.OSMRoutingService.interpolateRouteProgress(
                            snappedPoints,
                            navigationProgressFraction
                        )
                    }
                }
            }
        }
    }

    val currentColorScheme = if (appSkin == "VINTAGE_RPG") {
        darkColorScheme(
            primary = Color(0xFFC5A059),
            onPrimary = Color(0xFF1E100A),
            secondary = Color(0xFF8C6D45),
            onSecondary = Color(0xFFFCF3D7),
            tertiary = Color(0xFF26A69A),
            background = Color(0xFF1C110A),
            surface = Color(0xFF261910),
            onBackground = Color(0xFFEADBBE),
            onSurface = Color(0xFFEADBBE),
            surfaceVariant = Color(0xFF382317),
            onSurfaceVariant = Color(0xFFC4AD85),
            outline = Color(0xFF6E4D36),
            outlineVariant = Color(0xFF4C3322)
        )
    } else {
        MaterialTheme.colorScheme
    }

    val currentTypography = if (appSkin == "VINTAGE_RPG") {
        Typography(
            titleLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                letterSpacing = 0.5.sp
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp
            ),
            titleSmall = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                letterSpacing = 0.25.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                letterSpacing = 0.25.sp
            ),
            bodySmall = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 0.25.sp
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            ),
            labelMedium = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            labelSmall = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        )
    } else {
        MaterialTheme.typography
    }

    MaterialTheme(
        colorScheme = currentColorScheme,
        typography = currentTypography
    ) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "BusTour Optimizer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnglishLanguage) "Smart Transit Planner" else "Planificator de Tranzit Inteligent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clean active city chip badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                                .clickable { showSettingsDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = translateCityName(selectedCity, isEnglishLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Toggle Dashboard Panel button
                        FilledIconButton(
                            onClick = { isDashboardVisible = !isDashboardVisible },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isDashboardVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (isDashboardVisible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("toggle_dashboard_button")
                        ) {
                            Icon(
                                imageVector = if (isDashboardVisible) Icons.Default.KeyboardArrowDown else Icons.Default.Menu,
                                contentDescription = if (isDashboardVisible) "Ascunde panou" else "Afișează panou",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Alternative RPG Style/Skin Toggle button
                        FilledIconButton(
                            onClick = { 
                                appSkin = if (appSkin == "MODERN") "VINTAGE_RPG" else "MODERN"
                                if (appSkin == "VINTAGE_RPG") {
                                    mapColorSchemeStyle = "Vintage Parchment"
                                } else {
                                    mapColorSchemeStyle = "Slate Neon"
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1E100A) else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("theme_skin_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Alternative Interface",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Settings Panel button
                        FilledIconButton(
                            onClick = { showSettingsDialog = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("settings_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Setări Aplicație",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
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
            val mapCornerRadius = if (isDashboardVisible) 24.dp else 0.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .clip(RoundedCornerShape(bottomStart = mapCornerRadius, bottomEnd = mapCornerRadius))
            ) {
                val stations = TransitNetwork.getStationsForCity(selectedCity)
                val lines = TransitNetwork.getLinesForCity(selectedCity)

                InteractiveMap(
                    city = selectedCity,
                    spots = if (isSimulatingNavigation) citySpots.filter { it.isSelected } else citySpots,
                    customStartSpot = customStartSpot,
                    stations = stations,
                    lines = lines,
                    journey = optimizedJourney,
                    onMapTap = { lat, lng ->
                        if (mapActionMode == "SET_GPS") {
                            userGpsLocation = Pair(lat, lng)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isEnglishLanguage) "🎯 Simulated GPS location moved on map!"
                                    else "🎯 Locația GPS simulată a fost mutată pe hartă!"
                                )
                            }
                        } else {
                            clickedLat = lat
                            clickedLng = lng
                            newSpotName = ""
                            newSpotDesc = ""
                            newSpotDuration = "60"
                            showAddSpotDialog = true
                        }
                    },
                    onSpotClick = { spot ->
                        viewModel.toggleSpotSelection(spot.id, !spot.isSelected)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isEnglishLanguage) "Selection changed for: ${translateSpotName(spot.name, true)}"
                                else "S-a schimbat selectarea pentru: ${spot.name}"
                            )
                        }
                    },
                    onSetStartSpot = { spot ->
                        viewModel.updateCustomStartSpot(spot.latitude, spot.longitude, spot.name)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isEnglishLanguage) "Starting point changed to: ${translateSpotName(spot.name, true)}"
                                else "Punct de pornire schimbat la: ${spot.name}"
                            )
                        }
                    },
                    onDeleteSpot = { id ->
                        viewModel.deleteSpot(id)
                    },
                    isDashboardVisible = isDashboardVisible,
                    onToggleDashboard = { isDashboardVisible = !isDashboardVisible },
                    userGpsLocation = userGpsLocation,
                    onEnableGpsClicked = {
                        if (userGpsLocation == null) {
                            val seedCoords = when (selectedCity) {
                                "București" -> Pair(44.4411, 26.0973) // near Ateneul Român
                                "Brașov" -> Pair(45.6540, 25.6030)
                                "Câmpina" -> Pair(45.1265, 25.7345)
                                else -> Pair(46.7684, 23.5862) // Memorandului Cluj
                            }
                            userGpsLocation = seedCoords
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isEnglishLanguage) "🛰️ GPS Signal Active on Map!"
                                    else "🛰️ Semnal GPS Activ pe Hartă!"
                                )
                            }
                        } else {
                            userGpsLocation = null
                            isSimulatingNavigation = false
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isEnglishLanguage) "GPS Disconnected."
                                    else "Gps Deconectat."
                                )
                            }
                        }
                    },
                    onUseGpsAsStart = { lat, lng ->
                        viewModel.updateCustomStartSpot(lat, lng, "Locație GPS Ta")
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
                            activeNavigationLegIndex = legsSize 
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isEnglishLanguage) "🎉 Congratulations! You have successfully completed the entire trip!"
                                    else "🎉 Felicitări! Ai finalizat cu succes întregul traseu!"
                                )
                            }
                        } else {
                            activeNavigationLegIndex++
                            navigationProgressFraction = 0f
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isEnglishLanguage) "Stage completed! Moving to the next direction..."
                                    else "Etapă finalizată! Se trece la următoarea direcție..."
                                )
                            }
                        }
                    },
                    onStopSimulationClick = {
                        isSimulatingNavigation = false
                        activeNavigationLegIndex = 0
                        navigationProgressFraction = 0f
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
                    onFocusedVehicleDismiss = { focusedTransitVehicle = null },
                    onVehicleClick = { vehicle ->
                        focusedTransitVehicle = vehicle
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

            // Dashboard Card with Tabs or Collapsed summary bar
            if (isDashboardVisible) {
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
                            // Mini summary & Actions Row
                            OptimizedSummaryBar(
                                city = selectedCity,
                                journey = optimizedJourney,
                                onSaveClick = {
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
                                onShareClick = {
                                    optimizedJourney?.let {
                                        val names = it.orderedSpots.joinToString(" ➔ ") { s -> translateSpotName(s.name, isEnglishLanguage) }
                                        val startName = translateSpotName(customStartSpot?.name ?: com.example.domain.TransitNetwork.getStartSpot(selectedCity).name, isEnglishLanguage)
                                        val details = (if (isEnglishLanguage) "Start: $startName\n" else "Pornire: $startName\n") +
                                                (if (isEnglishLanguage) "Route: $names\n\n" else "Traseu: $names\n\n") +
                                                (if (isEnglishLanguage) "Transport stages:\n" else "Etape de transport:\n") +
                                                it.legs.joinToString("\n") { leg ->
                                                    "- ${translateSpotName(leg.directions, isEnglishLanguage)} (${leg.durationMinutes} min)"
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
                                isCollapsed = false,
                                onToggleDashboard = { isDashboardExpanded = false },
                                isSimulatingNavigation = isSimulatingNavigation,
                                onStartSimulationClick = {
                                    if (userGpsLocation == null) {
                                        val seedCoords = when (selectedCity) {
                                            "București" -> Pair(44.4411, 26.0973)
                                            "Brașov" -> Pair(45.6540, 25.6030)
                                            "Câmpina" -> Pair(45.1265, 25.7345)
                                            else -> Pair(46.7684, 23.5862)
                                        }
                                        userGpsLocation = seedCoords
                                    }
                                    isSimulatingNavigation = true
                                    activeNavigationLegIndex = 0
                                    navigationProgressFraction = 0f
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isEnglishLanguage) "🚀 Route guidance started! Follow the map." 
                                            else "🚀 Ghidajul traseului a pornit! Urmărește harta."
                                        )
                                    }
                                },
                                onStopSimulationClick = {
                                    isSimulatingNavigation = false
                                    activeNavigationLegIndex = 0
                                    navigationProgressFraction = 0f
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isEnglishLanguage) "Guidance stopped." 
                                            else "Ghidaj oprit."
                                        )
                                    }
                                },
                                onHideCompletely = { isDashboardVisible = false },
                                isEnglish = isEnglishLanguage
                            )

                            // Premium Adaptive Tab Row
                            TabRow(
                                selectedTabIndex = selectedDashboardTab,
                                containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                indicator = {}, // Hide default ugly thick line indicator
                                divider = {}, // Hide standard bottom divider
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .run {
                                        if (appSkin == "VINTAGE_RPG") {
                                            this.border(
                                                width = 1.dp,
                                                color = Color(0xFF6E4D36),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        } else {
                                            this.border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                            ) {
                                // Tab 0: Attractions Checklist
                                val isTab0Selected = selectedDashboardTab == 0
                                Tab(
                                    selected = isTab0Selected,
                                    onClick = { selectedDashboardTab = 0 },
                                    modifier = Modifier
                                        .testTag("tab_spots_checklist")
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isTab0Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            } else Color.Transparent
                                        )
                                        .run {
                                            if (isTab0Selected && appSkin == "VINTAGE_RPG") {
                                                this.border(1.dp, Color(0xFFC5A059), RoundedCornerShape(8.dp))
                                            } else if (isTab0Selected) {
                                                this.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            } else this
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.List,
                                            contentDescription = null,
                                            tint = if (isTab0Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "Attractions" else "Atracții",
                                            fontSize = 11.sp,
                                            fontWeight = if (isTab0Selected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isTab0Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Tab 1: Itinerary Timeline
                                val isTab1Selected = selectedDashboardTab == 1
                                Tab(
                                    selected = isTab1Selected,
                                    onClick = { selectedDashboardTab = 1 },
                                    modifier = Modifier
                                        .testTag("tab_itinerary_timeline")
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isTab1Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            } else Color.Transparent
                                        )
                                        .run {
                                            if (isTab1Selected && appSkin == "VINTAGE_RPG") {
                                                this.border(1.dp, Color(0xFFC5A059), RoundedCornerShape(8.dp))
                                            } else if (isTab1Selected) {
                                                this.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            } else this
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = if (isTab1Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "Itinerary" else "Itinerar",
                                            fontSize = 11.sp,
                                            fontWeight = if (isTab1Selected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isTab1Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Tab 2: Testing logs
                                val isTab2Selected = selectedDashboardTab == 2
                                Tab(
                                    selected = isTab2Selected,
                                    onClick = { selectedDashboardTab = 2 },
                                    modifier = Modifier
                                        .testTag("tab_live_testing")
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isTab2Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFF1E2822) else Color(0xFF10B981).copy(alpha = 0.15f)
                                            } else Color.Transparent
                                        )
                                        .run {
                                            if (isTab2Selected && appSkin == "VINTAGE_RPG") {
                                                this.border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                            } else if (isTab2Selected) {
                                                this.border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            } else this
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isTab2Selected) {
                                                Color(0xFF10B981)
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "Testing" else "Testare",
                                            fontSize = 11.sp,
                                            fontWeight = if (isTab2Selected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isTab2Selected) {
                                                Color(0xFF10B981)
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Tab 3: AI Guide Recommendation
                                val isTab3Selected = selectedDashboardTab == 3
                                Tab(
                                    selected = isTab3Selected,
                                    onClick = { selectedDashboardTab = 3 },
                                    modifier = Modifier
                                        .testTag("tab_ai_guide")
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isTab3Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else Color(0xFFEAB308).copy(alpha = 0.15f)
                                            } else Color.Transparent
                                        )
                                        .run {
                                            if (isTab3Selected && appSkin == "VINTAGE_RPG") {
                                                this.border(1.dp, Color(0xFFEAB308), RoundedCornerShape(8.dp))
                                            } else if (isTab3Selected) {
                                                this.border(1.dp, Color(0xFFEAB308).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            } else this
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (isTab3Selected) {
                                                Color(0xFFEAB308)
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "AI Guide" else "Ghid AI",
                                            fontSize = 11.sp,
                                            fontWeight = if (isTab3Selected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isTab3Selected) {
                                                Color(0xFFEAB308)
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Tab 4: Saved Local Offline Itineraries
                                val isTab4Selected = selectedDashboardTab == 4
                                Tab(
                                    selected = isTab4Selected,
                                    onClick = { selectedDashboardTab = 4 },
                                    modifier = Modifier
                                        .testTag("tab_saved_itineraries")
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isTab4Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            } else Color.Transparent
                                        )
                                        .run {
                                            if (isTab4Selected && appSkin == "VINTAGE_RPG") {
                                                this.border(1.dp, Color(0xFFC5A059), RoundedCornerShape(8.dp))
                                            } else if (isTab4Selected) {
                                                this.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            } else this
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = if (isTab4Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "Saved" else "Salvate",
                                            fontSize = 11.sp,
                                            fontWeight = if (isTab4Selected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isTab4Selected) {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                                            } else {
                                                if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = if (appSkin == "VINTAGE_RPG") Color(0xFF4C3322) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
                                            spots = if (isSimulatingNavigation) citySpots.filter { it.isSelected } else citySpots,
                                            customStartSpot = customStartSpot,
                                            onToggle = { id, selected -> viewModel.toggleSpotSelection(id, selected) },
                                            onDeselectAll = { viewModel.deselectAllSpots() },
                                            onDelete = { id -> viewModel.deleteSpot(id) },
                                            onClearCustom = { viewModel.clearAllCustomCurrentCity() },
                                            onSetStartClick = { spot ->
                                                viewModel.updateCustomStartSpot(spot.latitude, spot.longitude, spot.name)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (isEnglishLanguage) "Starting point changed to: ${spot.name}"
                                                        else "Punct de pornire schimbat la: ${spot.name}"
                                                    )
                                                }
                                            },
                                            isEnglish = isEnglishLanguage,
                                            weather = currentWeather,
                                            selectedPreset = selectedWeatherPreset,
                                            onSelectPreset = { preset -> viewModel.setWeatherPreset(preset) },
                                            routingPreference = routingPreference,
                                            onPrefChange = { viewModel.updateRoutingPreference(it) },
                                            enabledTransitModes = enabledTransitModes,
                                            onTransitModeToggle = { viewModel.toggleTransitMode(it) },
                                            appSkin = appSkin
                                        )
                                        1 -> TimelineTab(isEnglish = isEnglishLanguage,
                                            journey = optimizedJourney,
                                            startLabel = customStartSpot?.let { it.translate(isEnglishLanguage).name } ?: TransitNetwork.getStartSpot(selectedCity).translate(isEnglishLanguage).name,
                                            startHour = "09:00",
                                            city = selectedCity,
                                            isSimulatingNavigation = isSimulatingNavigation,
                                            activeLegIndex = activeNavigationLegIndex,
                                            onStartSimulationClick = {
                                                if (userGpsLocation == null) {
                                                    // Auto-enable GPS to initial position when starting routing simulation
                                                    val seedCoords = when (selectedCity) {
                                                        "București" -> Pair(44.4411, 26.0973)
                                                        "Brașov" -> Pair(45.6540, 25.6030)
                                                        "Câmpina" -> Pair(45.1265, 25.7345)
                                                        else -> Pair(46.7684, 23.5862)
                                                    }
                                                    userGpsLocation = seedCoords
                                                }
                                                isSimulatingNavigation = true
                                                activeNavigationLegIndex = 0
                                                navigationProgressFraction = 0f
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (isEnglishLanguage) "🚀 Route guidance started! Follow the map." 
                                                        else "🚀 Ghidajul traseului a pornit! Urmărește harta."
                                                    )
                                                }
                                            },
                                            onStopSimulationClick = {
                                                isSimulatingNavigation = false
                                                activeNavigationLegIndex = 0
                                                navigationProgressFraction = 0f
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (isEnglishLanguage) "Guidance stopped." 
                                                        else "Ghidaj oprit."
                                                    )
                                                }
                                            },
                                            liveTransitVehicles = liveTransitVehiclesGlobal,
                                            onVehicleClick = { vehicle ->
                                                focusedTransitVehicle = vehicle
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (isEnglishLanguage) "⚡ Radar Lock: Auto-focusing on Transit line ${vehicle.lineName}"
                                                        else "⚡ Radar localizat: Focalizare automată pe linia ${vehicle.lineName}"
                                                    )
                                                }
                                            },
                                            appSkin = appSkin
                                        )
                                        2 -> LiveTestingTab(
                                            isEnglish = isEnglishLanguage,
                                            city = selectedCity,
                                            journey = optimizedJourney,
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
                                            onSetMapActionMode = { mode -> mapActionMode = mode },
                                            onSaveLog = { placeName, note, observerName ->
                                                viewModel.addTestingLog(placeName, note, observerName)
                                            },
                                            onDeleteLog = { id ->
                                                viewModel.deleteTestingLog(id)
                                            },
                                            onClearLogs = {
                                                viewModel.clearAllTestingLogsCurrentCity()
                                            },
                                            appSkin = appSkin
                                        )
                                        3 -> AiGuideTab(
                                            recommendationText = aiRecommendation,
                                            isLoading = isAiLoading,
                                            onCallAi = { viewModel.askGeminiForItinerary(isEnglishLanguage) },
                                            isEnglish = isEnglishLanguage,
                                            aiProvider = aiProvider,
                                            appSkin = appSkin
                                        )
                                        4 -> SavedItinerariesTab(
                                            isEnglish = isEnglishLanguage,
                                            itineraries = savedItineraries,
                                            onDelete = { id -> viewModel.deleteSavedItinerary(id) },
                                            onClearAll = { viewModel.clearAllSavedItineraries() },
                                            appSkin = appSkin
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Collapsed State: Show only the compact OptimizedSummaryBar to keep route stats and Save flow accessible, while maximizing Map space
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
                            onSaveClick = {
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
                            onShareClick = {
                                optimizedJourney?.let {
                                    val names = it.orderedSpots.joinToString(" ➔ ") { s -> translateSpotName(s.name, isEnglishLanguage) }
                                    val startName = translateSpotName(customStartSpot?.name ?: com.example.domain.TransitNetwork.getStartSpot(selectedCity).name, isEnglishLanguage)
                                    val details = (if (isEnglishLanguage) "Start: $startName\n" else "Pornire: $startName\n") +
                                            (if (isEnglishLanguage) "Route: $names\n\n" else "Traseu: $names\n\n") +
                                            (if (isEnglishLanguage) "Transport stages:\n" else "Etape de transport:\n") +
                                            it.legs.joinToString("\n") { leg ->
                                                "- ${translateSpotName(leg.directions, isEnglishLanguage)} (${leg.durationMinutes} min)"
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
                            isCollapsed = true,
                            onToggleDashboard = { isDashboardExpanded = true },
                            onHideCompletely = { isDashboardVisible = false },
                            isSimulatingNavigation = isSimulatingNavigation,
                            isEnglish = isEnglishLanguage,
                            onStartSimulationClick = {
                                if (userGpsLocation == null) {
                                    val seedCoords = when (selectedCity) {
                                        "București" -> Pair(44.4411, 26.0973)
                                        "Brașov" -> Pair(45.6540, 25.6030)
                                        "Câmpina" -> Pair(45.1265, 25.7345)
                                        else -> Pair(46.7684, 23.5862)
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
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Custom Spot Dialog
    if (showAddSpotDialog) {
        AlertDialog(
            onDismissRequest = { showAddSpotDialog = false },
            modifier = if (appSkin == "VINTAGE_RPG") {
                Modifier.border(
                    BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45)))),
                    shape = RoundedCornerShape(24.dp)
                )
            } else Modifier,
            containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(if (isEnglishLanguage) "Add New Tourist Spot" else "Adaugă Punct Turistic Nou")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEnglishLanguage) 
                            "Enter the details of the selected point directly on the map (Lat: ${String.format("%.4f", clickedLat)}, Lng: ${String.format("%.4f", clickedLng)}):"
                            else "Introdu detaliile punctului selectat direct pe hartă (Lat: ${String.format("%.4f", clickedLat)}, Lng: ${String.format("%.4f", clickedLng)}):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newSpotName,
                        onValueChange = { newSpotName = it },
                        label = { Text(if (isEnglishLanguage) "Attraction Name *" else "Nume Atracție *") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_spot_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newSpotDesc,
                        onValueChange = { newSpotDesc = it },
                        label = { Text(if (isEnglishLanguage) "Short Description" else "Descriere Scurtă") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = newSpotDuration,
                        onValueChange = { newSpotDuration = it },
                        label = { Text(if (isEnglishLanguage) "Estimated visit time (minutes)" else "Timp estimat în vizită (minute)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Quick duration chip helpers
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("30", "60", "90", "120").forEach { d ->
                            FilterChip(
                                selected = newSpotDuration == d,
                                onClick = { newSpotDuration = d },
                                label = { Text("$d min") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
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
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text(if (isEnglishLanguage) "Save" else "Salvează")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSpotDialog = false }) {
                    Text(if (isEnglishLanguage) "Cancel" else "Anulează")
                }
            }
        )
    }

    SettingsDialog(
        show = showSettingsDialog,
        onDismiss = { showSettingsDialog = false },
        selectedCity = selectedCity,
        onCityChange = { viewModel.selectCity(it) },
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
        aiProvider = aiProvider,
        onAiProviderChange = { viewModel.updateAiProvider(it) },
        openrouterApiKey = openrouterApiKey,
        onOpenrouterApiKeyChange = { viewModel.updateOpenRouterApiKey(it) },
        openrouterModel = openrouterModel,
        onOpenrouterModelChange = { viewModel.updateOpenRouterModel(it) },
        appSkin = appSkin,
        onAppSkinChange = { appSkin = it },
        onFactoryReset = {
            viewModel.factoryResetData()
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (isEnglishLanguage) "All user data has been cleared!" else "Toate datele au fost șterse!"
                )
            }
        }
    )
    }
}

// --- Dynamic Optimization UI Localization Helpers ---

fun translateSpotName(name: String, isEnglish: Boolean): String {
    if (!isEnglish) return name
    return when (name) {
        // Bucharest starting point
        "Gara de Nord (Hotel/Start)" -> "North Stream Station (Hotel/Start)"
        "Gara de Nord" -> "North Station"
        "Palatul Parlamentului" -> "Palace of the Parliament"
        "Centrul Vechi" -> "Old Town"
        "Ateneul Român" -> "Romanian Athenaeum"
        "Parcul Herăstrău (Mihai I)" -> "Herăstrău Park (Mihai I)"
        "Arcul de Triumf" -> "Triumph Arch"
        "Muzeul Național al Satului" -> "National Village Museum"
        "Parcul Cișmigiu" -> "Cișmigiu Gardens"
        "Muzeul Național Grigore Antipa" -> "Grigore Antipa Natural History Museum"
        "Cărturești Carusel" -> "Cărturești Carusel Bookstore"
        "Biserica Stavropoleos" -> "Stavropoleos Church"
        "Muzeul de Artă al României" -> "National Museum of Art"
        "Grădina Botanică Dimitrie Brândză" -> "Botanical Garden"
        "Parcul Carol I" -> "Carol I Park"
        "Palatul Primăverii" -> "Spring Palace"
        "Piața Revoluției" -> "Revolution Square"
        "Hanul lui Manuc" -> "Manuc's Inn"
        "Muzeul Național de Istorie a României" -> "National History Museum"
        "Palatul Cotroceni" -> "Cotroceni Palace"
        "Parcul Tineretului" -> "Tineretului Park"
        "Catedrala Mântuirii Neamului" -> "People's Salvation Cathedral"
        "Parcul Drumul Taberei" -> "Drumul Taberei Park"
        "Palatul Mogoșoaia" -> "Mogoșoaia Palace"
        "Muzeul de Artă Contemporană (MNAC)" -> "National Museum of Contemporary Art"
        "Piața Universității" -> "University Square"
        "Opera Națională București" -> "Bucharest National Opera"
        "Parcul Alexandru Ioan Cuza (IOR)" -> "Alexandru Ioan Cuza (IOR) Park"
        "Therme București" -> "Therme Bucharest"
        "Palatul Șuțu (Muzeul Bucureștiului)" -> "Sutu Palace (Bucharest History Museum)"
        "Teatrul Național I.L. Caragiale" -> "I.L. Caragiale National Theatre"
        "Observatorul Astronomic Vasile Urseanu" -> "Vasile Urseanu Astronomical Observatory"
        "Parcul Kiseleff" -> "Kiseleff Park"
        "Palatul Cantacuzino" -> "Cantacuzino Palace"
        "Pasajul Macca-Vilacrosse" -> "Macca-Vilacrosse Passage"
        "Palatul CEC" -> "CEC Palace"
        "Muzeul Colecțiilor de Artă" -> "Museum of Art Collections"
        "Palatul Justiției" -> "Palace of Justice"
        "Palatul Patriarhiei" -> "Patriarchal Palace"
        "Muzeul Militar Național" -> "National Military Museum"
        "Arena Națională" -> "National Arena"
        "Muzeul Național al Literaturii Române" -> "National Museum of Romanian Literature"
        "Palatul Kretzulescu" -> "Kretzulescu Palace"
        "Biserica Kretzulescu" -> "Kretzulescu Church"
        "Muzeul Tehnic Dimitrie Leonida" -> "Dimitrie Leonida Technical Museum"
        "Palatul Primăriei Capitalei" -> "Bucharest City Hall Palace"
        "Turnul de Artă (Pantelimon)" -> "Art Tower (Pantelimon)"
        "Muzeul Național al Hărților și Cărții Vechi" -> "National Museum of Maps and Old Books"
        "Parcul Plumbuita" -> "Plumbuita Park"
        "Parcul Circului de Stat" -> "State Circus Park"
        "Palatul Ghica Tei" -> "Ghica Tei Palace"
        "Cimitirul Bellu" -> "Bellu Cemetery"

        // Cluj starting point & presets
        "Gara Cluj-Napoca (Hotel/Start)" -> "Cluj-Napoca Railway Station (Hotel/Start)"
        "Gara Cluj-Napoca" -> "Cluj-Napoca Railway Station"
        "Grădina Botanică Alexandru Borza" -> "Botanical Garden"
        "Parcul Central Simion Bărnuțiu" -> "Central Park Simion Bărnuțiu"
        "Piața Unirii & Biserica Sf. Mihail" -> "Union Square & St. Michael Church"
        "Catedrala Mitropolitană & Piața Avram Iancu" -> "Metropolitan Cathedral & Avram Iancu Square"
        "Dealul Cetățuia" -> "Cetățuia Hill"
        "Muzeul de Artă & Palatul Bánffy" -> "Art Museum & Bánffy Palace"
        "Parcul Romulus Vuia (Etnografic)" -> "Romulus Vuia Ethnographic Park"
        "Bastionul Croitorilor" -> "Tailors' Tower"
        "Parcul Iulius (Lacul Gheorgheni)" -> "Iulius Park (Gheorgheni Lake)"
        "Piața Muzeului" -> "Museum Square"
        "Pădurea Hoia-Baciu" -> "Hoia-Baciu Haunted Forest"
        "The Office Cluj & Podul de Fier" -> "The Office Cluj & The Iron Bridge"
        "Teatrul Național și Opera Română" -> "National Theatre and Romanian Opera"
        "Turnul Pompierilor" -> "Firemen's Tower"
        "Parcul Cetățuia Buburuza" -> "Buburuza Cetățuia Park"
        "Muzeul Național de Istorie a Transilvaniei" -> "National History Museum of Transylvania"
        "Biserica Reformată de pe ulița Lupilor" -> "Reformed Church on Wolves' Street"
        "Cluj Arena" -> "Cluj Arena Stadium"
        "BT Arena (Sala Polivalentă)" -> "BT Arena Multi-purpose Hall"
        "Parcul Rozelor" -> "Rose Park"
        "Biserica Calvaria (Mănăștur)" -> "Calvaria Church (Mănăștur)"
        "Catedrala Greco-Catolică Sf. Iosif (Cipariu)" -> "St. Joseph Greek-Catholic Cathedral"
        "Observatorul Astronomic" -> "Astronomical Observatory"
        "Campusul Istoric USAMV" -> "Historical USAMV Campus"
        "Cetatea Fetei Florești" -> "Cetatea Fetei Florești (Hiking Spot)"

        // Brasov starting point & presets
        "Gara Brașov (Hotel/Start)" -> "Brașov Railway Station (Hotel/Start)"
        "Gara Brașov" -> "Brașov Railway Station"
        "Biserica Neagră" -> "The Black Church"
        "Piața Sfatului" -> "Council Square"
        "Telecabina Tâmpa" -> "Tâmpa Cable Car"
        "Turnul Alb" -> "The White Tower"
        "Poarta Șchei" -> "Șchei Gate"
        "Turnul Negru" -> "The Black Tower"
        "Bastionul Țesătorilor" -> "Weavers' Bastion"
        "Strada Sforii" -> "Rope Street"
        "Prima Școală Românească" -> "First Romanian School"
        "Poarta Ecaterinei" -> "Catherine's Gate"
        "Parcul Central Nicolae Titulescu" -> "Nicolae Titulescu Central Park"
        "Bastionul Graft" -> "Graft Bastion"
        "Muzeul de Artă Brașov" -> "Brașov Art Museum"
        "Pietrele lui Solomon" -> "Solomon's Rocks"
        "Cetățuia de pe Strajă" -> "The Citadel on Strajă Hill"
        "Turnul Măcelarilor" -> "Butchers' Tower"
        "Bastionul Cojocarilor" -> "Furriers' Bastion"
        "Sinagoga Neologă din Brașov" -> "Neologue Synagogue of Brașov"
        "Casa Sfatului (Muzeul de Istorie)" -> "Council House (History Museum)"
        "Biserica Sfântul Nicolae" -> "St. Nicholas Church"
        "Promenada de sub Tâmpa" -> "Promenade under Tâmpa Mountain"
        "Turnul Lemnarilor" -> "Woodworkers' Tower"
        "Cartierul Istoric Șchei" -> "Șchei Historical Quarter"
        "Grădina Zoologică Brașov (Noua)" -> "Brașov Zoo (Noua)"
        "Lacul Noua & Parc Agrement" -> "Noua Lake & Leisure Park"

        // Campina starting point & presets
        "Gara Câmpina (Hotel/Start)" -> "Câmpina Railway Station (Hotel/Start)"
        "Gara Câmpina" -> "Câmpina Railway Station"
        "Castelul \"Iulia Hasdeu\"" -> "Iulia Hasdeu Castle"
        "Muzeul Memorial \"Nicolae Grigorescu\"" -> "Nicolae Grigorescu Memorial Museum"
        "Casa de Cultură Câmpina" -> "Câmpina House of Culture"
        "Primăria Câmpina" -> "Câmpina City Hall"
        "Biserica de Lemn \"Adormirea Maicii Domnului\"" -> "Wood Church of Dormition"
        "Capela în stil Gotic \"Hernea\"" -> "Hernea Gothic Chapel"
        "Bulevardul Culturii (Aleea cu Platani)" -> "Culture Boulevard (Planet Avenue)"
        "Fântâna cu Cireși & Dealul Muscel" -> "Cherry Well & Muscel Hill"
        "Lacul Câmpina (Lacul Peștelui)" -> "Campina Lake (Fish Lake)"

        else -> name
    }
}

fun translateSpotDescription(description: String, isEnglish: Boolean): String {
    if (!isEnglish) return description
    return when (description) {
        "Punctul de pornire al călătoriei." -> "The starting point of your custom tour."
        "Una dintre cele mai mari clădiri administrative din lume." -> "One of the largest administrative buildings in the world."
        "Inima istorică a Bucureștiului, plină de viață și clădiri de epocă." -> "The historical heart of Bucharest, bursting with life and old buildings."
        "O bijuterie arhitecturală de importanță istorică națională." -> "An architectural treasure of national historical importance."
        "Un parc uriaș, liniștit, situat în jurul unui lac superb." -> "A massive, peaceful park arranged around a pristine lake."
        "Monumentul care celebrează victoria României în Primul Război Mondial." -> "The monument celebrating Romania's victory in World War I."
        "O incursiune în viața rurală tradițională românească în aer liber." -> "An open-air museum exploration of traditional Romanian village life."
        "Cea mai veche grădină publică din București, un lac romantic și alei liniștite." -> "The oldest public garden in Bucharest, featuring a romantic lake and quiet alleys."
        "Expoziții interactive de zoologie, biodiversitate și fosile de dinozaur." -> "Interactive exhibitions of zoology, biodiversity, and dinosaur fossils."
        "Una dintre cele mai spectaculoase librării din lume, situată în Centrul Vechi." -> "One of the most spectacular bookshops in the world, in the Old Town."
        "O capodoperă a stilului brâncovenesc, faimoasă pentru curtea sa interioară." -> "A masterpiece of Brâncovenesc style, famous for its interior courtyard."
        "Fostul Palat Regal găzduiește colecții remarcabile de artă românească." -> "The former Royal Palace, hosting remarkable collections of Romanian art."
        "Oaze de verdeață, sere exotice tropicale și mii de specii de plante în Cotroceni." -> "A green oasis with exotic tropical greenhouses and thousands of plant species in Cotroceni."
        "Parc istoric frumos cu Mausoleul impunător și fântâni elegante." -> "Beautiful historical park with a majestic Mausoleum and elegant fountains."

        // Campina descriptions
        "Un castel încărcat de mister, construit de savantul Bogdan Petriceicu Hasdeu în memoria fiicei sale geniale, Iulia." -> "A mysterious castle built by B.P. Hasdeu in memory of his genius daughter, Iulia."
        "Casa memorială unde marele pictor Nicolae Grigorescu a trăit și creat în ultimii săi ani de viață." -> "The memorial house where the great painter Nicolae Grigorescu lived and created in his twilight years."
        "Centrul cultural principal al orașului, gazdă a numeroase spectacole, expoziții și evenimente locale." -> "The city's main cultural center, hosting numerous performances, exhibitions, and local events."
        "Clădirea administrativă centrală din Câmpina, situată pe pitorescul Bulevard al Culturii." -> "The central administrative building of Campina, located on the scenic Culture Boulevard."
        "O pitorească biserică istorică de lemn datând de la 1714, formată dintr-un singur trunchi de stejar." -> "A picturesque historic wooden church dating to 1714, carved out of a single massive oak trunk."
        "O capelă gotică misterioasă, monument de arhitectură, ridicată în memoria pionierului petrolului, Dumitru Hernea." -> "A mysterious Gothic chapel built in memory of petroleum pioneer Dumitru Hernea."
        "Zonă de promenadă superbă și relaxantă mărginită de platani uriași, considerat unul dintre cele mai ozonate locuri." -> "A gorgeous promenade flanked by towering sycamores, considered one of the highest ozone spots in Europe."
        "Cel mai înalt punct de belvedere din zonă, oferind panorame uluitoare spre valea Doftanei și dealurile prahovene." -> "Highest local scenic outlook, offering stunning panoramas of Doftana Valley and the Prahova hills."
        "Un lac natural liniștit ideal pentru plimbări relaxante pe mal, pescuit și evadare în mijlocul naturii locale." -> "A serene natural lake perfect for calm boardwalk strolls, recreation and local nature getaways."
        "Fostul palat luxos de protocol al soților Nicolae și Elena Ceaușescu." -> "The former luxurious private residence of Nicolae and Elena Ceaușescu."
        "Piața istorică centrală cu Memorialul Renașterii și clădiri celebre." -> "The central historical square with the Memorial of Rebirth."
        "Cel mai vechi han funcțional din Europa, oferind o ambianță tradițională excelentă." -> "The oldest active inn in Europe, offering an excellent traditional Romanian vibe."
        "Exponate arheologice și istorice inestimabile, incluzând Tezaurul istoric național." -> "Invaluable archaeological and historical exhibits, including the national Treasury."
        "Reședința oficială a Președintelui și un muzeu istoric de o rară frumusețe." -> "The official Presidential residence and a historic museum of rare beauty."
        "Un parc modern imens cu lac de agrement, piste și un ambient extrem de relaxant." -> "A massive modern park with a recreational lake, tracks, and relaxing ambiance."
        "Cea mai mare catedrală ortodoxă din lume, o structură arhitecturală colosală." -> "The largest Orthodox Cathedral in the world, a colossal architectural masterwork."
        "Cunoscut și ca Parcul Moghioroș, revitalizat cu poduri cochete și sere moderne." -> "Also known as Moghioroș Park, revitalized with chic bridges and modern greenhouses."
        "O clădire istorică în stil brâncovenesc deosebit, situată în exteriorul orașului." -> "A beautiful brâncovenesc-style castle situated just outside the city."
        "Situat în aripa din spate a Palatului Parlamentului, cu expoziții avangardiste." -> "Located in the back wing of the Palace of the Parliament, featuring avant-garde exhibitions."
        "Kilometrul zero al democrației bucureștene, încadrat de clădiri universitare emblematice." -> "The landmark of Romanian democracy, surrounded by iconic university buildings."
        "Clădire istorică neoclasică, faimos centru de cultura pentru spectacole lirice și balet." -> "A historic neoclassical building, a famous cultural venue for opera and ballet."

        // Cluj descriptions
        "Oază magnifică de verdeață ce adăpostește plante rare și o grădină japoneză." -> "A magnificent green oasis sheltering rare plants and a Japanese garden."
        "Parcul istoric central cu un lac superb de plimbări cu barca și Casino." -> "The historic central park with a boating lake and the Casino building."
        "Piața istorică principală delimitată de monumentala catedrală gotică." -> "The main historical square dominated by the monumental Gothic Cathedral."
        "Catedrală ortodoxă impunătoare și piațetă cu fântâni arteziene animate." -> "An imposing Orthodox Cathedral and public square with animated artesian fountains."
        "O panoramă spectaculaosă a întregului oraș, ideală la apus de soare." -> "A spectacular panoramic view of the entire city, ideal at sunset."
        "O panoramă spectaculoasă a întregului oraș, ideală la apus de soare." -> "A spectacular panoramic view of the entire city, ideal at sunset."
        "Palat baroc splendid ce găzduiește colecții naționale valoroase de artă." -> "A splendid baroque palace hosting valuable national art collections."
        "Primul muzeu în aer liber din România cu gospodării tradiționale transilvănene." -> "Romania's first open-air museum featuring historic Transylvanian homesteads."
        "Unul dintre puțele turnuri de apărare care s-au păstrat intacte din vechea cetate." -> "One of the few defensive towers preserved intact from the old citadel."
        "Zonă modernă de recreere în jurul lacului, plină de spații verzi și pontoane." -> "A modern lakeside recreation area filled with green spaces and boardwalks."
        "Cea mai veche piață din Cluj-Napoca, flancată de Biserica Franciscană." -> "The oldest square in Cluj-Napoca, flanked by the elegant Franciscan Church."
        "Pădurea faimoasă la nivel mondial pentru peisajele sale misterioase și legende." -> "The world-famous forest known for its mysterious landscapes and legends."
        "O zonă modernă vibrantă, îmbinând arhitectura de birouri cu malul Someșului." -> "A vibrant modern area combining office development with the Someș riverfront."
        "Clădire neobarocă superbă destinată spectacolelor lirice și teatrale." -> "A superb neo-baroque building designed for opera and theatrical performances."
        "Turn istoric reabilitat recent cu o platformă panoramică superbă." -> "A newly rehabilitated historical tower with a magnificent panoramic platform."
        "Zonă adiacentă cetățuii cu alei umbroase, spații de joacă și belvedere retras." -> "Area near Cetățuia with shaded alleys, playgrounds, and cozy viewpoints."
        "Colecții arheologice valoroase despre istoria antică, romană și medievală a Transilvaniei." -> "Valuable archaeological collections about the ancient, Roman, and medieval history of Transylvania."
        "O clădire monument istoric gotic de tip sală, una dintre cele mai vaste din Europa de Est." -> "A historic monumental Gothic hall church, one of the largest in Eastern Europe."
        "Cel mai modern stadion multifuncțional din inima Transilvaniei, cu o arhitectură high-tech." -> "The most modern multi-use stadium in the heart of Transylvania, featuring high-tech architecture."
        "Cea mai mare sală polivalentă din România, găzduiește concerte mari și evenimente sportive." -> "The largest multi-purpose arena in Romania, hosting major concerts and sports events."
        "Parc renumit pentru sutele de soiuri de trandafiri și faleza liniștită pe malul Someșului." -> "A park famous for hundreds of rose varieties and a peaceful Someș riverfront path."
        "O veche mănăstire benedictină fortificată, fiind una dintre cele mai bătrâne biserici din Cluj." -> "An ancient fortified Benedictine monastery, one of the oldest standing churches in Cluj."
        "Catedrală monumentală cu design modern magnific, aflată în Piața Cipariu." -> "A monumental cathedral with a magnificent modern design, located in Cipariu Square."
        "Situat în campusul USAMV, ideal pentru explorarea stelelor și activități educaționale." -> "Located on the USAMV campus, ideal for star-gazing and educational outreach."
        "Grădini, livezi istorice și oază verde extinsă în una dintre faimoasele universități clujene." -> "Gardens, orchards, and an expansive green sanctuary inside USAMV University."
        "Loc istoric plin de mister situat pe deal, înconjurat de pădure, ideal pentru drumeții." -> "A mysterious historical site situated on a hill, surrounded by forest, ideal for hiking."

        // Brasov descriptions
        "Cea mai mare biserică gotică din sud-estul Europei." -> "The largest Gothic church in Southeastern Europe."
        "Piața istorică principală din Brașov, plină de farmec și cafenele." -> "The main historical square in Brașov, filled with charm and cozy cafes."
        "Telecabina spre muntele Tâmpa cu panoramă excelentă a orașului." -> "The cable car climbing Tâmpa Mountain, offering scenic panoramic city views."
        "Turn istoric de apărare oferind o vedere spectaculoasă la înălțime." -> "A historic defense tower providing spectacular aerial views from the hill."
        "Poartă barocă superbă ce duce spre vechiul cartier românesc." -> "A beautiful baroque gate leading into the historic Romanian quarter Schei."
        "Turn de strajă din secolul al XV-lea cu vedere panoramică spre Biserica Neagră." -> "A 15th-century defense tower with panoramic views looking towards the Black Church."
        "Unul dintre cele mai bine conservate bastioane medievale, adăpostind o machetă rară." -> "One of the best-preserved medieval bastions, hosting a rare scale model."
        "Una dintre cele mai înguste străzi din Europa, un reper fotografic iconic." -> "One of the narrowest alleys in Europe, a truly iconic photo landmark."
        "Situată în Șchei, locul unde s-au tipărit primele cărți în limba română." -> "Located in Șchei, the historic cradle where the first Romanian books were printed."
        "Singura poartă medievală de acces în cetate păstrată în forma sa originală." -> "The only medieval city entry gate surviving fully in its original form."
        "Un park mare și liniștit în centrul orașului cu alei largi și fântâni." -> "A large and tranquil park in the city center with wide paths and fountains."
        "Bastion fortificat pitoresc deasupra pârâului Graft, legat de Turnul Alb." -> "A picturesque fortified bastion over Graft creek, linked physically to the White Tower."
        "Expoziție de pictură și sculptură românească valoroasă, aproape de primărie." -> "A rich collection of valuable Romanian paintings and sculptures near City Hall."
        "Zonă naturală de chei spectaculoase cu spații verzi pentru recreere." -> "A spectacular natural gorge area with lush green spaces for picnics."
        "Fortăreață istorică pe dealul Strajă, monument istoric de importanță națională." -> "A hilltop citadel on Strajă hill, a national historical heritage site."
        "Turn vechi de apărare din secolul al XV-lea, parte integrantă din fortificații." -> "An ancient 15th-century defense tower, integral to the old town walls."
        "Bastion istoric ridicat pe latura de sud a cetății sub muntele Tâmpa." -> "A historic bastion erected on the southern walls right under Tâmpa Mountain."
        "O clădire religioasă splendidă în stil bizantin, cu detalii decorative fermecătoare." -> "A splendid Religious monument designed in Byzantine style with charming decor."
        "Simbolul central al orașului Brașov, fostul sediu administrativ medieval." -> "The central symbol of Brașov, formerly the medieval administrative headquarters."
        "O biserică orthodoxă impunătoare din Șchei, fondată în secolul al XIII-lea." -> "An imposing Orthodox Church in Șchei, with foundations from the 13th century."
        "Aleea pietonală umbroasă Tiberiu Brediceanu, perfectă pentru plimbări relaxante pe sub pădure." -> "The shaded pedestrian alley under Tâmpa, perfect for relaxing forest walks."
        "Turnul Lemnarilor" -> "The Woodworkers' Tower"
        "Turnul Lemnarilor, găzduiește expoziții de sculptură și ateliere de artă." -> "The Woodworkers' Tower, showcasing woodcarving exhibits and art workshops."
        "Turn istoric restaurat cochet, găzduiește expoziții de sculptură și ateliere de artă." -> "A beautifully restored historic tower, hosting sculpture exhibits and art workshops."
        "Explorare pe străduțele vechi și întortocheate, inima spiritului românesc brașovean." -> "An exploration of winding old cobblestone streets, the cradle of Romanian heritage."
        "Una dintre cele mai moderne grădini zoologice din țară, amplasată în pădurea Noua." -> "One of the country's most modern zoos, nestling beautifully in Noua Forest."
        "Zonă superbă de relaxare cu bărci, pontoane, terenuri de sport și un aer minunat de munte." -> "A stellar lakeside park with rental boats, sports grounds, and pure mountain air."
        else -> description
    }
}

fun TouristSpot.translate(isEnglish: Boolean): TouristSpot {
    return this.copy(
        name = translateSpotName(this.name, isEnglish),
        description = translateSpotDescription(this.description, isEnglish)
    )
}

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
        else -> "📍"
    }
}

fun translateDirections(directions: String, isEnglish: Boolean): String {
    if (!isEnglish) return directions
    return directions
        .replace("Plimbare pe jos", "Walk")
        .replace("plimbare pe jos", "walk on foot")
        .replace("plimbare", "walk")
        .replace("pe jos", "on foot")
        .replace("până la", "until")
        .replace("către", "towards")
        .replace("de la", "from")
        .replace("la", "at")
        .replace("stânga", "left")
        .replace("dreapta", "right")
        .replace("mergi", "go")
        .replace("Mergi", "Go")
        .replace("Ia autobuzul", "Take bus")
        .replace("Ia troleibuzul", "Take trolley")
        .replace("Ia tramvaiul", "Take tram")
        .replace("Ieși din", "Exit from")
        .replace("stația", "station")
        .replace("Stația", "Station")
        .replace("direcția", "direction")
        .replace("coboară la", "get off at")
        .replace("Coboară la", "Get off at")
        .replace("spre", "towards")
        .replace("Spre", "Towards")
        .replace("apoi mergi în jur de", "then walk about")
        .replace("până la intrare", "to the entrance")
        .replace("metrou", "metro")
        .replace("Metrou", "Metro")
        .replace("linii", "lines")
        .replace("linia", "line")
        .replace("Autobuz", "Bus")
        .replace("autobuz", "bus")
        .replace("Tramvai", "Tram")
        .replace("tramvai", "tram")
        .replace("Troleibuz", "Trolleybus")
        .replace("troleibuz", "trolleybus")
        .replace("Timp estimat", "Estimated time")
        .replace("minute", "minutes")
        .replace("minut", "minute")
}

fun translateAlertText(text: String, isEnglish: Boolean): String {
    if (!isEnglish) return text
    return when (text) {
        "Lucrări pe Linia 41" -> "Works on Line 41"
        "Tramvaiele liniei 41 circulă deviat temporar din cauza lucrărilor de reabilitare a carosabilului din zona Pasajului Grant. Autobuzele navetă 641 preiau fluxul de călători." ->
            "Tram 41 is temporarily rerouted due to roadworks near Grant Passage. Shuttle buses 641 are covering the passenger flow."
        "Modificare Traseu Eveniment" -> "Event Route Changes"
        "În weekendul curent, liniile din zona Calea Victoriei sunt deviate temporar pentru evenimentul Străzi Deschise." ->
            "This weekend, bus lines around Calea Victoriei are temporarily rerouted for the 'Străzi Deschise' event."
        "Zilele Clujului - Trasee Deviate" -> "Zilele Clujului - Rerouted Lines"
        "Liniile de troleibuz 6, 7 și 25 vor avea traseul scurtat până în Piața Cipariu în intervalul orar 18:00 - 23:00 pentru concertele din Piața Unirii." ->
            "Trolleybus lines 6, 7, and 25 will be shortened to Cipariu Square from 18:00 to 23:00 due to concerts in Piața Unirii."
        "Suplimentare Linie 20 Poiana" -> "Poiana Line 20 Supplemental Buses"
        "A fost suplimentat numărul de autobuze de pe linia 20 Livada Poștei - Poiana Brașov datorită afluxului masiv de turiști sosiți în weekend." ->
            "Additional buses have been scheduled on Line 20 (Livada Poștei - Poiana Brașov) due to massive tourist influx over the weekend."
        else -> text
    }
}

fun getTranslatedCostExplanation(city: String, isEnglish: Boolean, defaultExplanation: String): String {
    if (!isEnglish) return defaultExplanation
    return when (city) {
        "București" -> "1.30 € + VAT (~6.5 Lei) for a 90-minute urban travel on any metropolitan STB line."
        "Brașov" -> "4.00 Lei for a 60-minute travel on the urban RATBV network."
        "Câmpina" -> "3.00 Lei for a single travel on the urban Eliado Câmpina network."
        else -> "0.65 € + VAT (~3.2 Lei) for a 30-minute urban travel on any CTP Cluj line."
    }
}

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
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpotsChecklistTab(
    city: String,
    spots: List<TouristSpot>,
    customStartSpot: TouristSpot?,
    onToggle: (Long, Boolean) -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: (Long) -> Unit,
    onClearCustom: () -> Unit,
    onSetStartClick: (TouristSpot) -> Unit,
    isEnglish: Boolean = false,
    weather: WeatherInfo? = null,
    selectedPreset: String = "DEFAULT",
    onSelectPreset: (String) -> Unit = {},
    routingPreference: String = "DEFAULT",
    onPrefChange: (String) -> Unit = {},
    enabledTransitModes: Set<LegType> = emptySet(),
    onTransitModeToggle: (LegType) -> Unit = {},
    appSkin: String = "MODERN"
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterOnlySelected by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }
    var isSearchPanelExpanded by remember { mutableStateOf(false) }
    var isRouteSettingsExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val selectedCount = spots.count { it.isSelected }
    var isBannerDismissed by remember(selectedCount) { mutableStateOf(false) }

    val translatedSpots = remember(spots, isEnglish) {
        spots.map { it.translate(isEnglish) }
    }

    val translatedCustomStartSpot = remember(customStartSpot, isEnglish) {
        customStartSpot?.translate(isEnglish)
    }

    val filteredSpots = translatedSpots.filter { spot ->
        val matchesSearch = spot.name.contains(searchQuery, ignoreCase = true) ||
                spot.description.contains(searchQuery, ignoreCase = true)
        val matchesSelected = !filterOnlySelected || spot.isSelected
        matchesSearch && matchesSelected
    }

    var showOptimizationDialog by remember { mutableStateOf(false) }

    if (showOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { showOptimizationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                    Text(
                        text = if (isEnglish) "Route Optimization AI" else "Optimizare Inteligentă Rută",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = if (isEnglish) {
                        "Our algorithm automatically re-orders all selected sights starting from your START point based on their geographic distance (TSP nearest-neighbor heuristic). It also automatically determines whether walking or using the transit network (bus/metro) is more efficient for each leg, minimizing travel time and transit fare for you!"
                    } else {
                        "Algoritmul reordonează automat toate atracțiile selectate pornind de la punctul tău de START în funcție de distanța lor geografică (rezolvarea problemei comis-voiajorului - TSP). De asemenea, calculează automat dacă mersul pe jos sau utilizarea rețelei de tranzit este mai eficientă pentru fiecare segment! Totul pentru a-ți minimiza timpul de deplasare și cheltuielile!"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showOptimizationDialog = false }) {
                    Text(if (isEnglish) "Got it" else "Am înțeles")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnglish) "Sights in $city" else "Atracții în $city",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEnglish) {
                        "$selectedCount selected"
                    } else {
                        "$selectedCount selectate"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small circular toggle button to show/expand route optimization & config
                FilledIconButton(
                    onClick = { isRouteSettingsExpanded = !isRouteSettingsExpanded },
                    modifier = Modifier.size(36.dp).testTag("toggle_route_settings_panel_btn"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRouteSettingsExpanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isRouteSettingsExpanded) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isRouteSettingsExpanded) Icons.Default.Close else Icons.Default.Settings,
                        contentDescription = if (isEnglish) "Route Options" else "Opțiuni Traseu",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Small circular toggle button to show/expand search functionality and help algorithms
                FilledIconButton(
                    onClick = { isSearchPanelExpanded = !isSearchPanelExpanded },
                    modifier = Modifier.size(36.dp).testTag("toggle_search_panel_btn"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isSearchPanelExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isSearchPanelExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isSearchPanelExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isEnglish) "Search and filters" else "Caută și filtre",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expanded Search & Filter panel, Route Settings Panel, and Utility buttons are now placed directly
        // inside the LazyColumn to prevent nested scroll conflicts and ensure infinite smooth scrolling on all device sizes.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .testTag("spots_checklist_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Search Panel (If Expanded)
            if (isSearchPanelExpanded) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                        colors = CardDefaults.cardColors(
                            containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Search text field inside the expanded card
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(if (isEnglish) "Search attractions..." else "Caută atracții...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("attr_search_tf"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(24.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                            )

                            // Filter chips row inside the expanded card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // A. "Doar selectate" (Filter Selected) Chip
                                FilterChip(
                                    selected = filterOnlySelected,
                                    onClick = { filterOnlySelected = !filterOnlySelected },
                                    label = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (filterOnlySelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check, 
                                                    contentDescription = null, 
                                                    modifier = Modifier.size(14.dp), 
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(if (isEnglish) "Selected Only" else "Doar selectate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    modifier = Modifier.height(32.dp).testTag("filter_selected_chip"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )

                                // B. "Info Algoritm" Chip
                                SuggestionChip(
                                    onClick = { showOptimizationDialog = true },
                                    label = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info, 
                                                contentDescription = null, 
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(if (isEnglish) "Info Alg" else "Info Algoritm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                )
                            }
                        }
                    }
                }
            }

            // 2. Route Settings Panel (If Expanded)
            if (isRouteSettingsExpanded) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                        colors = CardDefaults.cardColors(
                            containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isEnglish) "Route Preference & Modes" else "Configurare Traseu & Optimizare",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Preference Section
                            Text(
                                text = if (isEnglish) "Routing Priority" else "Criteriu de Optimizare",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val preferences = listOf(
                                "DEFAULT" to (if (isEnglish) "Default" else "Implicit/Echilibrat"),
                                "LESS_WALKING" to (if (isEnglish) "Less Walking" else "Mai puțin pe jos"),
                                "FASTER" to (if (isEnglish) "Faster" else "Cel mai rapid"),
                                "CHEAPER" to (if (isEnglish) "Cheaper" else "Cel mai ieftin"),
                                "MORE_TRANSFERS" to (if (isEnglish) "More Transfers" else "Cu mai multe schimburi")
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                preferences.forEach { (prefKey, label) ->
                                    val isSelected = routingPreference == prefKey
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onPrefChange(prefKey) },
                                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp).testTag("routing_pref_${prefKey}")
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Allowed Transit Modes Section
                            Text(
                                text = if (isEnglish) "Preferred Transportation Modes" else "Mijloace de Transport Permise",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (isEnglish) "Excluding a mode automatically recalculates alternatives ('remove methods')" else "Dezactivarea calculează automat rute alternative și elimină metode",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            val transitModes = listOf(
                                Triple(LegType.WALK, "🚶 Pe jos", "Walk"),
                                Triple(LegType.BUS, "🚌 Autobuz", "Bus"),
                                Triple(LegType.METRO, "🚇 Metrou", "Metro"),
                                Triple(LegType.TROLLEY, "🚎 Troleibuz", "Trolley"),
                                Triple(LegType.TRAIN, "🚆 Tren", "Train"),
                                Triple(LegType.TAXI, "🚖 Taxi/Uber", "Taxi/Uber")
                            )
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    transitModes.take(3).forEach { (mode, nameRo, nameEn) ->
                                        val isEnabled = enabledTransitModes.contains(mode)
                                        FilterChip(
                                            selected = isEnabled,
                                            onClick = { onTransitModeToggle(mode) },
                                            leadingIcon = {
                                                if (isEnabled) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(10.dp))
                                                }
                                            },
                                            label = { Text(if (isEnglish) nameEn else nameRo, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                                selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(28.dp).testTag("transit_mode_${mode.name}")
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    transitModes.drop(3).forEach { (mode, nameRo, nameEn) ->
                                        val isEnabled = enabledTransitModes.contains(mode)
                                        FilterChip(
                                            selected = isEnabled,
                                            onClick = { onTransitModeToggle(mode) },
                                            leadingIcon = {
                                                if (isEnabled) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(10.dp))
                                                }
                                            },
                                            label = { Text(if (isEnglish) nameEn else nameRo, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                                selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(28.dp).testTag("transit_mode_${mode.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Always-Visible action and utility chips (Deselect, Optimize, Delete Custom)
            val customCount = spots.count { it.isCustom }
            if (selectedCount > 0 || customCount > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. "Optimizează" (Route Optimization Helper) Card Button
                        if (selectedCount > 0) {
                            val premiumGreen = Color(0xFF10B981)
                            SuggestionChip(
                                onClick = {
                                    scope.launch {
                                        isOptimizing = true
                                        kotlinx.coroutines.delay(800)
                                        isOptimizing = false
                                        android.widget.Toast.makeText(
                                            context,
                                            if (isEnglish) "Route successfully optimized!" else "Traseul a fost optimizat cu succes!",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isOptimizing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                color = Color.White,
                                                strokeWidth = 1.5.dp
                                            )
                                        } else {
                                            Text("⚡", fontSize = 11.sp)
                                        }
                                        Text(
                                            text = if (isOptimizing) {
                                                (if (isEnglish) "Optimizing..." else "Se optimizează...")
                                            } else {
                                                (if (isEnglish) "Optimize Route" else "Optimizează Traseu")
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                },
                                modifier = Modifier.height(34.dp).testTag("optimize_route_trigger_btn"),
                                shape = RoundedCornerShape(17.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = premiumGreen,
                                    labelColor = Color.White
                                ),
                                border = BorderStroke(1.dp, premiumGreen.copy(alpha = 0.8f))
                            )
                        }

                        // 2. "Deselectează tot" Action Chip
                        if (selectedCount > 0) {
                            SuggestionChip(
                                onClick = onDeselectAll,
                                label = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(if (isEnglish) "Deselect All" else "Deselectează tot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                modifier = Modifier.height(34.dp).testTag("deselect_all_spots_btn"),
                                shape = RoundedCornerShape(17.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            )
                        }

                        // 3. "Șterge noi" Action Chip
                        if (customCount > 0) {
                            SuggestionChip(
                                onClick = onClearCustom,
                                label = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(if (isEnglish) "Delete Custom" else "Șterge noi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(17.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                                    labelColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
            }

            // 4. Empty Result Banner or Weather card with List elements inside the LazyColumn
            if (filteredSpots.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            border = if (appSkin == "VINTAGE_RPG") {
                                BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                            } else null,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (isEnglish) "No sights found" else "Nicio atracție găsită",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEnglish) "Try modifying the search text or define a custom location by clicking directly on the map above!" else "Încearcă să modifici textul de căutare sau definește un punct personalizat atingând direct pe harta de mai sus!",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // 5. Weather card (If visible)
                if (weather != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            border = if (appSkin == "VINTAGE_RPG") {
                                BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Left Column: Big icon & Temp & Condition
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            text = weather.iconEmoji,
                                            fontSize = 36.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                            text = "${weather.tempCelsius}°C",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isEnglish) weather.conditionEn else weather.conditionRo,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Divider line inside card
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(80.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )

                                    // Right Column: Details & Tourist Advice
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Tourist Weather Advisory" else "Consilier Meteo Turistic",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        
                                        Text(
                                            text = if (isEnglish) weather.adviceEn else weather.adviceRo,
                                            fontSize = 11.sp,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        // Technical parameters
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Wind
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(text = "${weather.windKmh} km/h", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            
                                            // Rain %
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(11.dp), tint = if (weather.rainProbabilityPercent > 50) Color(0xFFEC4899) else Color(0xFF10B981))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = (if (isEnglish) "Rain: " else "Ploaie: ") + "${weather.rainProbabilityPercent}%",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (weather.rainProbabilityPercent > 50) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            // UV Index
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(11.dp), tint = if (weather.uvIndex > 6) Color(0xFFFBBF24) else Color(0xFF60A5FA))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(text = "UV: ${weather.uvIndex}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                // Presets selection row
                                Text(
                                    text = if (isEnglish) "Simulate Weather Scenario:" else "Simulează alt scenariu meteo:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val presets = listOf(
                                        Pair("DEFAULT", if (isEnglish) "Default" else "Implicit"),
                                        Pair("SUNNY", "☀️ " + (if (isEnglish) "Sunny" else "Senin")),
                                        Pair("RAINY", "🌧️ " + (if (isEnglish) "Rainy" else "Ploaie")),
                                        Pair("CLOUDY", "⛅ " + (if (isEnglish) "Cloudy" else "Noros")),
                                        Pair("HEATWAVE", "🥵 " + (if (isEnglish) "Heat" else "Arșiță"))
                                    )
                                    presets.forEach { (presetKey, displayName) ->
                                        val isSelected = selectedPreset == presetKey
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .clickable { onSelectPreset(presetKey) }
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = displayName,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(
                    items = filteredSpots,
                    key = { spot -> spot.id }
                ) { spot ->
                    val isCurrentStart = (translatedCustomStartSpot?.id == spot.id) || 
                        (translatedCustomStartSpot == null && spot.name == TransitNetwork.getStartSpot(city).translate(isEnglish).name)

                    val cardColor = if (appSkin == "VINTAGE_RPG") {
                        if (spot.isSelected) Color(0xFF382317) else Color(0xFF261910)
                    } else {
                        if (spot.isSelected) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) 
                        else MaterialTheme.colorScheme.surface
                    }

                    val borderColor = if (isCurrentStart) {
                        Color(0xFFEC4899)
                    } else if (spot.isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = if (appSkin == "VINTAGE_RPG") {
                            if (isCurrentStart) {
                                BorderStroke(2.dp, Brush.verticalGradient(listOf(Color(0xFFEC4899), Color(0xFFF472B6))))
                            } else if (spot.isSelected) {
                                BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                            } else {
                                BorderStroke(1.dp, Color(0xFF5C4033).copy(alpha = 0.5f))
                            }
                        } else {
                            BorderStroke(if (isCurrentStart || spot.isSelected) 1.5.dp else 1.dp, borderColor)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clickable { onToggle(spot.id, !spot.isSelected) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = spot.isSelected,
                                onCheckedChange = { onToggle(spot.id, it) },
                                modifier = Modifier.testTag("checkbox_spot_${spot.id}")
                            )

                            // 🎨 PERSONALIZED MINIATURE THUMBNAIL FOR ATTRACTION
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
                                    .size(42.dp)
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

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.wrapContentHeight()
                                ) {
                                    Text(
                                        text = translateSpotName(spot.name, isEnglish),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (spot.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isCurrentStart) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEC4899).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text("START", fontSize = 8.sp, color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (spot.isCustom) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEAB308).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (isEnglish) "CUSTOM" else "PERS.", fontSize = 8.sp, color = Color(0xFFEAB308), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (spot.description.isNotEmpty()) {
                                    Text(
                                        text = spot.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
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
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isEnglish) "Visit duration: ${spot.visitDurationMinutes} mins" else "Durată vizită: ${spot.visitDurationMinutes} min",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pin as Start Quick Button
                                IconButton(
                                    onClick = { onSetStartClick(spot) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = if (isEnglish) "Set as starting point" else "Setează ca pornire",
                                        tint = if (isCurrentStart) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (spot.isCustom) {
                                    IconButton(
                                        onClick = { onDelete(spot.id) },
                                        modifier = Modifier.size(36.dp).testTag("delete_custom_spot_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = if (isEnglish) "Delete spot" else "Șterge atracție",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
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

    }
}

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
    liveTransitVehicles: List<com.example.domain.LiveTransitVehicle> = emptyList(),
    onVehicleClick: ((com.example.domain.LiveTransitVehicle) -> Unit)? = null,
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

@Composable
fun LiveOperatorApiPanel(
    city: String,
    modifier: Modifier = Modifier,
    isEnglish: Boolean = false,
    liveVehiclesPassed: List<com.example.domain.LiveTransitVehicle> = emptyList(),
    onVehicleClick: ((com.example.domain.LiveTransitVehicle) -> Unit)? = null,
    appSkin: String = "MODERN"
) {
    val context = LocalContext.current
    val op = remember(city) { TransportApiEngine.getOperatorForCity(city) }
    val smsConfig = remember(city) { TransportApiEngine.getSmsTicketDetails(city) }
    val alerts = remember(city) { TransportApiEngine.getLiveAlerts(city) }
    
    val translatedAlerts = remember(alerts, isEnglish) {
        alerts.map { alert ->
            alert.copy(
                title = translateAlertText(alert.title, isEnglish),
                message = translateAlertText(alert.message, isEnglish)
            )
        }
    }

    // State to simulate paid ticket right inside the app
    var hasActiveTicket by remember { mutableStateOf(false) }
    var ticketTimeLeft by remember { mutableStateOf(smsConfig.validityMinutes * 60) } // in seconds
    var showAlertsDesc by remember { mutableStateOf(false) }
    
    // Fallback to local simulation if no vehicles passed from the main runner
    var liveVehiclesLocal by remember(city) { mutableStateOf(TransportApiEngine.simulateLiveVehicles(city)) }
    val liveVehicles = if (liveVehiclesPassed.isNotEmpty()) liveVehiclesPassed else liveVehiclesLocal
    
    // Timer for bilet electronic countdown
    LaunchedEffect(hasActiveTicket) {
        if (hasActiveTicket) {
            ticketTimeLeft = smsConfig.validityMinutes * 60
            while (ticketTimeLeft > 0) {
                kotlinx.coroutines.delay(1000)
                ticketTimeLeft--
            }
            hasActiveTicket = false
        }
    }
    
    // Timer for refreshing local live vehicle speeds/occupancy/delays if no global active feed
    LaunchedEffect(city) {
        while(true) {
            kotlinx.coroutines.delay(10000) // update every 10s
            if (liveVehiclesPassed.isEmpty()) {
                liveVehiclesLocal = TransportApiEngine.simulateLiveVehicles(city)
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else Color(0xFF0F172A)
        ),
        border = if (appSkin == "VINTAGE_RPG") {
            BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
        } else {
            BorderStroke(1.dp, Color(0xFF334155))
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(op) {
                                TransportOperator.STB -> "🚋"
                                TransportOperator.CTP -> "🚍"
                                TransportOperator.RATBV -> "🚌"
                                TransportOperator.ELIADO -> "🚎"
                            },
                            fontSize = 18.sp
                        )
                    }
                    Column {
                        Text(
                            text = op.shortName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isEnglish) "API Dispatch Connected" else "Dispecerat API Conectat",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981), // Emerald online indicator
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Pulsing Green feed indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Text(
                        text = "LIVE FEED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Subtitle full name of operator
            Text(
                text = op.fullName,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 1. Alerts Section (Collapsible)
            if (translatedAlerts.isNotEmpty()) {
                Surface(
                    onClick = { showAlertsDesc = !showAlertsDesc },
                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isEnglish) "Active Transit Alerts (${translatedAlerts.size})" else "Alerte de Tranzit Active (${translatedAlerts.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF87171)
                                )
                            }
                            Icon(
                                imageVector = if (showAlertsDesc) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        // Detail text inside
                        AnimatedVisibility(visible = showAlertsDesc) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                translatedAlerts.forEach { alert ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "•", color = Color(0xFFF87171), fontSize = 11.sp)
                                        Column {
                                            Text(
                                                text = alert.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = alert.message,
                                                fontSize = 10.sp,
                                                color = Color(0xFFCBD5E1),
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            
            // 2. LIVE VEHICLES SENSOR (Horizontal Row)
            Text(
                text = if (isEnglish) "🚌 Live GPS Vehicle Positions (${op.shortName})" else "🚌 GPS Live Poziție Vehicule (${op.shortName})",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(liveVehicles.size) { idx ->
                    val v = liveVehicles[idx]
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else Color(0xFF1E293B)
                        ),
                        border = if (appSkin == "VINTAGE_RPG") {
                            BorderStroke(1.2.dp, Color(0xFFC5A059).copy(alpha = 0.7f))
                        } else {
                            BorderStroke(1.dp, Color(0xFF475569))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(180.dp)
                            .clickable(enabled = onVehicleClick != null) {
                                onVehicleClick?.invoke(v)
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = v.lineName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF3B82F6)
                                )
                                Text(
                                    text = if (v.delayMinutes > 0) "+${v.delayMinutes} min" else (if (isEnglish) "On time" else "La timp"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (v.delayMinutes > 0) Color(0xFFEAB308) else Color(0xFF10B981)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ID: ${v.id.substringAfterLast("-")}",
                                fontSize = 8.sp,
                                color = Color(0xFF64748B)
                              )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEnglish) "Direction: ${v.direction.replace("Spre", "To")}" else "Direcție: ${v.direction}",
                                fontSize = 9.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${v.speedKmh} km/h • GPS OK",
                                    fontSize = 8.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "${v.occupancyPercentage}%",
                                        fontSize = 8.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEnglish) "📍 Tap to locate on map" else "📍 Apasă pt. localizare",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF43F5E),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(8.dp))
            
            // 3. Ticketing SMS & Wallet Section
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEnglish) "🎟️ Electronic Tickets Wallet" else "🎟️ Portofel Bilete Electronice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
                Text(
                    text = if (isEnglish) "SMS to ${smsConfig.number}" else "SMS la ${smsConfig.number}",
                    fontSize = 10.sp,
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            if (hasActiveTicket) {
                // Active ticket UI
                val minsLeft = ticketTimeLeft / 60
                val secsLeft = ticketTimeLeft % 60
                val countdownFormatted = String.format("%02d:%02d", minsLeft, secsLeft)
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1E2822) else Color(0xFF0284C7).copy(alpha = 0.12f)
                    ),
                    border = if (appSkin == "VINTAGE_RPG") {
                        BorderStroke(1.8.dp, Color(0xFF10B981))
                    } else {
                        BorderStroke(1.dp, Color(0xFF0284C7))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "ACTIVE TICKET: ${op.shortName}" else "BILET ACTIV: ${op.shortName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "VALID" else "VALID",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Countdown Clock
                        Text(
                            text = countdownFormatted,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isEnglish) "TIME REMAINING" else "TIMP RĂMAS",
                            fontSize = 8.sp,
                            color = Color(0xFF94A3B8)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Holographic confirmation code generator
                        val securityCode = remember { "SMS-${op.shortName}-${1000 + kotlin.random.Random.nextInt(9000)}-OK" }
                        Text(
                            text = if (isEnglish) "Control Code: $securityCode" else "Cod Control: $securityCode",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) "Presenting this screen during inspection confirms urban ticket payment." else "Prezentarea acestui ecran la control confirmă plata biletului urban.",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Inactive state: trigger buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = getTranslatedCostExplanation(city, isEnglish, smsConfig.costExplanation),
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 13.sp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                TransportApiEngine.triggerSmsCompose(context, smsConfig)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Send Real SMS" else "Trimite SMS Real",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                hasActiveTicket = true
                            },
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Simulate Purchase" else "Simulează Achiziție",
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

@Composable
fun AiGuideTab(
    recommendationText: String,
    isLoading: Boolean,
    onCallAi: () -> Unit,
    isEnglish: Boolean = false,
    aiProvider: String = "GEMINI",
    appSkin: String = "MODERN"
) {
    LaunchedEffect(recommendationText) {
        if (recommendationText.isEmpty() && !isLoading) {
            onCallAi()
        }
    }
    val providerName = when (aiProvider) {
        "OPENAI" -> "OpenAI"
        "OPENROUTER" -> "OpenRouter"
        else -> "Gemini"
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isEnglish) "$providerName is compiling optimized response..." else "$providerName scrie răspunsul optimizat...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (recommendationText.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = Color(0xFFEAB308)
                )
                Text(
                    text = if (isEnglish) "$providerName AI Tour Guide" else "Ghid Turistic AI $providerName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEnglish) "Let $providerName AI analyze your selected spots and compile a full-day optimized schedule with estimated travel times, cost approximations, transit recommendations and local tips!"
                           else "Lăsați AI-ul $providerName să analizeze punctele tale alese și să compileze o descriere a întregii zile cu ore estimate, recomandări de autobuze optime din mers și detalii specifice fiecărui obiectiv!",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onCallAi,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("request_ai_button")
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEnglish) "Generate AI Route with $providerName" else "Generează Traseu AI cu $providerName")
                }
            }
        }
        return
    }

    // Scrollable AI Markdowns Output
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308))
                    Text(
                        text = if (isEnglish) "Real-Time AI Guide & Tips" else "Sfaturi și Ghid AI Real-Time",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onCallAi,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("regenerate_ai_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(if (isEnglish) "Refresh" else "Actualizează", fontSize = 10.sp)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = if (appSkin == "VINTAGE_RPG") {
                    BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                } else null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = recommendationText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun LiveTestingTab(
    city: String,
    journey: OptimizedJourney?,
    testingLogs: List<TestingLog>,
    useRealDeviceGps: Boolean,
    onToggleRealGps: (Boolean) -> Unit,
    mapActionMode: String,
    onSetMapActionMode: (String) -> Unit,
    onSaveLog: (String, String, String) -> Unit,
    onDeleteLog: (Long) -> Unit,
    onClearLogs: () -> Unit,
    isEnglish: Boolean = false,
    appSkin: String = "MODERN"
) {
    var placeNameInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var observerNameInput by remember(isEnglish) { mutableStateOf(if (isEnglish) "Field Tester" else "Testor Teren") }

    val itinerarySpots = remember(journey, isEnglish) {
        val list = mutableListOf<String>()
        journey?.orderedSpots?.forEach { list.add(translateSpotName(it.name, isEnglish)) }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_testing_tab_list")
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Tracker Control Header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = if (appSkin == "VINTAGE_RPG") {
                    BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isEnglish) "🛠️ Real Field Testing Mode" else "🛠️ Mod Testare Reală pe Teren",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEnglish) "This section controls GPS and allows recording on-site audits while traversing the tourist bus route." else "Această secțiune controlează GPS-ul și permite înregistrarea de audituri de la fața locului în timp ce parcurgi traseul de autobuz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // GPS Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "Hardware GPS Sensor (Satellite)" else "Senzor GPS Hardware (Satelit)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isEnglish) {
                                    if (useRealDeviceGps) "Active: following real physical location." else "Inactive: using simulated coordinates."
                                } else {
                                    if (useRealDeviceGps) "Activ: urmărește locația fizică reală." else "Inactiv: folosește coordonate simulate."
                                },
                                fontSize = 10.sp,
                                color = if (useRealDeviceGps) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useRealDeviceGps,
                            onCheckedChange = onToggleRealGps,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Map action mode segmented chooser
                    Text(
                        text = if (isEnglish) "On Map Tap:" else "La Apăsarea pe Hartă:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = mapActionMode == "ADD_SPOT",
                            onClick = { onSetMapActionMode("ADD_SPOT") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text(if (isEnglish) "Add Attraction" else "Adaugă Atracție", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = mapActionMode == "SET_GPS",
                            onClick = { onSetMapActionMode("SET_GPS") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text(if (isEnglish) "Teleport GPS" else "Teleportează GPS", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Logging Interface
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surface
                ),
                border = if (appSkin == "VINTAGE_RPG") {
                    BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isEnglish) "📝 Record Field Audit" else "📝 Înregistrează Audit de Teren",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEnglish) "Take notes regarding bus delays, attraction conditions, or station cleanliness." else "Scrie notițe referitoare la întârzieri de autobuze, starea atracțiilor sau curățenia stației.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (itinerarySpots.isNotEmpty()) {
                        Text(
                            text = if (isEnglish) "Quickly select spot from route:" else "Alege rapid punct din traseu:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 6.dp)
                        ) {
                            items(
                                items = itinerarySpots,
                                key = { s -> s }
                            ) { s ->
                                val isSelected = placeNameInput == s
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { placeNameInput = s }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = s,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = placeNameInput,
                        onValueChange = { placeNameInput = it },
                        label = { Text(if (isEnglish) "Location / Station Name" else "Nume Locație / Stație", fontSize = 11.sp) },
                        placeholder = { Text(if (isEnglish) "e.g., Sanitas Station, Graft Bastion..." else "Ex. Stația Sanitas, Bastionul Graft...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text(if (isEnglish) "Audit Observation (Field Note)" else "Observație Audit (Notă Teren)", fontSize = 11.sp) },
                        placeholder = { Text(if (isEnglish) "e.g. Bus 4 arrived with 3 min delay. Fairly empty." else "Ex. Autobuzul 4 a sosit cu 3 min întârziere. Destul de liber.", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = observerNameInput,
                        onValueChange = { observerNameInput = it },
                        label = { Text(if (isEnglish) "Inspector / Auditor / Tester Name" else "Nume Controlor / Auditor / Testor", fontSize = 11.sp) },
                        placeholder = { Text(if (isEnglish) "e.g. Popescu Ion, Maria, etc." else "Ex. Popescu Ion, Maria, etc.", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (placeNameInput.isNotBlank() && noteInput.isNotBlank()) {
                                val obs = if (observerNameInput.isBlank()) (if (isEnglish) "Field Tester" else "Testor Teren") else observerNameInput
                                onSaveLog(placeNameInput, noteInput, obs)
                                noteInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = placeNameInput.isNotBlank() && noteInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEnglish) "Save to Report" else "Salvează în Raport", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Saved Audit Logs list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "📊 Local Testing Report (${testingLogs.size})" else "📊 Raport de Testare local (${testingLogs.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (testingLogs.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val context = LocalContext.current
                        TextButton(
                            onClick = {
                                val reportText = testingLogs.joinToString("\n\n") { "📍 ${it.placeName}\n👤 ${it.observerName}\n📝 ${it.note}" }
                                val reportTitle = if (isEnglish) "Field Testing Report for $city:\n\n" else "Raport Testare Teren pentru $city:\n\n"
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, reportTitle + reportText)
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(if (isEnglish) "Share" else "Distribuie", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        TextButton(
                            onClick = onClearLogs,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(if (isEnglish) "Clear" else "Șterge", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (testingLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    ),
                    border = if (appSkin == "VINTAGE_RPG") {
                        BorderStroke(1.2.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                    } else null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "No observations recorded yet.\nTake notes on route status when you are in the field!" else "Nicio observație înregistrată încă.\nNotează starea traseului când ești pe teren!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = testingLogs,
                key = { log -> log.id }
            ) { log ->
                val timeFormatted = remember(log.timestamp) {
                    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(log.timestamp))
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (appSkin == "VINTAGE_RPG") {
                        BorderStroke(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "📍 ${translateSpotName(log.placeName, isEnglish)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• $timeFormatted",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { onDeleteLog(log.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = if (isEnglish) "Delete log" else "Șterge log",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEnglish) "Observed by: ${log.observerName}" else "Observat de: ${log.observerName}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

fun translateCityName(city: String, isEnglish: Boolean): String {
    return if (isEnglish) {
        when (city) {
            "București" -> "Bucharest"
            "Brașov" -> "Brasov"
            "Câmpina" -> "Campina"
            else -> city
        }
    } else {
        city
    }
}

fun translateRouteDetailsText(text: String, isEnglish: Boolean): String {
    if (!isEnglish) return text
    var translated = text
        .replace("Pornire:", "Start:")
        .replace("Gara Centrală", "Central Station")
        .replace("Traseu:", "Route:")
        .replace("Etape de transport:", "Transport stages:")
    val lines = translated.split("\n").map { line ->
        if (line.trim().startsWith("- ")) {
            val rawStage = line.trim().substring(2) // remove "- "
            val durationIndex = rawStage.lastIndexOf("(")
            if (durationIndex != -1) {
                val directions = rawStage.substring(0, durationIndex).trim()
                val durationPart = rawStage.substring(durationIndex).trim() // e.g. "(15 min)"
                val translatedDurationPart = durationPart.replace("min", "minutes")
                "- ${translateDirections(directions, true)} $translatedDurationPart"
            } else {
                "- ${translateDirections(rawStage, true)}"
            }
        } else {
            if (line.startsWith("Route: ")) {
                val routeList = line.substring(7)
                val translatedRouteList = routeList.split(" ➔ ").map { spot ->
                    translateSpotName(spot.trim(), true)
                }.joinToString(" ➔ ")
                "Route: $translatedRouteList"
            } else if (line.startsWith("Start: ")) {
                val startName = line.substring(7).trim()
                "Start: ${translateSpotName(startName, true)}"
            } else {
                line
            }
        }
    }
    return lines.joinToString("\n")
}

@Composable
fun SavedItinerariesTab(
    itineraries: List<SavedItinerary>,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    isEnglish: Boolean = false,
    appSkin: String = "MODERN"
) {
    val context = LocalContext.current
    if (itineraries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isEnglish) "No saved itineraries yet." else "Nu ai itinerarii salvate încă.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { showClearAllDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isEnglish) "Clear All" else "Șterge Toate", fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        items(
            items = itineraries,
            key = { record -> record.id }
        ) { record ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surface
                ),
                border = if (appSkin == "VINTAGE_RPG") {
                    BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                } else null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Itinerary ${translateCityName(record.city, true)}" else "Itinerar ${record.city}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val hrs = record.totalDurationMinutes / 60
                            val mins = record.totalDurationMinutes % 60
                            Text(
                                text = if (isEnglish) "${record.spotsCount} objectives ➔ Total: $hrs h $mins min" else "${record.spotsCount} obiective ➔ Durată totală: $hrs h $mins min",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    val translatedDetails = translateRouteDetailsText(record.routeDetails, isEnglish)
                                    val shareIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, if (isEnglish) "Here is my itinerary for ${record.city}:\n\n$translatedDetails" else "Aici este itinerarul meu pentru ${record.city}:\n\n$translatedDetails")
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = if (isEnglish) "Share" else "Distribuie",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { onDelete(record.id) },
                                modifier = Modifier.size(36.dp).testTag("delete_saved_itinerary_${record.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = if (isEnglish) "Delete saved" else "Șterge salvare",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = translateRouteDetailsText(record.routeDetails, isEnglish),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(if (isEnglish) "Clear All Saved Itineraries?" else "Ștergeți Toate Itinerariile Salvate?") },
            text = { Text(if (isEnglish) "Are you sure you want to delete all saved itineraries? This action cannot be undone." else "Sunteți sigur că doriți să ștergeți toate itinerariile salvate? Această acțiune nu poate fi anulată.") },
            confirmButton = {
                Button(
                    onClick = { 
                        showClearAllDialog = false
                        onClearAll() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isEnglish) "Delete" else "Șterge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "Anulare")
                }
            }
        )
    }

    // Close the Column
}
}

@Composable
fun SettingsDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    selectedCity: String,
    onCityChange: (String) -> Unit,
    isStationsVisible: Boolean,
    onStationsToggle: (Boolean) -> Unit,
    isTransitLinesVisible: Boolean,
    onTransitLinesToggle: (Boolean) -> Unit,
    simulationSpeed: Float,
    onSimulationSpeedChange: (Float) -> Unit,
    mapColorScheme: String,
    onMapColorSchemeChange: (String) -> Unit,
    isHighResolution: Boolean,
    onHighResolutionToggle: (Boolean) -> Unit,
    isSoundEnabled: Boolean,
    onSoundEnabledToggle: (Boolean) -> Unit,
    isEnglish: Boolean,
    onLanguageToggle: (Boolean) -> Unit,
    openaiApiKey: String,
    onOpenaiApiKeyChange: (String) -> Unit,
    aiProvider: String,
    onAiProviderChange: (String) -> Unit,
    openrouterApiKey: String,
    onOpenrouterApiKeyChange: (String) -> Unit,
    openrouterModel: String,
    onOpenrouterModelChange: (String) -> Unit,
    appSkin: String = "MODERN",
    onAppSkinChange: (String) -> Unit,
    onFactoryReset: () -> Unit
) {
    if (!show) return

    var showChangelog by remember { mutableStateOf(false) }

    if (showChangelog) {
        ChangelogDialog(
            isEnglish = isEnglish,
            onDismiss = { showChangelog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = if (appSkin == "VINTAGE_RPG") {
            Modifier.border(
                BorderStroke(2.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45)))),
                shape = RoundedCornerShape(24.dp)
            )
        } else Modifier,
        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEnglish) "Application Settings" else "Setări Aplicație",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION: ACTIVE CITY SELECTOR
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEnglish) "Select Active City" else "Selectează Orașul Activ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val cities = listOf(
                            Triple("București", "Bucharest", "🚋"),
                            Triple("Cluj-Napoca", "Cluj-Napoca", "🚍"),
                            Triple("Brașov", "Brasov", "🏔️"),
                            Triple("Câmpina", "Campina", "🏰")
                        )
                        cities.chunked(2).forEach { rowCities ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCities.forEach { (cityRaw, cityEng, emoji) ->
                                    val isSelected = selectedCity == cityRaw
                                    val displayName = if (isEnglish) cityEng else cityRaw
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.Transparent
                                                },
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onCityChange(cityRaw) }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = emoji, fontSize = 16.sp)
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // SECTION: LANGUAGE SELECTOR
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isEnglish) "App Language" else "Limba Aplicației",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onLanguageToggle(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isEnglish) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f).testTag("lang_ro_button")
                        ) {
                            Text("Română (RO)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onLanguageToggle(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnglish) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f).testTag("lang_en_button")
                        ) {
                            Text("English (EN)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // SECTION: APP SKIN / THEME
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isEnglish) "Application Theme" else "Tematica Aplicației",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAppSkinChange("MODERN") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appSkin == "MODERN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (appSkin == "MODERN") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEnglish) "Modern UI" else "Interfață Modernă", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        Button(
                            onClick = { onAppSkinChange("VINTAGE_RPG") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEnglish) "Vintage RPG" else "RPG Vintage", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // SECTION: MAP VISUALS
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEnglish) "Interactive Map Layers" else "Straturi Vizuale Hartă",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Stations Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "Show Bus Stations" else "Afișează Stațiile de Tranzit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEnglish) "Display vehicle path stations" else "Arată stațiile de autobuz pe hartă",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isStationsVisible,
                            onCheckedChange = onStationsToggle,
                            modifier = Modifier.testTag("toggle_stations_switch")
                        )
                    }

                    // Transit lines Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "Show Bus Roads" else "Afișează Liniile de Tranzit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEnglish) "Draw complete bus tracks on map" else "Randează traseele complete pe hartă",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTransitLinesVisible,
                            onCheckedChange = onTransitLinesToggle,
                            modifier = Modifier.testTag("toggle_transit_lines_switch")
                        )
                    }

                    // Map Resolution Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "High Resolution Map (HD/Retina)" else "Hartă de Rezoluție Înaltă (HD/Retina)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEnglish) "Load double-density 512px map tiles and street details" else "Încarcă dale de hartă la densitate dublă (512px) și detalii stradale",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isHighResolution,
                            onCheckedChange = onHighResolutionToggle,
                            modifier = Modifier.testTag("toggle_high_res_map_switch")
                        )
                    }

                    // Map Scheme Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isEnglish) "Map Style & API Service" else "Stil Hartă și Serviciu API",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            val schemes = listOf(
                                "Vintage Parchment",
                                "OsmAnd (HD maps + relief)",
                                "OSM Standard", 
                                "OSM Humanitarian", 
                                "OSM French", 
                                "Slate Neon", 
                                "Cyberpunk", 
                                "Muted Gray", 
                                "Ocean Breeze",
                                "Google Maps"
                            )
                            items(schemes.size) { index ->
                                val themeName = schemes[index]
                                val isActive = mapColorScheme == themeName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onMapColorSchemeChange(themeName) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = themeName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // SECTION: NAVIGATION PROP
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEnglish) "GPS Simulation Parameters" else "Proprietăți Simulare GPS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Speed Multipliers
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isEnglish) "Movement Simulation Speed" else "Viteza de Deplasare Simulare",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f).forEach { speed ->
                                val isActive = simulationSpeed == speed
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onSimulationSpeedChange(speed) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Chime Alert Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "Audible Step Chime" else "Semnale Sonore Etape",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEnglish) "Play notification beep on stage completion" else "Redă un sunet la finalizarea fiecărei etape",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = onSoundEnabledToggle,
                            modifier = Modifier.testTag("toggle_sound_switch")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // SECTION: AI API SETTINGS (OpenAI vs Gemini vs OpenRouter)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEnglish) "AI Service Provider" else "Furnizor Serviciu AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Provider Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isEnglish) "Select AI Engine" else "Selectează Motorul AI",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onAiProviderChange("GEMINI") },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (aiProvider == "GEMINI") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (aiProvider == "GEMINI") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f).testTag("provider_gemini_button")
                            ) {
                                Text("Gemini", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Button(
                                onClick = { onAiProviderChange("OPENAI") },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (aiProvider == "OPENAI") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (aiProvider == "OPENAI") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f).testTag("provider_openai_button")
                            ) {
                                Text("OpenAI", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Button(
                                onClick = { onAiProviderChange("OPENROUTER") },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (aiProvider == "OPENROUTER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (aiProvider == "OPENROUTER") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f).testTag("provider_openrouter_button")
                            ) {
                                Text("OpenRouter", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    // OpenAI Key TextField
                    if (aiProvider == "OPENAI") {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = openaiApiKey,
                            onValueChange = onOpenaiApiKeyChange,
                            label = { Text(if (isEnglish) "OpenAI API Key" else "Cheie API OpenAI") },
                            placeholder = { Text("sk-proj-...") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openai_key_input"),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure Key",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Text(
                            text = if (isEnglish) 
                                "Enter your personal OpenAI API Key. It is stored securely in your local preferences." 
                                else "Introdu cheia personală API OpenAI. Este salvată în siguranță local.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // OpenRouter Block
                    if (aiProvider == "OPENROUTER") {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // API Key Input
                        OutlinedTextField(
                            value = openrouterApiKey,
                            onValueChange = onOpenrouterApiKeyChange,
                            label = { Text(if (isEnglish) "OpenRouter API Key" else "Cheie API OpenRouter") },
                            placeholder = { Text("sk-or-...") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openrouter_key_input"),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure Key",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Model ID Input
                        OutlinedTextField(
                            value = openrouterModel,
                            onValueChange = onOpenrouterModelChange,
                            label = { Text(if (isEnglish) "OpenRouter Model ID" else "ID Model OpenRouter") },
                            placeholder = { Text("google/gemma-2-9b-it:free") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openrouter_model_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Presets Row for Quick Insertion
                        Text(
                            text = if (isEnglish) "Model Quick Presets:" else "Presetări rapide model:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Display clean horizontal scrollable presets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                "gpt-oss-120b",
                                "google/gemma-2-9b-it:free",
                                "meta-llama/llama-3-8b-instruct:free",
                                "mistralai/mistral-7b-instruct:free"
                            )
                            presets.forEach { presetName ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (openrouterModel == presetName) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { onOpenrouterModelChange(presetName) }
                                        .border(
                                            width = 1.dp,
                                            color = if (openrouterModel == presetName) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = presetName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (openrouterModel == presetName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isEnglish) 
                                "Enter your OpenRouter key and select/type a Model. Use 'gpt-oss-120b' or other free models on OpenRouter." 
                                else "Introdu cheia OpenRouter și alege/scrie un Model. Poți folosi 'gpt-oss-120b' sau alte modele gratuite din OpenRouter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // SECTION: DATA & STORAGE
                var showWipeConfirm by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEnglish) "Data & Storage" else "Date și Stocare",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = { showWipeConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isEnglish) "Factory Reset & Wipe All Data" else "Ștergere totală a datelor", fontWeight = FontWeight.Bold)
                    }

                    if (showWipeConfirm) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isEnglish) "Are you sure? This will permanently delete ALL saved itineraries, field testing logs, and custom spots." else "Ești sigur? Aceasta va șterge DEFINITIV toate itinerariile salvate, rapoartele de testare și punctele personalizate.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { showWipeConfirm = false }, modifier = Modifier.weight(1f)) {
                                        Text(if (isEnglish) "Cancel" else "Anulează")
                                    }
                                    Button(
                                        onClick = {
                                            onFactoryReset()
                                            showWipeConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isEnglish) "WIPE" else "ȘTERGE")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("close_settings_button")
            ) {
                Text(
                    text = if (isEnglish) "Apply & Close" else "Aplică și Închide",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { showChangelog = true }) {
                Text(text = if (isEnglish) "Changelog" else "Modificări")
            }
        }
    )
}

@Composable
fun ChangelogDialog(isEnglish: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isEnglish) "Changelog & Versions" else "Versiuni și Modificări", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val versions = listOf(
                    "0.107" to (if (isEnglish) "Added the ability to clear all saved itineraries." else "S-a adăugat posibilitatea de a șterge toate itinerariile salvate."),
                    "0.106" to (if (isEnglish) "Added the ability to share the field testing report." else "S-a adăugat posibilitatea de a partaja raportul de testare pe teren."),
                    "0.105" to (if (isEnglish) "Added the ability to share saved itineraries." else "S-a adăugat funcționalitatea de a partaja itinerarii salvate."),
                    "0.104" to (if (isEnglish) "Added Changelog button in settings and version tracking." else "S-a adăugat butonul de Modificări în setări și urmărirea versiunilor."),
                    "0.103" to (if (isEnglish) "Fix automatic regeneration of AI tips on tab switch." else "S-a reparat regenerarea automată a sfaturilor AI la schimbarea filei."),
                    "0.102" to (if (isEnglish) "Added new AI Providers (OpenRouter) and custom skins." else "S-au adăugat noi furnizori AI (OpenRouter) și teme vizuale."),
                    "0.101" to (if (isEnglish) "Initial beta release with interactive map, routing and AI tips." else "Lansare inițială beta cu hartă interactivă, rute și sfaturi AI.")
                )
                
                versions.forEach { (version, description) ->
                    Text(text = "v$version", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = description, modifier = Modifier.padding(bottom = 12.dp, top = 2.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = if (isEnglish) "Close" else "Închide")
            }
        }
    )
}
