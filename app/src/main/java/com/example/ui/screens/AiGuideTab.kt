package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AiGuideTab(
    recommendationText: String,
    isLoading: Boolean,
    onCallAi: () -> Unit,
    isEnglish: Boolean = false,
    aiProvider: String = "GEMINI",
    appSkin: String = "MODERN"
) {
    LaunchedEffect(recommendationText) {
        if (recommendationText.isEmpty() && !isLoading) {
            onCallAi()
        }
    }
    val providerName = when (aiProvider) {
        "OPENAI" -> "OpenAI"
        "OPENROUTER" -> "OpenRouter"
        else -> "Gemini"
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isEnglish) "$providerName is compiling optimized response..." else "$providerName scrie răspunsul optimizat...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (recommendationText.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = Color(0xFFEAB308)
                )
                Text(
                    text = if (isEnglish) "$providerName AI Tour Guide" else "Ghid Turistic AI $providerName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEnglish) "Let $providerName AI analyze your selected spots and compile a full-day optimized schedule with estimated travel times, cost approximations, transit recommendations and local tips!"
                           else "Lăsați AI-ul $providerName să analizeze punctele tale alese și să compileze o descriere a întregii zile cu ore estimate, recomandări de autobuze optime din mers și detalii specifice fiecărui obiectiv!",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onCallAi,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("request_ai_button")
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEnglish) "Generate AI Itinerary with $providerName" else "Generează Traseu AI cu $providerName")
                }
            }
        }
        return
    }

    // Scrollable AI Markdowns Output
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308))
                    Text(
                        text = if (isEnglish) "Real-Time AI Guide & Tips" else "Sfaturi și Ghid AI Real-Time",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onCallAi,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("regenerate_ai_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(if (isEnglish) "Refresh" else "Actualizează", fontSize = 10.sp)
                }
            }
        }

        item {
            val primaryColor = MaterialTheme.colorScheme.primary
            val annotatedRecommendationText = remember(recommendationText, primaryColor) {
                parseMarkdownToAnnotatedString(recommendationText, primaryColor)
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (appSkin == "VINTAGE_RPG") Color(0xFF261910) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = if (appSkin == "VINTAGE_RPG") {
                    BorderStroke(1.8.dp, Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF8C6D45))))
                } else null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = annotatedRecommendationText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun parseMarkdownToAnnotatedString(
    text: String,
    primaryColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            when {
                currentLine.startsWith("### ") -> {
                    val headerText = currentLine.removePrefix("### ")
                    appendInlineFormatted(
                        text = headerText,
                        primaryColor = primaryColor,
                        baseStyle = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryColor
                        )
                    )
                }
                currentLine.startsWith("## ") -> {
                    val headerText = currentLine.removePrefix("## ")
                    appendInlineFormatted(
                        text = headerText,
                        primaryColor = primaryColor,
                        baseStyle = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryColor
                        )
                    )
                }
                currentLine.startsWith("# ") -> {
                    val headerText = currentLine.removePrefix("# ")
                    appendInlineFormatted(
                        text = headerText,
                        primaryColor = primaryColor,
                        baseStyle = SpanStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = primaryColor
                        )
                    )
                }
                currentLine.trim().startsWith("- ") -> {
                    val trimmedLine = currentLine.trim()
                    val leadingSpacesCount = currentLine.length - currentLine.removePrefix(" ").length
                    val indent = " ".repeat(leadingSpacesCount + 1)
                    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                        append("$indent• ")
                    }
                    val itemText = trimmedLine.removePrefix("- ")
                    appendInlineFormatted(
                        text = itemText,
                        primaryColor = primaryColor,
                        baseStyle = SpanStyle()
                    )
                }
                currentLine.trim().startsWith("* ") -> {
                    val trimmedLine = currentLine.trim()
                    val leadingSpacesCount = currentLine.length - currentLine.removePrefix(" ").length
                    val indent = " ".repeat(leadingSpacesCount + 1)
                    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                        append("$indent• ")
                    }
                    val itemText = trimmedLine.removePrefix("* ")
                    appendInlineFormatted(
                        text = itemText,
                        primaryColor = primaryColor,
                        baseStyle = SpanStyle()
                    )
                }
                currentLine.trim().startsWith("• ") -> {
                    val trimmedLine = currentLine.trim()
                    val leadingSpacesCount = currentLine.length - currentLine.removePrefix(" ").length
                    val indent = " ".repeat(leadingSpacesCount + 1)
                    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                        append("$indent• ")
                    }
                    val itemText = trimmedLine.removePrefix("• ")
                    appendInlineFormatted(
                        text = itemText,
                        primaryColor = primaryColor,
                        baseStyle = SpanStyle()
                    )
                }
                else -> {
                    appendInlineFormatted(
                        text = currentLine,
                        primaryColor = primaryColor,
                        baseStyle = SpanStyle()
                    )
                }
            }
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatted(
    text: String,
    primaryColor: Color,
    baseStyle: SpanStyle
) {
    var i = 0
    val len = text.length
    while (i < len) {
        // Double Asterisks Bold
        if (text.startsWith("**", i)) {
            val endIdx = text.indexOf("**", i + 2)
            if (endIdx != -1) {
                withStyle(baseStyle.copy(fontWeight = FontWeight.Bold)) {
                    appendInlineFormatted(text.substring(i + 2, endIdx), primaryColor, baseStyle.copy(fontWeight = FontWeight.Bold))
                }
                i = endIdx + 2
                continue
            }
        }
        // Double Underscores Bold
        if (text.startsWith("__", i)) {
            val endIdx = text.indexOf("__", i + 2)
            if (endIdx != -1) {
                withStyle(baseStyle.copy(fontWeight = FontWeight.Bold)) {
                    appendInlineFormatted(text.substring(i + 2, endIdx), primaryColor, baseStyle.copy(fontWeight = FontWeight.Bold))
                }
                i = endIdx + 2
                continue
            }
        }
        // Backticks Code Block
        if (text.startsWith("`", i)) {
            val endIdx = text.indexOf("`", i + 1)
            if (endIdx != -1) {
                withStyle(baseStyle.copy(fontFamily = FontFamily.Monospace, background = primaryColor.copy(alpha = 0.08f))) {
                    append(text.substring(i + 1, endIdx))
                }
                i = endIdx + 1
                continue
            }
        }
        // Single Asterisk Italic
        if (text.startsWith("*", i)) {
            val endIdx = text.indexOf("*", i + 1)
            if (endIdx != -1) {
                withStyle(baseStyle.copy(fontStyle = FontStyle.Italic)) {
                    appendInlineFormatted(text.substring(i + 1, endIdx), primaryColor, baseStyle.copy(fontStyle = FontStyle.Italic))
                }
                i = endIdx + 1
                continue
            }
        }
        // Single Underscore Italic
        if (text.startsWith("_", i)) {
            val endIdx = text.indexOf("_", i + 1)
            if (endIdx != -1) {
                withStyle(baseStyle.copy(fontStyle = FontStyle.Italic)) {
                    appendInlineFormatted(text.substring(i + 1, endIdx), primaryColor, baseStyle.copy(fontStyle = FontStyle.Italic))
                }
                i = endIdx + 1
                continue
            }
        }

        // Just standard character
        withStyle(baseStyle) {
            append(text[i].toString())
        }
        i++
    }
}
