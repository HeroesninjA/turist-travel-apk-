package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    selectedCity: String,
    onCityChange: (String) -> Unit,
    availableCities: List<String>,
    isIndexingCity: Boolean,
    indexingStatus: String,
    onIndexCity: (String) -> Unit,
    isStationsVisible: Boolean,
    onStationsToggle: (Boolean) -> Unit,
    isTransitLinesVisible: Boolean,
    onTransitLinesToggle: (Boolean) -> Unit,
    simulationSpeed: Float,
    onSimulationSpeedChange: (Float) -> Unit,
    mapColorScheme: String,
    onMapColorSchemeChange: (String) -> Unit,
    isHighResolution: Boolean,
    onHighResolutionToggle: (Boolean) -> Unit,
    isSoundEnabled: Boolean,
    onSoundEnabledToggle: (Boolean) -> Unit,
    isEnglish: Boolean,
    onLanguageToggle: (Boolean) -> Unit,
    openaiApiKey: String,
    onOpenaiApiKeyChange: (String) -> Unit,
    openaiBaseUrl: String,
    onOpenaiBaseUrlChange: (String) -> Unit,
    openaiModel: String,
    onOpenaiModelChange: (String) -> Unit,
    aiProvider: String,
    onAiProviderChange: (String) -> Unit,
    openrouterApiKey: String,
    onOpenrouterApiKeyChange: (String) -> Unit,
    openrouterModel: String,
    onOpenrouterModelChange: (String) -> Unit,
    appSkin: String = "MODERN",
    onAppSkinChange: (String) -> Unit,
    onManagePermissionsClick: () -> Unit = {},
    onFactoryReset: () -> Unit
) {
    if (!show) return

    var showChangelog by remember { mutableStateOf(false) }
    var newCityInput by remember { mutableStateOf("") }

    if (showChangelog) {
        ChangelogDialog(
            isEnglish = isEnglish,
            onDismiss = { showChangelog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = if (appSkin == "VINTAGE_RPG") {
            Modifier.border(
                BorderStroke(2.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45)))),
                shape = RoundedCornerShape(24.dp)
            )
        } else Modifier,
        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.surface,
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
                // SECTION: ACTIVE CITY SELECTOR
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEnglish) "Select Active City" else "Selectează Orașul Activ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val cities = availableCities.map { city ->
                            when (city) {
                                "București" -> Triple("București", "Bucharest", "🚋")
                                "Cluj-Napoca" -> Triple("Cluj-Napoca", "Cluj-Napoca", "🚍")
                                "Brașov" -> Triple("Brașov", "Brasov", "🏔️")
                                "Câmpina" -> Triple("Câmpina", "Campina", "🏰")
                                else -> Triple(city, city, "🌆")
                            }
                        }
                        cities.chunked(2).forEach { rowCities ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCities.forEach { (cityRaw, cityEng, emoji) ->
                                    val isSelected = selectedCity == cityRaw
                                    val displayName = if (isEnglish) cityEng else cityRaw
                                    
                               Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.Transparent
                                                },
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onCityChange(cityRaw) }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = emoji, fontSize = 16.sp)
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AI Dynamic City Auto-Indexing Form
                    Text(
                        text = if (isEnglish) "Index New City (AI Auto-Indexing)" else "Indexează Oraș Nou (Auto-Configurare prin AI)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = newCityInput,
                        onValueChange = { newCityInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("add_new_city_field"),
                        placeholder = {
                            Text(text = if (isEnglish) "e.g. Sinaia, Sibiu, Sighișoara..." else "Ex: Sinaia, Sibiu, Timișoara...")
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (newCityInput.isNotBlank()) {
                                    onIndexCity(newCityInput.trim())
                                    newCityInput = ""
                                }
                            },
                            enabled = !isIndexingCity && newCityInput.isNotBlank(),
                            modifier = Modifier.testTag("add_new_city_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text(text = if (isEnglish) "AI Auto-Configure" else "Auto-Configurare AI")
                        }
                    }

                    if (isIndexingCity) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = indexingStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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

                // SECTION: APP SKIN / THEME
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isEnglish) "Application Theme" else "Tematica Aplicației",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAppSkinChange("MODERN") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appSkin == "MODERN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (appSkin == "MODERN") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEnglish) "Modern UI" else "Interfață Modernă", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        Button(
                            onClick = { onAppSkinChange("VINTAGE_RPG") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEnglish) "Vintage RPG" else "RPG Vintage", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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

                    // Map Resolution Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "High Resolution Map (HD/Retina)" else "Hartă de Rezoluție Înaltă (HD/Retina)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEnglish) "Load double-density 512px map tiles and street details" else "Încarcă dale de hartă la densitate dublă (512px) și detalii stradale",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isHighResolution,
                            onCheckedChange = onHighResolutionToggle,
                            modifier = Modifier.testTag("toggle_high_res_map_switch")
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
                                "Vintage Parchment",
                                "OsmAnd (HD maps + relief)",
                                "OSM Standard", 
                                "OSM Humanitarian", 
                                "OSM French", 
                                "Slate Neon", 
                                "Cyberpunk", 
                                "Muted Gray", 
                                "Ocean Breeze",
                                "Google Maps"
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // SECTION: AI API SETTINGS (OpenAI vs Gemini vs OpenRouter)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEnglish) "AI Service Provider" else "Furnizor Serviciu AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Provider Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isEnglish) "Select AI Engine" else "Selectează Motorul AI",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onAiProviderChange("GEMINI") },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (aiProvider == "GEMINI") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (aiProvider == "GEMINI") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f).testTag("provider_gemini_button")
                            ) {
                                Text("Gemini", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Button(
                                onClick = { onAiProviderChange("OPENAI") },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (aiProvider == "OPENAI") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (aiProvider == "OPENAI") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f).testTag("provider_openai_button")
                            ) {
                                Text("OpenAI", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Button(
                                onClick = { onAiProviderChange("OPENROUTER") },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (aiProvider == "OPENROUTER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (aiProvider == "OPENROUTER") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f).testTag("provider_openrouter_button")
                            ) {
                                Text("OpenRouter", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    // OpenAI Key TextField
                    if (aiProvider == "OPENAI") {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = openaiApiKey,
                            onValueChange = onOpenaiApiKeyChange,
                            label = { Text(if (isEnglish) "OpenAI/Compatible API Key" else "Cheie API OpenAI/Compatibil") },
                            placeholder = { Text("sk-proj-... / any key") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openai_key_input"),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure Key",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = openaiBaseUrl,
                            onValueChange = onOpenaiBaseUrlChange,
                            label = { Text(if (isEnglish) "API Base URL (OpenAI Compatible)" else "URL Bază API (Compatibil OpenAI)") },
                            placeholder = { Text("https://api.openai.com/v1/") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openai_base_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = openaiModel,
                            onValueChange = onOpenaiModelChange,
                            label = { Text(if (isEnglish) "Model ID / Name" else "ID / Nume Model") },
                            placeholder = { Text("gpt-4o-mini") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openai_model_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Presets Row for Quick Insertion
                        Text(
                            text = if (isEnglish) "Provider Quick Presets:" else "Presetări rapide furnizor:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val providerPresets = listOf(
                                Triple("OpenAI (Default)", "https://api.openai.com/v1/", "gpt-4o-mini"),
                                Triple("DeepSeek", "https://api.deepseek.com/v1/", "deepseek-chat"),
                                Triple("Groq", "https://api.groq.com/openai/v1/", "llama-3.1-8b-instant"),
                                Triple("LM Studio (Local)", "http://localhost:1234/v1/", "meta-llama-3-8b-instruct"),
                                Triple("Ollama (Local)", "http://localhost:11434/v1/", "llama3")
                            )
                            providerPresets.forEach { (label, url, model) ->
                                val isActive = openaiBaseUrl == url && openaiModel == model
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { 
                                            onOpenaiBaseUrlChange(url)
                                            onOpenaiModelChange(model)
                                        }
                                        .border(
                                            width = 1.dp,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isEnglish) 
                                "Configure any OpenAI compatible service or custom endpoint. Presets help configure DeepSeek, Groq, or local inference instantly." 
                                else "Configurează orice serviciu compatibil OpenAI sau o adresă locală custom. Presetările ajută la configurarea rapidă pentru DeepSeek, Groq sau rulare locală.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // OpenRouter Block
                    if (aiProvider == "OPENROUTER") {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // API Key Input
                        OutlinedTextField(
                            value = openrouterApiKey,
                            onValueChange = onOpenrouterApiKeyChange,
                            label = { Text(if (isEnglish) "OpenRouter API Key" else "Cheie API OpenRouter") },
                            placeholder = { Text("sk-or-...") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openrouter_key_input"),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure Key",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Model ID Input
                        OutlinedTextField(
                            value = openrouterModel,
                            onValueChange = onOpenrouterModelChange,
                            label = { Text(if (isEnglish) "OpenRouter Model ID" else "ID Model OpenRouter") },
                            placeholder = { Text("google/gemma-2-9b-it:free") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openrouter_model_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Presets Row for Quick Insertion
                        Text(
                            text = if (isEnglish) "Model Quick Presets:" else "Presetări rapide model:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Display clean horizontal scrollable presets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                "gpt-oss-120b",
                                "google/gemma-2-9b-it:free",
                                "meta-llama/llama-3-8b-instruct:free",
                                "mistralai/mistral-7b-instruct:free"
                            )
                            presets.forEach { presetName ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (openrouterModel == presetName) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { onOpenrouterModelChange(presetName) }
                                        .border(
                                            width = 1.dp,
                                            color = if (openrouterModel == presetName) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = presetName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (openrouterModel == presetName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isEnglish) 
                                "Enter your OpenRouter key and select/type a Model. Use 'gpt-oss-120b' or other free models on OpenRouter." 
                                else "Introdu cheia OpenRouter și alege/scrie un Model. Poți folosi 'gpt-oss-120b' sau alte modele gratuite din OpenRouter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // SECTION: SYSTEM PERMISSIONS
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEnglish) "System Permissions" else "Permisiunile Sistemului",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = onManagePermissionsClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF8C6D45) else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_permissions_hub_settings_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Open Permissions Hub (GPS, Alerts, Overlays)" else "Deschide Centrul de Permisiuni (GPS, Alerte, Suprapuneri)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // SECTION: DATA & STORAGE
                var showWipeConfirm by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEnglish) "Data & Storage" else "Date și Stocare",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = { showWipeConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isEnglish) "Factory Reset & Wipe All Data" else "Ștergere totală a datelor", fontWeight = FontWeight.Bold)
                    }

                    if (showWipeConfirm) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isEnglish) "Are you sure? This will permanently delete ALL saved itineraries, field testing logs, and custom spots." else "Ești sigur? Aceasta va șterge DEFINITIV toate itinerariile salvate, rapoartele de testare și punctele personalizate.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { showWipeConfirm = false }, modifier = Modifier.weight(1f)) {
                                        Text(if (isEnglish) "Cancel" else "Anulează")
                                    }
                                    Button(
                                        onClick = {
                                            onFactoryReset()
                                            showWipeConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isEnglish) "WIPE" else "ȘTERGE")
                                    }
                                }
                            }
                        }
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
        },
        dismissButton = {
            TextButton(onClick = { showChangelog = true }) {
                Text(text = if (isEnglish) "Changelog" else "Modificări")
            }
        }
    )
}

@Composable
fun ChangelogDialog(isEnglish: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isEnglish) "Changelog & Versions" else "Versiuni și Modificări", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val versions = listOf(
                    "0.107" to (if (isEnglish) "Added the ability to clear all saved itineraries." else "S-a adăugat posibilitatea de a șterge toate itinerariile salvate."),
                    "0.106" to (if (isEnglish) "Added the ability to share the field testing report." else "S-a adăugat posibilitatea de a partaja raportul de testare pe teren."),
                    "0.105" to (if (isEnglish) "Added the ability to share saved itineraries." else "S-a adăugat funcționalitatea de a partaja itinerarii salvate."),
                    "0.104" to (if (isEnglish) "Added Changelog button in settings and version tracking." else "S-a adăugat butonul de Modificări în setări și urmărirea versiunilor."),
                    "0.103" to (if (isEnglish) "Fix automatic regeneration of AI tips on tab switch." else "S-a reparat regenerarea automată a sfaturilor AI la schimbarea filei."),
                    "0.102" to (if (isEnglish) "Added new AI Providers (OpenRouter) and custom skins." else "S-au adăugat noi furnizori AI (OpenRouter) și teme vizuale."),
                    "0.101" to (if (isEnglish) "Initial beta release with interactive map, routing and AI tips." else "Lansare inițială beta cu hartă interactivă, rute și sfaturi AI.")
                )
                
                versions.forEach { (version, description) ->
                    Text(text = "v$version", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = description, modifier = Modifier.padding(bottom = 12.dp, top = 2.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = if (isEnglish) "Close" else "Închide")
            }
        }
    )
}
