const fs = require('fs');
const interactiveMapPath = 'app/src/main/java/com/example/ui/components/InteractiveMap.kt';
const canvasMapPath = 'app/src/main/java/com/example/ui/components/CustomCanvasMap.kt';

let content = fs.readFileSync(interactiveMapPath, 'utf8');
const lines = content.split('\n');

const startIdx = 747; // line 748
const endIdx = 1490; // line 1491

const canvasLines = lines.slice(startIdx, endIdx + 1);

const newComponent = `package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TouristSpot
import com.example.domain.BusLine
import com.example.domain.BusStation
import com.example.domain.LegType
import com.example.domain.LiveTransitVehicle
import com.example.domain.OptimizedJourney
import com.example.domain.TransitNetwork

@Composable
fun CustomCanvasMap(
    city: String,
    bounds: com.example.domain.MapBounds,
    spots: List<TouristSpot>,
    customStartSpot: TouristSpot?,
    stations: List<BusStation>,
    lines: List<BusLine>,
    journey: OptimizedJourney?,
    scaleProvider: () -> Float,
    offsetProvider: () -> Offset,
    scale: Float,
    offset: Offset,
    pulseScale: Float,
    animatedPhase: Float,
    isStationsVisible: Boolean,
    isTransitLinesVisible: Boolean,
    mapColorSchemeStyle: String,
    isHighResolutionMap: Boolean,
    isSimulatingNavigation: Boolean,
    activeNavigationLegIndex: Int,
    navigationProgressFraction: Float,
    userGpsLocation: Pair<Double, Double>?,
    liveTransitVehicles: List<LiveTransitVehicle>,
    activeTrackingVehicle: LiveTransitVehicle?,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onStationClick: (BusStation) -> Unit,
    onSpotClick: (TouristSpot) -> Unit,
    onMapClick: (Double, Double) -> Unit,
    isEnglish: Boolean,
    transitLineRoads: Map<String, List<Pair<Double, Double>>>,
    journeyLegRoads: Map<String, List<Pair<Double, Double>>>,
    getSpotCategoryEmoji: (String) -> String,
    textMeasurer: TextMeasurer
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    
${canvasLines.join('\n')}
}
`;

fs.writeFileSync(canvasMapPath, newComponent);

const callReplacement = `            CustomCanvasMap(
                city = city,
                bounds = bounds,
                spots = spots,
                customStartSpot = customStartSpot,
                stations = stations,
                lines = lines,
                journey = journey,
                scaleProvider = scaleProvider,
                offsetProvider = offsetProvider,
                scale = scale,
                offset = offset,
                pulseScale = pulseScale,
                animatedPhase = animatedPhase,
                isStationsVisible = isStationsVisible,
                isTransitLinesVisible = isTransitLinesVisible,
                mapColorSchemeStyle = mapColorSchemeStyle,
                isHighResolutionMap = isHighResolutionMap,
                isSimulatingNavigation = isSimulatingNavigation,
                activeNavigationLegIndex = activeNavigationLegIndex,
                navigationProgressFraction = navigationProgressFraction,
                userGpsLocation = userGpsLocation,
                liveTransitVehicles = liveTransitVehicles,
                activeTrackingVehicle = activeTrackingVehicle,
                onScaleChange = { scale = it },
                onOffsetChange = { offset = it },
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
                getSpotCategoryEmoji = { getSpotCategoryEmoji(it) },
                textMeasurer = textMeasurer
            )`;

const newInteractiveMapLines = [
    ...lines.slice(0, startIdx),
    callReplacement,
    ...lines.slice(endIdx + 1)
];

fs.writeFileSync(interactiveMapPath, newInteractiveMapLines.join('\n'));
console.log("Replaced CustomCanvasMap!");
