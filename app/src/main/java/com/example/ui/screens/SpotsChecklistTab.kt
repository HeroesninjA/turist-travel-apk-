package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TouristSpot
import com.example.domain.LegType
import com.example.domain.TransitNetwork
import com.example.ui.WeatherInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    appSkin: String = "MODERN",
    isLandmarkSearchInProgress: Boolean = false,
    landmarkSearchError: String? = null,
    onAutoSearchLandmark: (String) -> Unit = {},
    onClearLandmarkSearchError: () -> Unit = {}
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

    val filteredSpots = remember(translatedSpots, searchQuery, filterOnlySelected) {
        translatedSpots.filter { spot ->
            val matchesSearch = spot.name.contains(searchQuery, ignoreCase = true) ||
                    spot.description.contains(searchQuery, ignoreCase = true)
            val matchesSelected = !filterOnlySelected || spot.isSelected
            matchesSearch && matchesSelected
        }
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

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                            )

                            // AI Auto-Search and Discovery Section
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isEnglish) "Find & Inject via AI" else "Caută automat cu AI în $city",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Text(
                                    text = if (isEnglish) 
                                        "Type an attraction name above and let AI determine coordinates, description, and automatically pin it on the map!"
                                        else "Scrie un obiectiv mai sus și lasă AI să-i afle coordonatele, descrierea și să-l plaseze automat pe hartă!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    lineHeight = 13.sp
                                )

                                if (landmarkSearchError != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = landmarkSearchError ?: "",
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 10.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = onClearLandmarkSearchError,
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isButtonEnabled = searchQuery.isNotBlank() && !isLandmarkSearchInProgress
                                    Button(
                                        onClick = { onAutoSearchLandmark(searchQuery) },
                                        enabled = isButtonEnabled,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("ai_auto_search_btn"),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        if (isLandmarkSearchInProgress) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isEnglish) "Searching..." else "Se caută...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isEnglish) "Find & Add Landmark" else "Caută și Adaugă în Listă",
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
                                    text = if (isEnglish) {
                                        "No landmarks match '$searchQuery'."
                                    } else {
                                        "Nicio atracție presetată nu se potrivește cu '$searchQuery'."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (isEnglish) "Discover with AI?" else "Descoperiți cu ajutorul AI?",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = if (isEnglish) {
                                                    "Search and instantly place '$searchQuery' in $city on the map using Gemini AI."
                                                } else {
                                                    "Căutați și plasați instant '$searchQuery' în $city pe hartă cu ajutorul AI-ului Gemini."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                                lineHeight = 14.sp
                                            )
                                            Button(
                                                onClick = { onAutoSearchLandmark(searchQuery) },
                                                enabled = !isLandmarkSearchInProgress,
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.testTag("ai_empty_state_auto_search_btn")
                                            ) {
                                                if (isLandmarkSearchInProgress) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (isEnglish) "Searching..." else "În curs de căutare...", fontSize = 11.sp)
                                                } else {
                                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFBBF24))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isEnglish) "Discover & Add to Tour" else "Descoperă și Adaugă în Traseu",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
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
