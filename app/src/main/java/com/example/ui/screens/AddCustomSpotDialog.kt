package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddCustomSpotDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    clickedLat: Double,
    clickedLng: Double,
    spotName: String,
    onSpotNameChange: (String) -> Unit,
    spotDesc: String,
    onSpotDescChange: (String) -> Unit,
    spotDuration: String,
    onSpotDurationChange: (String) -> Unit,
    onSave: () -> Unit,
    isEnglish: Boolean = false,
    appSkin: String = "MODERN"
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = if (appSkin == "VINTAGE_RPG") {
            Modifier.border(
                BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45)))),
                shape = RoundedCornerShape(24.dp)
            )
        } else Modifier,
        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(if (isEnglish) "Add New Tourist Spot" else "Adaugă Punct Turistic Nou")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEnglish) 
                        "Enter the details of the selected point directly on the map (Lat: ${String.format("%.4f", clickedLat)}, Lng: ${String.format("%.4f", clickedLng)}):"
                        else "Introdu detaliile punctului selectat direct pe hartă (Lat: ${String.format("%.4f", clickedLat)}, Lng: ${String.format("%.4f", clickedLng)}):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = spotName,
                    onValueChange = onSpotNameChange,
                    label = { Text(if (isEnglish) "Attraction Name *" else "Nume Atracție *") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_spot_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = spotDesc,
                    onValueChange = onSpotDescChange,
                    label = { Text(if (isEnglish) "Short Description" else "Descriere Scurtă") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = spotDuration,
                    onValueChange = onSpotDurationChange,
                    label = { Text(if (isEnglish) "Estimated visit time (minutes)" else "Timp estimat în vizită (minute)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Quick duration chip helpers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("30", "60", "90", "120").forEach { d ->
                        FilterChip(
                            selected = spotDuration == d,
                            onClick = { onSpotDurationChange(d) },
                            label = { Text("$d min") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(if (isEnglish) "Save" else "Salvează")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Cancel" else "Anulează")
            }
        }
    )
}
