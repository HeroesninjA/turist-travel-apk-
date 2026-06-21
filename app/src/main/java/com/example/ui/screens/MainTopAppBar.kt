package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    isEnglishLanguage: Boolean,
    selectedCity: String,
    isDashboardVisible: Boolean,
    onToggleDashboard: () -> Unit,
    appSkin: String,
    onAppSkinToggle: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "BusTour Optimizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEnglishLanguage) "Smart Transit Planner" else "Planificator de Tranzit Inteligent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clean active city chip badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                        .clickable { onSettingsClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = translateCityName(selectedCity, isEnglishLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Toggle Dashboard Panel button
                FilledIconButton(
                    onClick = onToggleDashboard,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDashboardVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isDashboardVisible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("toggle_dashboard_button")
                ) {
                    Icon(
                        imageVector = if (isDashboardVisible) Icons.Default.KeyboardArrowDown else Icons.Default.Menu,
                        contentDescription = if (isDashboardVisible) "Ascunde panou" else "Afișează panou",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Alternative RPG Style/Skin Toggle button
                FilledIconButton(
                    onClick = onAppSkinToggle,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1E100A) else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("theme_skin_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Alternative Interface",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings Panel button
                FilledIconButton(
                    onClick = onSettingsClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Setări Aplicație",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
