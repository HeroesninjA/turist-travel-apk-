package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardTabRow(
    selectedDashboardTab: Int,
    onTabSelected: (Int) -> Unit,
    appSkin: String,
    isEnglishLanguage: Boolean
) {
    TabRow(
        selectedTabIndex = selectedDashboardTab,
        containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF1C110A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        indicator = {}, // Hide default ugly thick line indicator
        divider = {}, // Hide standard bottom divider
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .run {
                if (appSkin == "VINTAGE_RPG") {
                    this.border(
                        width = 1.dp,
                        color = Color(0xFF6E4D36),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    this.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
    ) {
        // Tab 0: Attractions Checklist
        val isTab0Selected = selectedDashboardTab == 0
        Tab(
            selected = isTab0Selected,
            onClick = { onTabSelected(0) },
            modifier = Modifier
                .testTag("tab_spots_checklist")
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isTab0Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else Color.Transparent
                )
                .run {
                    if (isTab0Selected && appSkin == "VINTAGE_RPG") {
                        this.border(1.dp, Color(0xFFC5A059), RoundedCornerShape(8.dp))
                    } else if (isTab0Selected) {
                        this.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    } else this
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = if (isTab0Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isEnglishLanguage) "Attractions" else "Atracții",
                    fontSize = 11.sp,
                    fontWeight = if (isTab0Selected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isTab0Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Tab 1: Itinerary Timeline
        val isTab1Selected = selectedDashboardTab == 1
        Tab(
            selected = isTab1Selected,
            onClick = { onTabSelected(1) },
            modifier = Modifier
                .testTag("tab_itinerary_timeline")
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isTab1Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else Color.Transparent
                )
                .run {
                    if (isTab1Selected && appSkin == "VINTAGE_RPG") {
                        this.border(1.dp, Color(0xFFC5A059), RoundedCornerShape(8.dp))
                    } else if (isTab1Selected) {
                        this.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    } else this
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = if (isTab1Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isEnglishLanguage) "Itinerary" else "Itinerar",
                    fontSize = 11.sp,
                    fontWeight = if (isTab1Selected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isTab1Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Tab 2: Testing logs
        val isTab2Selected = selectedDashboardTab == 2
        Tab(
            selected = isTab2Selected,
            onClick = { onTabSelected(2) },
            modifier = Modifier
                .testTag("tab_live_testing")
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isTab2Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFF1E2822) else Color(0xFF10B981).copy(alpha = 0.15f)
                    } else Color.Transparent
                )
                .run {
                    if (isTab2Selected && appSkin == "VINTAGE_RPG") {
                        this.border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                    } else if (isTab2Selected) {
                        this.border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    } else this
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isTab2Selected) {
                        Color(0xFF10B981)
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isEnglishLanguage) "Testing" else "Testare",
                    fontSize = 11.sp,
                    fontWeight = if (isTab2Selected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isTab2Selected) {
                        Color(0xFF10B981)
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Tab 3: AI Guide Recommendation
        val isTab3Selected = selectedDashboardTab == 3
        Tab(
            selected = isTab3Selected,
            onClick = { onTabSelected(3) },
            modifier = Modifier
                .testTag("tab_ai_guide")
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isTab3Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else Color(0xFFEAB308).copy(alpha = 0.15f)
                    } else Color.Transparent
                )
                .run {
                    if (isTab3Selected && appSkin == "VINTAGE_RPG") {
                        this.border(1.dp, Color(0xFFEAB308), RoundedCornerShape(8.dp))
                    } else if (isTab3Selected) {
                        this.border(1.dp, Color(0xFFEAB308).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    } else this
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isTab3Selected) {
                        Color(0xFFEAB308)
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isEnglishLanguage) "AI Guide" else "Ghid AI",
                    fontSize = 11.sp,
                    fontWeight = if (isTab3Selected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isTab3Selected) {
                        Color(0xFFEAB308)
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Tab 4: Saved Local Offline Itineraries
        val isTab4Selected = selectedDashboardTab == 4
        Tab(
            selected = isTab4Selected,
            onClick = { onTabSelected(4) },
            modifier = Modifier
                .testTag("tab_saved_itineraries")
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isTab4Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFF382317) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else Color.Transparent
                )
                .run {
                    if (isTab4Selected && appSkin == "VINTAGE_RPG") {
                        this.border(1.dp, Color(0xFFC5A059), RoundedCornerShape(8.dp))
                    } else if (isTab4Selected) {
                        this.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    } else this
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (isTab4Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isEnglishLanguage) "Saved" else "Salvate",
                    fontSize = 11.sp,
                    fontWeight = if (isTab4Selected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isTab4Selected) {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC5A059) else MaterialTheme.colorScheme.primary
                    } else {
                        if (appSkin == "VINTAGE_RPG") Color(0xFFC4AD85).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
