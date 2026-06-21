package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedItinerary

@Composable
fun SavedItinerariesTab(
    itineraries: List<SavedItinerary>,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    isEnglish: Boolean = false,
    appSkin: String = "MODERN"
) {
    val context = LocalContext.current
    if (itineraries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isEnglish) "No saved itineraries yet." else "Nu ai itinerarii salvate încă.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { showClearAllDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isEnglish) "Clear All" else "Șterge Toate", fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = itineraries,
                key = { record -> record.id }
            ) { record ->
                val memoizedRouteDetails = remember(record.routeDetails, isEnglish) {
                    translateRouteDetailsText(record.routeDetails, isEnglish)
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (appSkin == "VINTAGE_RPG") {
                        BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isEnglish) "Itinerary ${translateCityName(record.city, true)}" else "Itinerar ${record.city}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val hrs = record.totalDurationMinutes / 60
                                val mins = record.totalDurationMinutes % 60
                                Text(
                                    text = if (isEnglish) "${record.spotsCount} objectives ➔ Total: $hrs h $mins min" else "${record.spotsCount} obiective ➔ Durată totală: $hrs h $mins min",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { 
                                        val shareIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, if (isEnglish) "Here is my itinerary for ${record.city}:\n\n$memoizedRouteDetails" else "Aici este itinerarul meu pentru ${record.city}:\n\n$memoizedRouteDetails")
                                            type = "text/plain"
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = if (isEnglish) "Share" else "Distribuie",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onDelete(record.id) },
                                    modifier = Modifier.size(36.dp).testTag("delete_saved_itinerary_${record.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = if (isEnglish) "Delete saved" else "Șterge salvare",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = memoizedRouteDetails,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(if (isEnglish) "Clear All Saved Itineraries?" else "Ștergeți Toate Itinerariile Salvate?") },
            text = { Text(if (isEnglish) "Are you sure you want to delete all saved itineraries? This action cannot be undone." else "Sunteți sigur că doriți să ștergeți toate itinerariile salvate? Această acțiune nu poate fi anulată.") },
            confirmButton = {
                Button(
                    onClick = { 
                        showClearAllDialog = false
                        onClearAll() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isEnglish) "Delete" else "Șterge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "Anulare")
                }
            }
        )
    }
}
