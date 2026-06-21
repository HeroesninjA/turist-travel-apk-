package com.example.ui.screens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun getAppColorScheme(appSkin: String): ColorScheme {
    return if (appSkin == "VINTAGE_RPG") {
        darkColorScheme(
            primary = Color(0xFFC5A059),
            onPrimary = Color(0xFF1E100A),
            secondary = Color(0xFF8C6D45),
            onSecondary = Color(0xFFFCF3D7),
            tertiary = Color(0xFF26A69A),
            background = Color(0xFF1C110A),
            surface = Color(0xFF261910),
            onBackground = Color(0xFFEADBBE),
            onSurface = Color(0xFFEADBBE),
            surfaceVariant = Color(0xFF382317),
            onSurfaceVariant = Color(0xFFC4AD85),
            outline = Color(0xFF6E4D36),
            outlineVariant = Color(0xFF4C3322)
        )
    } else {
        MaterialTheme.colorScheme
    }
}

@Composable
fun getAppTypography(appSkin: String): Typography {
    return if (appSkin == "VINTAGE_RPG") {
        Typography(
            titleLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                letterSpacing = 0.5.sp
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp
            ),
            titleSmall = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                letterSpacing = 0.25.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                letterSpacing = 0.25.sp
            ),
            bodySmall = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 0.25.sp
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            ),
            labelMedium = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            labelSmall = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        )
    } else {
        MaterialTheme.typography
    }
}
