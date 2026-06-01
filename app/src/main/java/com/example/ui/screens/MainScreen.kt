package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val customStartSpot by viewModel.customStartSpot.collectAsState()
    val savedItineraries by viewModel.savedItineraries.collectAsState()
    val optimizedJourney by viewModel.optimizedJourney.collectAsState()
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

    // --- LIVE GPS LOCATION & ROUTE NAVIGATION SIMULATION ---
    var userGpsLocation by remember(selectedCity) { mutableStateOf<Pair<Double, Double>?>(null) }
    var isSimulatingNavigation by remember { mutableStateOf(false) }
    var activeNavigationLegIndex by remember { mutableStateOf(0) }
    var navigationProgressFraction by remember { mutableStateOf(0f) }

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
                snackbarHostState.showSnackbar("🛰️ GPS Real Activ! Harta îți va urmări poziția fizică din satelit.")
            }
        } else {
            useRealDeviceGps = false
            scope.launch {
                snackbarHostState.showSnackbar("❌ Permisiunea GPS refuzată. Modul Satelit are nevoie de permisiuni.")
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
                        // Quick city picker in actions bar
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("București", "Cluj-Napoca", "Brașov").forEach { city ->
                                val isActive = selectedCity == city
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.selectCity(city) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = city,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                    spots = citySpots,
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
                    isEnglish = isEnglishLanguage
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                isCollapsed = false,
                                onToggleDashboard = { isDashboardExpanded = false },
                                isSimulatingNavigation = isSimulatingNavigation,
                                onStartSimulationClick = {
                                    if (userGpsLocation == null) {
                                        val seedCoords = when (selectedCity) {
                                            "București" -> Pair(44.4411, 26.0973)
                                            "Brașov" -> Pair(45.6540, 25.6030)
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

                            // Tab Row
                            TabRow(
                                selectedTabIndex = selectedDashboardTab,
                                containerColor = Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Tab(
                                    selected = selectedDashboardTab == 0,
                                    onClick = { selectedDashboardTab = 0 },
                                    modifier = Modifier.testTag("tab_spots_checklist")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.List,
                                            contentDescription = null,
                                            tint = if (selectedDashboardTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "Attractions" else "Atracții",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDashboardTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Tab(
                                    selected = selectedDashboardTab == 1,
                                    onClick = { selectedDashboardTab = 1 },
                                    modifier = Modifier.testTag("tab_itinerary_timeline")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = if (selectedDashboardTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "Itinerary" else "Itinerar",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDashboardTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Tab(
                                    selected = selectedDashboardTab == 2,
                                    onClick = { selectedDashboardTab = 2 },
                                    modifier = Modifier.testTag("tab_live_testing")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (selectedDashboardTab == 2) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "Testing" else "Testare",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDashboardTab == 2) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Tab(
                                    selected = selectedDashboardTab == 3,
                                    onClick = { selectedDashboardTab = 3 },
                                    modifier = Modifier.testTag("tab_ai_guide")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (selectedDashboardTab == 3) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "AI Guide" else "Ghid AI",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDashboardTab == 3) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Tab(
                                    selected = selectedDashboardTab == 4,
                                    onClick = { selectedDashboardTab = 4 },
                                    modifier = Modifier.testTag("tab_saved_itineraries")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = if (selectedDashboardTab == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isEnglishLanguage) "Saved" else "Salvate",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDashboardTab == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

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
                                            spots = citySpots,
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
                                            onSelectPreset = { preset -> viewModel.setWeatherPreset(preset) }
                                        )
                                        1 -> TimelineTab(isEnglish = isEnglishLanguage,
                                            journey = optimizedJourney,
                                            startLabel = customStartSpot?.let { it.translate(isEnglishLanguage).name } ?: TransitNetwork.getStartSpot(selectedCity).translate(isEnglishLanguage).name,
                                            startHour = "09:00",
                                            city = selectedCity,
                                            isSimulatingNavigation = isSimulatingNavigation,
                                            onStartSimulationClick = {
                                                if (userGpsLocation == null) {
                                                    // Auto-enable GPS to initial position when starting routing simulation
                                                    val seedCoords = when (selectedCity) {
                                                        "București" -> Pair(44.4411, 26.0973)
                                                        "Brașov" -> Pair(45.6540, 25.6030)
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
                                            }
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
                                            }
                                        )
                                        3 -> AiGuideTab(
                                            recommendationText = aiRecommendation,
                                            isLoading = isAiLoading,
                                            onCallAi = { viewModel.askGeminiForItinerary(isEnglishLanguage) },
                                            isEnglish = isEnglishLanguage
                                        )
                                        4 -> SavedItinerariesTab(
                                            isEnglish = isEnglishLanguage,
                                            itineraries = savedItineraries,
                                            onDelete = { id -> viewModel.deleteSavedItinerary(id) }
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
                            .padding(horizontal = 12.dp, vertical = 6.dp),
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
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Adaugă Punct Turistic Nou")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Introdu detaliile punctului selectat direct pe hartă (Lat: ${String.format("%.4f", clickedLat)}, Lng: ${String.format("%.4f", clickedLng)}):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newSpotName,
                        onValueChange = { newSpotName = it },
                        label = { Text("Nume Atracție *") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_spot_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newSpotDesc,
                        onValueChange = { newSpotDesc = it },
                        label = { Text("Descriere Scurtă") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = newSpotDuration,
                        onValueChange = { newSpotDuration = it },
                        label = { Text("Timp estimat în vizită (minute)") },
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
                                snackbarHostState.showSnackbar("S-a adăugat: $newSpotName")
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Numele atracției este obligatoriu!")
                            }
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text("Salvează")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSpotDialog = false }) {
                    Text("Anulează")
                }
            }
        )
    }

    SettingsDialog(
        show = showSettingsDialog,
        onDismiss = { showSettingsDialog = false },
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
        onLanguageToggle = { isEnglishLanguage = it }
    )
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
        else -> "0.65 € + VAT (~3.2 Lei) for a 30-minute urban travel on any CTP Cluj line."
    }
}

@Composable
fun OptimizedSummaryBar(
    city: String,
    journey: OptimizedJourney?,
    onSaveClick: () -> Unit,
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
                    
                    Text(
                        text = if (isEnglish) "🏛️ $count spot${if (count != 1) "s" else ""} • 🕒 ${hours}h ${minutes}m • 🎫 $fare Lei" else "🏛️ $count obiective • 🕒 ${hours}h ${minutes}m • 🎫 $fare Lei",
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
    onSelectPreset: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterOnlySelected by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEnglish) "Available Spots in $city" else "Puncte Disponibile în $city",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val selectedCount = spots.count { it.isSelected }
                if (selectedCount > 0) {
                    TextButton(
                        onClick = onDeselectAll,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("deselect_all_spots_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isEnglish) "Deselect All" else "Deselectează tot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val customCount = spots.count { it.isCustom }
                if (customCount > 0) {
                    TextButton(
                        onClick = onClearCustom,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isEnglish) "Delete custom" else "Șterge noi", fontSize = 11.sp)
                    }
                }
            }
        }

        // Search & Filter bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (isEnglish) "Search attractions..." else "Caută atracții...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = if (isEnglish) "Clear" else "Șterge", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("attr_search_tf"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
                shape = RoundedCornerShape(24.dp)
            )

            FilterChip(
                selected = filterOnlySelected,
                onClick = { filterOnlySelected = !filterOnlySelected },
                label = { Text(if (isEnglish) "Selected only" else "Doar selectate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.height(34.dp).testTag("filter_selected_chip"),
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        if (filteredSpots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    if (weather != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
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

                items(filteredSpots) { spot ->
                    val isCurrentStart = (translatedCustomStartSpot?.id == spot.id) || 
                        (translatedCustomStartSpot == null && spot.name == TransitNetwork.getStartSpot(city).translate(isEnglish).name)

                    val cardColor = if (spot.isSelected) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) 
                    else MaterialTheme.colorScheme.surface

                    val borderColor = if (isCurrentStart) {
                        Color(0xFFEC4899)
                    } else if (spot.isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(if (isCurrentStart || spot.isSelected) 1.5.dp else 1.dp, borderColor),
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

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.wrapContentHeight()
                                ) {
                                    Text(
                                        text = spot.name,
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

@Composable
fun TimelineTab(
    journey: OptimizedJourney?,
    startLabel: String,
    startHour: String,
    city: String,
    isSimulatingNavigation: Boolean = false,
    onStartSimulationClick: () -> Unit = {},
    onStopSimulationClick: () -> Unit = {},
    isEnglish: Boolean = false
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
            LiveOperatorApiPanel(city = city, isEnglish = isEnglish)
        }

        if (journey != null && journey.orderedSpots.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("simulation_trigger_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSimulatingNavigation) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.5.dp, if (isSimulatingNavigation) Color(0xFF10B981) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEC4899).copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.4f)),
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
                        // Vertical visual rail indicator
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .fillMaxHeight()
                                .background(
                                    color = legColor.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )

                        // Details block
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (leg.type == LegType.BUS) 
                                    legColor.copy(alpha = 0.08f) 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (leg.type == LegType.BUS) legColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
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
                                            tint = legColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (leg.type == LegType.BUS) (leg.busLineName ?: (if (isEnglish) "Bus" else "Autobuz")) else (if (isEnglish) "Walking" else "Plimbare pe Jos"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (leg.type == LegType.BUS) legColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(legColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${leg.durationMinutes} min",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (leg.type == LegType.BUS) legColor else MaterialTheme.colorScheme.onSurfaceVariant
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
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
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (destinationIndex + 1).toString(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = spot.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
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
                                
                                // Visual Hour Interval Tag
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$arrivalStr - $departureStr",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
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
    isEnglish: Boolean = false
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
    
    // Simulated live vehicles data
    var liveVehicles by remember(city) { mutableStateOf(TransportApiEngine.simulateLiveVehicles(city)) }
    
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
    
    // Timer for refreshing live vehicle speeds/occupancy/delays slightly
    LaunchedEffect(city) {
        while(true) {
            kotlinx.coroutines.delay(10000) // update every 10s
            liveVehicles = TransportApiEngine.simulateLiveVehicles(city)
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Deep Slate background
        border = BorderStroke(1.dp, Color(0xFF334155))
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(180.dp)
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
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
    isEnglish: Boolean = false
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isEnglish) "Gemini is compiling optimized response..." else "Gemini scrie răspunsul optimizat...",
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
                    text = if (isEnglish) "Gemini AI Tour Guide" else "Ghid Turistic AI Gemini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEnglish) "Let Gemini AI analyze your selected spots and compile a full-day optimized schedule with estimated travel times, cost approximations, transit recommendations and local tips!"
                           else "Lăsați AI-ul Gemini să analizeze punctele tale alese și să compileze o descriere a întregii zile cu ore estimate, recomandări de autobuze optime din mers și detalii specifice fiecărui obiectiv!",
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
                    Text(if (isEnglish) "Generate AI Route with Gemini" else "Generează Traseu AI cu Gemini")
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
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
    isEnglish: Boolean = false
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            items(itinerarySpots) { s ->
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
                    TextButton(
                        onClick = onClearLogs,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isEnglish) "Clear Report" else "Șterge Raport", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (testingLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
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
            items(testingLogs) { log ->
                val timeFormatted = remember(log.timestamp) {
                    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(log.timestamp))
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
    isEnglish: Boolean = false
) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(itineraries) { record ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
}

@Composable
fun SettingsDialog(
    show: Boolean,
    onDismiss: () -> Unit,
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
    onLanguageToggle: (Boolean) -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
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
                                "OSM Standard", 
                                "OSM Humanitarian", 
                                "OSM French", 
                                "Slate Neon", 
                                "Cyberpunk", 
                                "Muted Gray", 
                                "Ocean Breeze"
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
        }
    )
}
