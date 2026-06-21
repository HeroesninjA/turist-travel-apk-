package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.LiveTransitVehicle
import com.example.domain.TransportApiEngine
import com.example.domain.TransportOperator

@Composable
fun LiveOperatorApiPanel(
    city: String,
    modifier: Modifier = Modifier,
    isEnglish: Boolean = false,
    liveVehiclesPassed: List<LiveTransitVehicle> = emptyList(),
    onVehicleClick: ((LiveTransitVehicle) -> Unit)? = null,
    appSkin: String = "MODERN"
) {
    val context = LocalContext.current
    val op = remember(city) { TransportApiEngine.getOperatorForCity(city) }
    val opInfo = remember(city) { TransportApiEngine.getOperatorUiInfo(city) }
    val smsConfig = remember(city) { TransportApiEngine.getSmsTicketDetails(city) }
    val alerts = remember(city) { TransportApiEngine.getLiveAlerts(city) }
    
    val translatedAlerts = remember(alerts, isEnglish) {
        alerts.map { alert ->
            alert.copy(
                title = translateAlertText(alert.title, isEnglish),
                message = translateAlertText(alert.message, isEnglish)
            )
        }
    }

    // State to simulate paid ticket right inside the app
    var hasActiveTicket by remember { mutableStateOf(false) }
    var ticketTimeLeft by remember { mutableStateOf(smsConfig.validityMinutes * 60) } // in seconds
    var showAlertsDesc by remember { mutableStateOf(false) }
    
    // Fallback to local simulation if no vehicles passed from the main runner
    var liveVehiclesLocal by remember(city) { mutableStateOf(TransportApiEngine.simulateLiveVehicles(city)) }
    val liveVehicles = if (liveVehiclesPassed.isNotEmpty()) liveVehiclesPassed else liveVehiclesLocal
    
    // Timer for bilet electronic countdown
    LaunchedEffect(hasActiveTicket) {
        if (hasActiveTicket) {
            ticketTimeLeft = smsConfig.validityMinutes * 60
            while (ticketTimeLeft > 0) {
                kotlinx.coroutines.delay(1000)
                ticketTimeLeft--
            }
            hasActiveTicket = false
        }
    }
    
    // Timer for refreshing local live vehicle speeds/occupancy/delays if no global active feed
    LaunchedEffect(city) {
        while(true) {
            kotlinx.coroutines.delay(10000) // update every 10s
            if (liveVehiclesPassed.isEmpty()) {
                liveVehiclesLocal = TransportApiEngine.simulateLiveVehicles(city)
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else Color(0xFF0F172A)
        ),
        border = if (appSkin == "VINTAGE_RPG") {
            BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
        } else {
            BorderStroke(1.dp, Color(0xFF334155))
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opInfo.emoji,
                            fontSize = 18.sp
                        )
                    }
                    Column {
                        Text(
                            text = opInfo.shortName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isEnglish) "API Dispatch Connected" else "Dispecerat API Conectat",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981), // Emerald online indicator
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Pulsing Green feed indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Text(
                        text = "LIVE FEED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Subtitle full name of operator
            Text(
                text = opInfo.fullName,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 1. Alerts Section (Collapsible)
            if (translatedAlerts.isNotEmpty()) {
                Surface(
                    onClick = { showAlertsDesc = !showAlertsDesc },
                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isEnglish) "Active Transit Alerts (${translatedAlerts.size})" else "Alerte de Tranzit Active (${translatedAlerts.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF87171)
                                )
                            }
                            Icon(
                                imageVector = if (showAlertsDesc) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        // Detail text inside
                        AnimatedVisibility(visible = showAlertsDesc) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                translatedAlerts.forEach { alert ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "•", color = Color(0xFFF87171), fontSize = 11.sp)
                                        Column {
                                            Text(
                                                text = alert.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = alert.message,
                                                fontSize = 10.sp,
                                                color = Color(0xFFCBD5E1),
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            
            // 2. LIVE VEHICLES SENSOR (Horizontal Row)
            Text(
                text = if (isEnglish) "🚌 Live GPS Vehicle Positions (${opInfo.shortName})" else "🚌 GPS Live Poziție Vehicule (${opInfo.shortName})",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = liveVehicles,
                    key = { vehicle -> vehicle.id }
                ) { v ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else Color(0xFF1E293B)
                        ),
                        border = if (appSkin == "VINTAGE_RPG") {
                            BorderStroke(1.2.dp, Color(0xFFC5A059).copy(alpha = 0.7f))
                        } else {
                            BorderStroke(1.dp, Color(0xFF475569))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(180.dp)
                            .clickable(enabled = onVehicleClick != null) {
                                onVehicleClick?.invoke(v)
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = v.lineName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF3B82F6)
                                )
                                Text(
                                    text = if (v.delayMinutes > 0) "+${v.delayMinutes} min" else (if (isEnglish) "On time" else "La timp"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (v.delayMinutes > 0) Color(0xFFEAB308) else Color(0xFF10B981)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ID: ${v.id.substringAfterLast("-")}",
                                fontSize = 8.sp,
                                color = Color(0xFF64748B)
                              )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEnglish) "Direction: ${v.direction.replace("Spre", "To")}" else "Direcție: ${v.direction}",
                                fontSize = 9.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${v.speedKmh} km/h • GPS OK",
                                    fontSize = 8.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "${v.occupancyPercentage}%",
                                        fontSize = 8.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEnglish) "📍 Tap to locate on map" else "📍 Apasă pt. localizare",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF43F5E),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(8.dp))
            
            // 3. Ticketing SMS & Wallet Section
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEnglish) "🎟️ Electronic Tickets Wallet" else "🎟️ Portofel Bilete Electronice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
                Text(
                    text = if (isEnglish) "SMS to ${smsConfig.number}" else "SMS la ${smsConfig.number}",
                    fontSize = 10.sp,
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            if (hasActiveTicket) {
                // Active ticket UI
                val minsLeft = ticketTimeLeft / 60
                val secsLeft = ticketTimeLeft % 60
                val countdownFormatted = String.format("%02d:%02d", minsLeft, secsLeft)
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1E2822) else Color(0xFF0284C7).copy(alpha = 0.12f)
                    ),
                    border = if (appSkin == "VINTAGE_RPG") {
                        BorderStroke(1.8.dp, Color(0xFF10B981))
                    } else {
                        BorderStroke(1.dp, Color(0xFF0284C7))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "ACTIVE TICKET: ${opInfo.shortName}" else "BILET ACTIV: ${opInfo.shortName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "VALID" else "VALID",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Countdown Clock
                        Text(
                            text = countdownFormatted,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isEnglish) "TIME REMAINING" else "TIMP RĂMAS",
                            fontSize = 8.sp,
                            color = Color(0xFF94A3B8)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Holographic confirmation code generator
                        val securityCode = remember { "SMS-${opInfo.shortName}-${1000 + kotlin.random.Random.nextInt(9000)}-OK" }
                        Text(
                            text = if (isEnglish) "Control Code: $securityCode" else "Cod Control: $securityCode",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) "Presenting this screen during inspection confirms urban ticket payment." else "Prezentarea acestui ecran la control confirmă plata biletului urban.",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Inactive state: trigger buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = getTranslatedCostExplanation(city, isEnglish, smsConfig.costExplanation),
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 13.sp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                TransportApiEngine.triggerSmsCompose(context, smsConfig)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Send Real SMS" else "Trimite SMS Real",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                hasActiveTicket = true
                            },
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Simulate Purchase" else "Simulează Achiziție",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
