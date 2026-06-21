const fs = require('fs');
const interactiveMapPath = 'app/src/main/java/com/example/ui/components/InteractiveMap.kt';
const googleMapPath = 'app/src/main/java/com/example/ui/components/GoogleMapLayer.kt';

let content = fs.readFileSync(interactiveMapPath, 'utf8');
const lines = content.split('\n');

const startGoogle = 495; // line number is 496
const endGoogle = 745;   // }

// WAIT: line indexing array: 
// 496 in 1-based is index 495
// 746 in 1-based is index 745

let googleLines = lines.slice(startGoogle, endGoogle + 1);

// Inside googleLines, change `selectedStationForDetails = it; selectedSpotForDetails = null`
googleLines = googleLines.map(line => {
    let replaced = line.replace('selectedStationForDetails = it', 'onStationClick(it)');
    replaced = replaced.replace('selectedSpotForDetails = null', '');
    replaced = replaced.replace('selectedSpotForDetails = it', 'onSpotClick(it)');
    replaced = replaced.replace('selectedStationForDetails = null', '');
    return replaced;
});

const newComponent = `package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.TouristSpot
import com.example.domain.BusLine
import com.example.domain.BusStation
import com.example.domain.LegType
import com.example.domain.LiveTransitVehicle
import com.example.domain.OptimizedJourney
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun GoogleMapLayer(
    cameraPositionState: CameraPositionState,
    city: String,
    spots: List<TouristSpot>,
    customStartSpot: TouristSpot?,
    stations: List<BusStation>,
    lines: List<BusLine>,
    journey: OptimizedJourney?,
    isStationsVisible: Boolean,
    isTransitLinesVisible: Boolean,
    mapColorSchemeStyle: String,
    isSimulatingNavigation: Boolean,
    activeNavigationLegIndex: Int,
    navigationProgressFraction: Float,
    userGpsLocation: Pair<Double, Double>?,
    liveTransitVehicles: List<LiveTransitVehicle>,
    activeTrackingVehicle: LiveTransitVehicle?,
    onStationClick: (BusStation) -> Unit,
    onSpotClick: (TouristSpot) -> Unit,
    onMapClick: (Double, Double) -> Unit,
    isEnglish: Boolean,
    transitLineRoads: Map<String, List<Pair<Double, Double>>>,
    journeyLegRoads: Map<String, List<Pair<Double, Double>>>,
    getSpotCategoryEmoji: (String) -> String
) {
${googleLines.join('\n')}
}
`;

fs.writeFileSync(googleMapPath, newComponent);

const callReplacement = `            GoogleMapLayer(
                cameraPositionState = cameraPositionState,
                city = city,
                spots = spots,
                customStartSpot = customStartSpot,
                stations = stations,
                lines = lines,
                journey = journey,
                isStationsVisible = isStationsVisible,
                isTransitLinesVisible = isTransitLinesVisible,
                mapColorSchemeStyle = mapColorSchemeStyle,
                isSimulatingNavigation = isSimulatingNavigation,
                activeNavigationLegIndex = activeNavigationLegIndex,
                navigationProgressFraction = navigationProgressFraction,
                userGpsLocation = userGpsLocation,
                liveTransitVehicles = liveTransitVehicles,
                activeTrackingVehicle = activeTrackingVehicle,
                onStationClick = { selectedStationForDetails = it; selectedSpotForDetails = null },
                onSpotClick = { 
                    onSpotClick(it)
                    selectedSpotForDetails = it
                    selectedStationForDetails = null
                },
                onMapClick = { lat, lng -> onMapTap(lat, lng) },
                isEnglish = isEnglish,
                transitLineRoads = transitLineRoads,
                journeyLegRoads = journeyLegRoads,
                getSpotCategoryEmoji = { getSpotCategoryEmoji(it) }
            )`;

const newInteractiveMapLines = [
    ...lines.slice(0, startGoogle),
    callReplacement,
    ...lines.slice(endGoogle + 1)
];

fs.writeFileSync(interactiveMapPath, newInteractiveMapLines.join('\n'));
console.log("Replaced GoogleMapLayer!");
