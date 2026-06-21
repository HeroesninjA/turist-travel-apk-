package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.testTag

@Composable
fun PermissionsDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasOverlayPermission: Boolean,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestOverlay: () -> Unit,
    isEnglish: Boolean = false,
    appSkin: String = "MODERN"
) {
    if (!show) return

    val titleText = if (isEnglish) "App Permissions Hub" else "Centru Permisiuni Aplicație"
    val subtitleText = if (isEnglish) {
        "To provide a complete transit tracing and background routing experience, the application operates best with these permissions enabled."
    } else {
        "Pentru a oferi o experiență completă de urmărire a transportului și rutare în fundal, aplicația funcționează optim cu aceste permisiuni activate."
    }

    val closeText = if (isEnglish) "Close Permissions Hub" else "Închide Centrul de Permisiuni"
    val grantAllText = if (isEnglish) "Grant Remaining Permissions" else "Acordă Permisiunile Rămase"

    val isAllGranted = hasLocationPermission && hasNotificationPermission && hasOverlayPermission

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .run {
                    if (appSkin == "VINTAGE_RPG") {
                        this.border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))),
                            shape = RoundedCornerShape(24.dp)
                        )
                    } else this
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isAllGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp)
                )

                // Title
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (appSkin == "VINTAGE_RPG") Color(0xFFD4AF37) else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (appSkin == "VINTAGE_RPG") Color(0xFFCEB89E) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. LOCATION PERMISSION ITEM
                PermissionRowItem(
                    title = if (isEnglish) "🛰️ Route GPS Location" else "🛰️ Localizare GPS Traseu",
                    description = if (isEnglish) {
                        "Tracks physical transit routes and positions on live bus layers in real-time."
                    } else {
                        "Urmărește traseele fizice și vehiculele de transport în timp real pe hartă."
                    },
                    isGranted = hasLocationPermission,
                    onRequestClick = onRequestLocation,
                    isEnglish = isEnglish,
                    appSkin = appSkin,
                    testTag = "grant_location_permission"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. NOTIFICATIONS PERMISSION ITEM
                PermissionRowItem(
                    title = if (isEnglish) "🔔 Background Sound Alerts" else "🔔 Alerte Sonore Tranzit",
                    description = if (isEnglish) {
                        "Dispatches background disruptions or station approach audio notifications."
                    } else {
                        "Trmite notificări în fundal pentru apropierea de stații sau perturbări."
                    },
                    isGranted = hasNotificationPermission,
                    onRequestClick = onRequestNotification,
                    isEnglish = isEnglish,
                    appSkin = appSkin,
                    testTag = "grant_notification_permission"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. OVERLAY PERMISSION ITEM
                PermissionRowItem(
                    title = if (isEnglish) "📱 Multi-app Navigation HUD" else "📱 HUD Suprapus pe Ecran",
                    description = if (isEnglish) {
                        "Draws floating mini-HUD panels showing transit progress when in other apps."
                    } else {
                        "Trasează panouri plutitoare cu progresul deplasării peste alte aplicații active."
                    },
                    isGranted = hasOverlayPermission,
                    onRequestClick = onRequestOverlay,
                    isEnglish = isEnglish,
                    appSkin = appSkin,
                    testTag = "grant_overlay_permission"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isAllGranted) {
                        Button(
                            onClick = {
                                if (!hasLocationPermission) onRequestLocation()
                                else if (!hasNotificationPermission) onRequestNotification()
                                else if (!hasOverlayPermission) onRequestOverlay()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary,
                                contentColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_all_permissions_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = grantAllText,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = if (appSkin == "VINTAGE_RPG") {
                            BorderStroke(1.dp, Color(0xFF8C6D45))
                        } else ButtonDefaults.outlinedButtonBorder,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (appSkin == "VINTAGE_RPG") Color(0xFFCEB89E) else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dismiss_permissions_hub"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = closeText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRowItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestClick: () -> Unit,
    isEnglish: Boolean,
    appSkin: String,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appSkin == "VINTAGE_RPG") {
                Color(0xFF261910)
            } else {
                if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isGranted) {
                Color(0xFF10B981).copy(alpha = 0.5f)
            } else {
                if (appSkin == "VINTAGE_RPG") Color(0xFF4C3322) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (appSkin == "VINTAGE_RPG") Color(0xFFD4AF37) else MaterialTheme.colorScheme.onSurface
                    )
                    if (isGranted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Missing",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = if (appSkin == "VINTAGE_RPG") Color(0xFFCEB89E) else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 12.sp
                )
            }

            if (!isGranted) {
                Button(
                    onClick = onRequestClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF8C6D45) else MaterialTheme.colorScheme.primary,
                        contentColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag(testTag)
                ) {
                    Text(
                        text = if (isEnglish) "Enable" else "Activează",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = if (isEnglish) "Active" else "Activ",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
