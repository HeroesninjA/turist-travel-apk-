const fs = require('fs');
const canvasMapPath = 'app/src/main/java/com/example/ui/components/CustomCanvasMap.kt';

let content = fs.readFileSync(canvasMapPath, 'utf8');

// 1. Fix missing imports
const importsToInject = `
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.screens.translateSpotName
import com.example.ui.components.MapBounds
`;

content = content.replace('import com.example.domain.TransitNetwork', 'import com.example.domain.TransitNetwork' + importsToInject);

// 2. Fix variable types
content = content.replace('bounds: com.example.domain.MapBounds', 'bounds: MapBounds');

// 3. Fix unresolved variables in detectTapGestures
content = content.replace('selectedStationForDetails = clickedStation', 'onStationClick(clickedStation)');
content = content.replace('selectedSpotForDetails = null', ''); // this is done in the callback

content = content.replace('selectedSpotForDetails = clickedSpot', 'onSpotClick(clickedSpot)');
content = content.replace('selectedStationForDetails = null', ''); // this is done in the callback

// Also the "toOffset" and "toLatLng" unresolved references... wait, if I import MapBounds from com.example.ui.components.MapBounds, the extensions will be solved since they are inside the class MapBounds itself!

// Fix the Array destructuring in Kotlin for List vs Pair.
// line: val (lat, lng) = bounds.toLatLng(mapCoords, width, height)
// returns Pair<Double, Double>. Pair does not have component1() dynamically if it's ambiguous, but Pair does have component1() natively in Kotlin.
// Wait! `bounds.toLatLng` returns `Pair<Double, Double>`.
// Oh, the error says:
// e: .../CustomCanvasMap.kt:145:62 Function 'component1()' is ambiguous for this expression: ...
// That happens if you import component1 from somewhere else or it's confused.
// I'll just change it to val latLng = bounds.toLatLng(...) \n val lat = latLng.first \n val lng = latLng.second

content = content.replace('val (lat, lng) = bounds.toLatLng(mapCoords, width, height)', 'val latLng = bounds.toLatLng(mapCoords, width, height)\\n                                            val lat = latLng.first\\n                                            val lng = latLng.second');

fs.writeFileSync(canvasMapPath, content);
