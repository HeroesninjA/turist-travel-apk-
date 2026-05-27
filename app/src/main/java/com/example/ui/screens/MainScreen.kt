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
import com.example.ui.components.InteractiveMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.IntrinsicSize
import kotlinx.coroutines.launch

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

    // Overlay add spot dialog state
    var showAddSpotDialog by remember { mutableStateOf(false) }
    var clickedLat by remember { mutableStateOf(0.0) }
    var clickedLng by remember { mutableStateOf(0.0) }

    // Dialog form state
    var newSpotName by remember { mutableStateOf("") }
    var newSpotDesc by remember { mutableStateOf("") }
    var newSpotDuration by remember { mutableStateOf("60") } // string for easy editing

    // Active bottom card tab state
    var selectedDashboardTab by remember { mutableStateOf(0) } // 0: Checklist, 1: Timeline, 2: AI Guide, 3: Saved

    var isDashboardVisible by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- SETTINGS STATES ---
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isStationsVisible by rememberSaveable { mutableStateOf(true) }
    var isTransitLinesVisible by rememberSaveable { mutableStateOf(true) }
    var simulationSpeedMultiplier by rememberSaveable { mutableStateOf(1.0f) }
    var mapColorSchemeStyle by rememberSaveable { mutableStateOf("OSM Standard") }
    var isSoundAlertEnabled by rememberSaveable { mutableStateOf(true) }
    var isEnglishLanguage by rememberSaveable { mutableStateOf(false) }

    // --- LIVE GPS LOCATION & ROUTE NAVIGATION SIMULATION ---
    var userGpsLocation by remember(selectedCity) { mutableStateOf<Pair<Double, Double>?>(null) }
    var isSimulatingNavigation by remember { mutableStateOf(false) }
    var activeNavigationLegIndex by remember { mutableStateOf(0) }
    var navigationProgressFraction by remember { mutableStateOf(0f) }

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
                                text = "Planificator de Tranzit Inteligent",
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
                        clickedLat = lat
                        clickedLng = lng
                        newSpotName = ""
                        newSpotDesc = ""
                        newSpotDuration = "60"
                        showAddSpotDialog = true
                    },
                    onSpotClick = { spot ->
                        viewModel.toggleSpotSelection(spot.id, !spot.isSelected)
                        scope.launch {
                            snackbarHostState.showSnackbar("S-a schimbat selectarea pentru: ${spot.name}")
                        }
                    },
                    onSetStartSpot = { spot ->
                        viewModel.updateCustomStartSpot(spot.latitude, spot.longitude, spot.name)
                        scope.launch {
                            snackbarHostState.showSnackbar("Punct de pornire schimbat la: ${spot.name}")
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
                                snackbarHostState.showSnackbar("🛰️ Semnal GPS Activ pe Hartă!")
                            }
                        } else {
                            userGpsLocation = null
                            isSimulatingNavigation = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Gps Deconectat.")
                            }
                        }
                    },
                    onUseGpsAsStart = { lat, lng ->
                        viewModel.updateCustomStartSpot(lat, lng, "Locație GPS Ta")
                        scope.launch {
                            snackbarHostState.showSnackbar("🗺️ Pornire setată din locația ta GPS reală!")
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
                                snackbarHostState.showSnackbar("🎉 Felicitări! Ai finalizat cu succes întregul traseu!")
                            }
                        } else {
                            activeNavigationLegIndex++
                            navigationProgressFraction = 0f
                            scope.launch {
                                snackbarHostState.showSnackbar("Etapă finalizată! Se trece la următoarea direcție...")
                            }
                        }
                    },
                    onStopSimulationClick = {
                        isSimulatingNavigation = false
                        activeNavigationLegIndex = 0
                        navigationProgressFraction = 0f
                        scope.launch {
                            snackbarHostState.showSnackbar("Navigație oprită.")
                        }
                    },
                    isStationsVisible = isStationsVisible,
                    isTransitLinesVisible = isTransitLinesVisible,
                    mapColorSchemeStyle = mapColorSchemeStyle
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
                            text = "Pornire: ${start.name}",
                            color = Color(0xFFF472B6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dashboard Card with Tabs or Collapsed summary bar
            if (isDashboardVisible) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f)
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
                                        snackbarHostState.showSnackbar("Traseul a fost salvat offline!")
                                    }
                                }
                            },
                            isCollapsed = false,
                            onToggleDashboard = { isDashboardVisible = !isDashboardVisible },
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
                                    snackbarHostState.showSnackbar("🚀 Ghidajul traseului a pornit! Urmărește harta.")
                                }
                            },
                            onStopSimulationClick = {
                                isSimulatingNavigation = false
                                activeNavigationLegIndex = 0
                                navigationProgressFraction = 0f
                                scope.launch {
                                    snackbarHostState.showSnackbar("Ghidaj oprit.")
                                }
                            }
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
                                        text = "Atracții",
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
                                        text = "Itinerar",
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
                                        tint = if (selectedDashboardTab == 2) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ghid AI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDashboardTab == 2) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Tab(
                                selected = selectedDashboardTab == 3,
                                onClick = { selectedDashboardTab = 3 },
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
                                        tint = if (selectedDashboardTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Salvate",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDashboardTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        onDelete = { id -> viewModel.deleteSpot(id) },
                                        onClearCustom = { viewModel.clearAllCustomCurrentCity() },
                                        onSetStartClick = { spot ->
                                            viewModel.updateCustomStartSpot(spot.latitude, spot.longitude, spot.name)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Punct de pornire schimbat la: ${spot.name}")
                                            }
                                        }
                                    )
                                    1 -> TimelineTab(
                                        journey = optimizedJourney,
                                        startLabel = customStartSpot?.name ?: TransitNetwork.getStartSpot(selectedCity).name,
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
                                                snackbarHostState.showSnackbar("🚀 Ghidajul traseului a pornit! Urmărește harta.")
                                            }
                                        },
                                        onStopSimulationClick = {
                                            isSimulatingNavigation = false
                                            activeNavigationLegIndex = 0
                                            navigationProgressFraction = 0f
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Ghidaj oprit.")
                                            }
                                        }
                                    )
                                    2 -> AiGuideTab(
                                        recommendationText = aiRecommendation,
                                        isLoading = isAiLoading,
                                        onCallAi = { viewModel.askGeminiForItinerary() }
                                    )
                                    3 -> SavedItinerariesTab(
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
                                    snackbarHostState.showSnackbar("Traseul a fost salvat offline!")
                                }
                            }
                        },
                        isCollapsed = true,
                        onToggleDashboard = { isDashboardVisible = !isDashboardVisible },
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
                                snackbarHostState.showSnackbar("🚀 Ghidajul traseului a pornit! Urmărește harta.")
                            }
                        },
                        onStopSimulationClick = {
                            isSimulatingNavigation = false
                            activeNavigationLegIndex = 0
                            navigationProgressFraction = 0f
                            scope.launch {
                                snackbarHostState.showSnackbar("Ghidaj oprit.")
                            }
                        }
                    )
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
        isSoundEnabled = isSoundAlertEnabled,
        onSoundEnabledToggle = { isSoundAlertEnabled = it },
        isEnglish = isEnglishLanguage,
        onLanguageToggle = { isEnglishLanguage = it }
    )
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
    onStopSimulationClick: () -> Unit = {}
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
                            text = "RUTĂ OPTIMIZATĂ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = "🏛️ $count spot • 🕒 ${hours}h ${minutes}m • 🎫 $fare Lei",
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
                            text = "Alege atracții în $city",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Selectează obiective din tab.",
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
                                contentDescription = if (isSimulatingNavigation) "Oprește ghidajul" else "Pornește ghidajul",
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
                                contentDescription = "Salvează online",
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
                                contentDescription = if (isCollapsed) "Afișează panoul" else "Ascunde panoul",
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
    onDelete: (Long) -> Unit,
    onClearCustom: () -> Unit,
    onSetStartClick: (TouristSpot) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterOnlySelected by remember { mutableStateOf(false) }

    val filteredSpots = spots.filter { spot ->
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
                text = "Puncte Disponibile în $city",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val customCount = spots.count { it.isCustom }
            if (customCount > 0) {
                TextButton(
                    onClick = onClearCustom,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Șterge noi", fontSize = 11.sp)
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
                placeholder = { Text("Caută atracții...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Șterge", modifier = Modifier.size(16.dp))
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
                label = { Text("Doar selectate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                            text = "Nicio atracție găsită",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Încearcă să modifici textul de căutare sau definește un punct personalizat atingând direct pe harta de mai sus!",
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
                items(filteredSpots) { spot ->
                    val isCurrentStart = (customStartSpot?.id == spot.id) || 
                        (customStartSpot == null && spot.name == TransitNetwork.getStartSpot(city).name)

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
                                            Text("PERS.", fontSize = 8.sp, color = Color(0xFFEAB308), fontWeight = FontWeight.Bold)
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
                                        text = "Durată vizită: ${spot.visitDurationMinutes} min",
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
                                        contentDescription = "Setează ca pornire",
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
                                            contentDescription = "Șterge atracție",
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
    onStopSimulationClick: () -> Unit = {}
) {
    // Dynamic timetable hour calculation
    val startMin = 540 // 09:00 AM
    var rollingMin = startMin
    val timelineTimes = remember(journey) {
        val list = mutableListOf<Pair<Int, Int>>() // Arrival, Departure
        val legsCount = journey?.legs?.size ?: 0
        for (i in 0 until legsCount) {
            val leg = journey!!.legs[i]
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
            LiveOperatorApiPanel(city = city)
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
                                text = if (isSimulatingNavigation) "🧭 Navigație Activă" else "🗺️ Pornește în Traseu",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSimulatingNavigation) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSimulatingNavigation) "Urmărește trackerul GPS live cum traversează traseul." else "Pornește ghidul interactiv pas cu pas pentru acest itinerar.",
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
                                text = if (isSimulatingNavigation) "Oprește" else "Start",
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
                            text = "Niciun traseu generat încă",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Alege puncte turistice din tab-ul 'Atracții' pentru a calcula automat un traseu optimizat cu etapele de tranzit!",
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
                                    text = "START: $startLabel",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Punct inițial de plecare",
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
                                text = "Plecare: $startHour",
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
                                            text = if (leg.type == LegType.BUS) (leg.busLineName ?: "Autobuz") else "Plimbare pe Jos",
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
                                    text = leg.directions,
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
                                            text = "LIVE: SOSIRE ÎN ${arrivals.first()} MIN (Următoarele: ${arrivals.drop(1).joinToString(", ")} min)",
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
                        val spot = journey.orderedSpots[destinationIndex]
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
                                                text = "Vizită de: ${spot.visitDurationMinutes} min",
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val op = remember(city) { TransportApiEngine.getOperatorForCity(city) }
    val smsConfig = remember(city) { TransportApiEngine.getSmsTicketDetails(city) }
    val alerts = remember(city) { TransportApiEngine.getLiveAlerts(city) }
    
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
                            text = "Dispecerat API Conectat",
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
            if (alerts.isNotEmpty()) {
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
                                    text = "Alerte de Tranzit Active (${alerts.size})",
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
                                alerts.forEach { alert ->
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
                text = "🚌 GPS Live Poziție Vehicule (${op.shortName})",
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
                                    text = if (v.delayMinutes > 0) "+${v.delayMinutes} min" else "La timp",
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
                                text = "Direcție: ${v.direction}",
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
                    text = "🎟️ Portofel Bilete Electronice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
                Text(
                    text = "SMS la ${smsConfig.number}",
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
                                text = "BILET ACTIV: ${op.shortName}",
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
                                    text = "VALID",
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
                            text = "TIMP RĂMAS",
                            fontSize = 8.sp,
                            color = Color(0xFF94A3B8)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Holographic confirmation code generator
                        val securityCode = remember { "SMS-${op.shortName}-${1000 + kotlin.random.Random.nextInt(9000)}-OK" }
                        Text(
                            text = "Cod Control: $securityCode",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Prezentarea acestui ecran la control confirmă plata biletului urban.",
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
                        text = smsConfig.costExplanation,
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
                                text = "Trimite SMS Real",
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
                                text = "Simulează Achiziție",
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
    onCallAi: () -> Unit
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
                    text = "Gemini scrie răspunsul optimizat...",
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
                    text = "Ghid Turistic AI Gemini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Lăsați AI-ul Gemini să analizeze punctele tale alese și să compileze o descriere a întregii zile cu ore estimate, recomandări de autobuze optime din mers și detalii specifice fiecărui obiectiv!",
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
                    Text("Generează Traseu AI cu Gemini")
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
                        text = "Sfaturi și Ghid AI Real-Time",
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
                    Text("Actualizează", fontSize = 10.sp)
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
fun SavedItinerariesTab(
    itineraries: List<SavedItinerary>,
    onDelete: (Long) -> Unit
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
                    text = "Nu ai itinerarii salvate încă.",
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
                                text = "Itinerar ${record.city}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val hrs = record.totalDurationMinutes / 60
                            val mins = record.totalDurationMinutes % 60
                            Text(
                                text = "${record.spotsCount} obiective ➔ Durată totală: $hrs h $mins min",
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
                                contentDescription = "Șterge salvare",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = record.routeDetails,
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
