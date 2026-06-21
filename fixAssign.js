const fs = require('fs');
const canvasMapPath = 'app/src/main/java/com/example/ui/components/CustomCanvasMap.kt';

let content = fs.readFileSync(canvasMapPath, 'utf8');

// Fix val cannot be reassigned
content = content.replace('scale = kotlin.math.max(0.8f, kotlin.math.min(nextScale, 6.0f))', 'onScaleChange(kotlin.math.max(0.8f, kotlin.math.min(nextScale, 6.0f)))');
content = content.replace('offset += pan', 'onOffsetChange(offset + pan)');

fs.writeFileSync(canvasMapPath, content);
