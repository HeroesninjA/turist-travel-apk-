const fs = require('fs');
const canvasMapPath = 'app/src/main/java/com/example/ui/components/CustomCanvasMap.kt';

let content = fs.readFileSync(canvasMapPath, 'utf8');

// Fix val re-assign
// Find: scale = kotlin.math.max(0.8f, kotlin.math.min(nextScale, 6.0f))
content = content.replace('scale = kotlin.math.max(0.8f, kotlin.math.min(nextScale, 6.0f))', 'onScaleChange(kotlin.math.max(0.8f, kotlin.math.min(nextScale, 6.0f)))');
content = content.replace('offset += pan', 'onOffsetChange(offset + pan)');

// Fix \n string literals
content = content.replace('val latLng = bounds.toLatLng(mapCoords, width, height)\\n                                            val lat = latLng.first\\n                                            val lng = latLng.second', 'val latLng = bounds.toLatLng(mapCoords, width, height)\n                                            val lat = latLng.first\n                                            val lng = latLng.second');

// Fix onMapTap
content = content.replace('onMapTap(lat, lng)', 'onMapClick(lat, lng)');

fs.writeFileSync(canvasMapPath, content);
