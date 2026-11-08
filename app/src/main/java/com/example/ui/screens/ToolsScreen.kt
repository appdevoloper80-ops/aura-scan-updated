package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkBackground
import com.example.ui.theme.NeonDarkBorder
import com.example.ui.theme.NeonDarkCardBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateToTranslator: () -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToAiSummary: () -> Unit,
    onNavigateToImagesToPdf: () -> Unit,
    onNavigateToResizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeonDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Document Tools",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeonDarkBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Photo & Text Translator (Auto-detects uploaded photo language, user picks target language)
            item {
                ToolOptionCard(
                    title = "Photo & Text Translator",
                    subtitle = "Auto-detect photo language & translate to Spanish, French, Hindi, German, and 20+ languages.",
                    icon = Icons.Default.Translate,
                    accentColor = Color(0xFF00E5FF),
                    badgeText = "Auto Detect",
                    testTag = "tool_translator_card",
                    onClick = onNavigateToTranslator
                )
            }

            // 2. Smart AI Summary (Works with photo upload, library documents, or text)
            item {
                ToolOptionCard(
                    title = "Smart AI Document Summary",
                    subtitle = "Analyze receipts, bills, invoices, contracts, and notes. Extract totals, dates, and key insights.",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = Color(0xFFFF9100),
                    badgeText = "On-Device AI",
                    testTag = "tool_ai_summary_card",
                    onClick = onNavigateToAiSummary
                )
            }

            // 3. On-Device Text OCR (Auto Detect + Latin, Hindi, Chinese, Japanese, Korean)
            item {
                ToolOptionCard(
                    title = "On-Device Text OCR",
                    subtitle = "Extract text from photos in English/Latin, Hindi, Chinese, Japanese, and Korean.",
                    icon = Icons.Default.TextFields,
                    accentColor = Color(0xFF00BFA5),
                    badgeText = "Multi-Language",
                    testTag = "tool_ocr_card",
                    onClick = onNavigateToOcr
                )
            }

            // 4. Images to PDF Converter
            item {
                ToolOptionCard(
                    title = "Images to PDF Converter",
                    subtitle = "Convert multiple photos into a clean PDF with A4 or Fit-to-image page formats.",
                    icon = Icons.Default.PictureAsPdf,
                    accentColor = Color(0xFF2979FF),
                    badgeText = "Offline",
                    testTag = "tool_images_to_pdf_card",
                    onClick = onNavigateToImagesToPdf
                )
            }

            // 5. Photo & Signature Resizer
            item {
                ToolOptionCard(
                    title = "Photo & Signature Resizer",
                    subtitle = "Resize & compress images to exact KB & pixels for government portals and exams.",
                    icon = Icons.Default.AspectRatio,
                    accentColor = Color(0xFF7C4DFF),
                    badgeText = "Exam Ready",
                    testTag = "tool_resizer_card",
                    onClick = onNavigateToResizer
                )
            }
        }
    }
}

@Composable
private fun ToolOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, NeonDarkBorder, RoundedCornerShape(20.dp))
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
