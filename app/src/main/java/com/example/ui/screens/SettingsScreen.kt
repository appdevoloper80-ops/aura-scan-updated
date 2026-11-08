package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ads.AdaptiveBanner
import com.example.ui.components.AdMobDiagnosticsDialog
import com.example.ui.theme.GoldCardGradient
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkBackground
import com.example.ui.theme.NeonDarkBorder
import com.example.ui.theme.NeonDarkCardBackground
import com.example.ui.theme.NeonGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isProUnlocked: Boolean,
    onUnlockPro: () -> Unit,
    isPrivacyOptionsRequired: Boolean = false,
    onShowPrivacyOptions: () -> Unit = {},
    onOpenAdInspector: () -> Unit = {},
    canRequestAds: Boolean = false,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }
    val context = LocalContext.current

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeonDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                        }
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. AuraScan v1.0.0 Brand Card (Screen 10 Header)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonDarkCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonDarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Neon Icon in rounded frame
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F2B5C))
                                .border(1.5.dp, NeonCyan, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "AuraScan",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "v1.0.0 • On-Device Edition",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Settings Menu Rows (Screen 10)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonDarkCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonDarkBorder)
                ) {
                    Column {
                        SettingsOptionRow(
                            title = "Appearance",
                            subtitle = "Dark Neon OLED Mode",
                            icon = Icons.Default.ColorLens,
                            iconColor = Color(0xFFFFB300),
                            onClick = { showAppearanceDialog = true }
                        )

                        SettingsOptionRow(
                            title = "Accessibility",
                            subtitle = "48dp touch targets, TalkBack ready",
                            icon = Icons.Default.Accessibility,
                            iconColor = NeonCyan,
                            onClick = { showAccessibilityDialog = true }
                        )

                        SettingsOptionRow(
                            title = "Language",
                            subtitle = "English / हिन्दी (Devanagari)",
                            icon = Icons.Default.Language,
                            iconColor = Color(0xFF00BFA5),
                            onClick = { showLanguageDialog = true }
                        )

                        SettingsOptionRow(
                            title = "About App",
                            subtitle = "Privacy Policy & Offline Guarantee",
                            icon = Icons.Default.Info,
                            iconColor = Color(0xFF29B6F6),
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_POLICY_URL))
                                context.startActivity(intent)
                            }
                        )

                        SettingsOptionRow(
                            title = "AdMob Diagnostics",
                            subtitle = "Real-time Ad status & verification",
                            icon = Icons.Default.BugReport,
                            iconColor = NeonCyan,
                            onClick = { showDiagnosticsDialog = true }
                        )

                        if (isPrivacyOptionsRequired) {
                            SettingsOptionRow(
                                title = "Ad privacy choices",
                                subtitle = "Manage your data and ad consent",
                                icon = Icons.Default.Description,
                                iconColor = Color(0xFF90A4AE),
                                onClick = onShowPrivacyOptions
                            )
                        }

                        if (com.example.BuildConfig.DEBUG) {
                            SettingsOptionRow(
                                title = "Ad Inspector",
                                subtitle = "Verify ad units and configuration (Debug)",
                                icon = Icons.Default.BugReport,
                                iconColor = Color(0xFFE91E63),
                                onClick = onOpenAdInspector
                            )
                        }
                    }
                }
            }

            // 3. "Go Premium - Unlock Advanced Features" Card (Screen 10 Golden Banner)
            item {
                Card(
                    onClick = onUnlockPro,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .testTag("settings_go_premium_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GoldCardGradient)
                            .border(1.dp, NeonGold.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF332000),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("👑", fontSize = 22.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isProUnlocked) "Premium Unlocked ✨" else "Go Premium",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFE082)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isProUnlocked) "All features unlocked" else "Unlock Advanced Features",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFFD54F).copy(alpha = 0.8f)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = NeonGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Banner Ad
            item {
                AdaptiveBanner(canRequestAds = canRequestAds)
            }
        }
    }

    // Appearance Dialog
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("Appearance", color = TextWhite) },
            text = {
                Text(
                    text = "AuraScan is rendered in the high-contrast Dark Neon theme as shown in your design mockup. Battery friendly for OLED displays.",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(onClick = { showAppearanceDialog = false }) {
                    Text("OK")
                }
            },
            containerColor = NeonDarkCardBackground
        )
    }

    // Accessibility Dialog
    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text("Accessibility", color = TextWhite) },
            text = {
                Text(
                    text = "All controls feature a minimum 48dp touch target, high-contrast text ratios, and TalkBack content descriptions.",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(onClick = { showAccessibilityDialog = false }) {
                    Text("Got It")
                }
            },
            containerColor = NeonDarkCardBackground
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language", color = TextWhite) },
            text = {
                Column {
                    Text("• English (United States)", color = NeonCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
                    Text("• हिन्दी (Hindi - Devanagari)", color = TextWhite, modifier = Modifier.padding(vertical = 6.dp))
                }
            },
            confirmButton = {
                Button(onClick = { showLanguageDialog = false }) {
                    Text("Confirm")
                }
            },
            containerColor = NeonDarkCardBackground
        )
    }

    // AdMob Diagnostics Dialog
    if (showDiagnosticsDialog) {
        AdMobDiagnosticsDialog(
            onDismissRequest = { showDiagnosticsDialog = false }
        )
    }
}

@Composable
private fun SettingsOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
