package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import com.example.domain.OptimizedJourney
import com.example.data.TestingLog

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
    isEnglish: Boolean = false,
    appSkin: String = "MODERN",
    hasLocationPermission: Boolean = false,
    hasNotificationPermission: Boolean = false,
    hasOverlayPermission: Boolean = false,
    onManagePermissionsClick: () -> Unit = {}
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
        // 0. Permissions Status Card
        item {
            val allGranted = hasLocationPermission && hasNotificationPermission && hasOverlayPermission
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else {
                        if (allGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.03f)
                    }
                ),
                border = BorderStroke(
                    1.2.dp,
                    if (allGranted) Color(0xFF10B981).copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("permissions_summary_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "🔐 Transit App Permissions Status" else "🔐 Status Permisiuni Tranzit",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (appSkin == "VINTAGE_RPG") Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (allGranted) {
                                    if (isEnglish) "Excellent! All transit systems are operational." else "Excelent! Toate sistemele de tranzit sunt active."
                                } else {
                                    if (isEnglish) "Setup is incomplete. Some features are restricted." else "Configurare incompletă. Unele funcții sunt restrictive."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = if (allGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = onManagePermissionsClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF8C6D45) else {
                                    if (allGranted) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.errorContainer
                                },
                                contentColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else {
                                    if (allGranted) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp).testTag("fix_permissions_btn")
                        ) {
                            Text(
                                text = if (isEnglish) "Manage" else "Administrează",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // GPS Indicator
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (hasLocationPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasLocationPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isEnglish) "GPS Location" else "Locație GPS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Notifications Indicator
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (hasNotificationPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasNotificationPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isEnglish) "Alarms/Notif." else "Notificări/Alerte",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Overlay Indicator
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasOverlayPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isEnglish) "Overlay HUD" else "Widget Suprapus",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 1. Tracker Control Header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = if (appSkin == "VINTAGE_RPG") {
                    BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                },
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
                colors = CardDefaults.cardColors(
                    containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surface
                ),
                border = if (appSkin == "VINTAGE_RPG") {
                    BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
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
                            items(
                                items = itinerarySpots,
                                key = { s -> s }
                            ) { s ->
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val context = LocalContext.current
                        TextButton(
                            onClick = {
                                val reportText = testingLogs.joinToString("\n\n") { "📍 ${it.placeName}\n👤 ${it.observerName}\n📝 ${it.note}" }
                                val reportTitle = if (isEnglish) "Field Testing Report for $city:\n\n" else "Raport Testare Teren pentru $city:\n\n"
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, reportTitle + reportText)
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(if (isEnglish) "Share" else "Distribuie", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        TextButton(
                            onClick = onClearLogs,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(if (isEnglish) "Clear" else "Șterge", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (testingLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    ),
                    border = if (appSkin == "VINTAGE_RPG") {
                        BorderStroke(1.2.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                    } else null,
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
            items(
                items = testingLogs,
                key = { log -> log.id }
            ) { log ->
                val timeFormatted = remember(log.timestamp) {
                    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(log.timestamp))
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (appSkin == "VINTAGE_RPG") {
                        BorderStroke(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    },
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
