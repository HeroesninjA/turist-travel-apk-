const fs = require('fs');

const path = 'app/src/main/java/com/example/ui/screens/MainScreen.kt';
const content = fs.readFileSync(path, 'utf8');
const lines = content.split('\n');

const replacement = `            MapContainer(
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
            )`;

let startIndex = -1;
let endIndex = -1;

for (let i = 0; i < lines.length; i++) {
    if (lines[i] === '            val mapCornerRadius = if (isDashboardVisible) 24.dp else 0.dp') {
        startIndex = i; // MapContainer Box
    }
    if (startIndex !== -1 && lines[i] === '            // Dashboard Card with Tabs or Collapsed summary bar') {
        endIndex = i - 1; 
        break;
    }
}

if (startIndex !== -1 && endIndex !== -1) {
    const newLines = [
        ...lines.slice(0, startIndex),
        replacement,
        ...lines.slice(endIndex + 1)
    ];
    fs.writeFileSync(path, newLines.join('\n'));
    console.log("Replaced");
} else {
    console.log("Not replaced", startIndex, endIndex);
}
