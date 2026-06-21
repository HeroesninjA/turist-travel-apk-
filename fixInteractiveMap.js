const fs = require('fs');
const interactiveMapPath = 'app/src/main/java/com/example/ui/components/InteractiveMap.kt';
const googleMapPath = 'app/src/main/java/com/example/ui/components/GoogleMapLayer.kt';

let interactiveContent = fs.readFileSync(interactiveMapPath, 'utf8');
let googleContent = fs.readFileSync(googleMapPath, 'utf8');

const googleLines = googleContent.split('\n');

let bodyStart = googleLines.findIndex(l => l.includes('        if (mapColorSchemeStyle == "Google Maps") {'));
if (bodyStart === -1) {
    bodyStart = googleLines.findIndex(l => l.startsWith('        fun getPointAlongLatLng'));
}

if (bodyStart !== -1) {
   let bodyLines = googleLines.slice(bodyStart, googleLines.length - 2); 
   let bodyStr = bodyLines.join('\n');
    
   // Undo the callbacks
   bodyStr = bodyStr.replace(/onStationClick\(it\)/g, 'selectedStationForDetails = it; selectedSpotForDetails = null');
   bodyStr = bodyStr.replace(/onSpotClick\(it\)/g, 'selectedSpotForDetails = it; selectedStationForDetails = null; onSpotClick(it)');

   const wrapStr = '        if (mapColorSchemeStyle == "Google Maps") {\n' + bodyStr;

   const idxStart = interactiveContent.indexOf('        if (mapColorSchemeStyle == "Google Maps") {');
   const idxEnd = interactiveContent.indexOf('        } else {', idxStart);
   if (idxStart !== -1 && idxEnd !== -1) {
       interactiveContent = interactiveContent.substring(0, idxStart) + wrapStr + '\n' + interactiveContent.substring(idxEnd);
       fs.writeFileSync(interactiveMapPath, interactiveContent);
       console.log("Restored GoogleMap successfully.");
   } else {
       console.log("Not found indexes");
   }
} else {
    console.log("Could not find start.");
}
