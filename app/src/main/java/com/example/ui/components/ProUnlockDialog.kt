package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdMobManager
import com.example.ads.AdStatus
import com.example.ui.theme.GoldCardGradient
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkCardBackground
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun ProUnlockDialog(
    isProUnlocked: Boolean,
    proRemainingTime: String,
    adsWatchedCount: Int,
    onWatchAdForPro: () -> Unit,
    onInstantUnlock: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val rewardedStatus by AdMobManager.rewardedStatus.collectAsState()
    val isRewardedLoading by AdMobManager.isRewardedLoading.collectAsState()

    // Ensure ad preloading is active when dialog is displayed
    LaunchedEffect(Unit) {
        if (rewardedStatus !is AdStatus.Ready && rewardedStatus !is AdStatus.Loading) {
            AdMobManager.preloadRewarded(context)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = NeonDarkCardBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF332000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("👑", fontSize = 18.sp)
                    }
                }
                Text(
                    text = "AuraScan Pro Access",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GoldCardGradient)
                            .border(1.dp, NeonGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✨", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isProUnlocked) "Pro Status: ACTIVE" else "Unlock AuraScan Pro",
                                    color = Color(0xFFFFE082),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isProUnlocked) {
                                    "Remaining Time: $proRemainingTime • Watch a video ad to extend for 30 minutes!"
                                } else {
                                    "Watch a short video ad to unlock 30 minutes of full Pro access!"
                                },
                                color = Color(0xFFFFD54F).copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Features list
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeatureRow("Unlimited Batch Scanning & High-Res PDF Export")
                    FeatureRow("Unlimited OCR & Multi-language Translation")
                    FeatureRow("AI Document Summarization")
                    FeatureRow("Ad-Free Processing Experience")
                }

                // Rewarded Ad Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4A))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AdMob Rewarded Video Status",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        when (rewardedStatus) {
                            is AdStatus.Ready -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Rewarded Video Ad Ready!",
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            is AdStatus.Loading -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Loading Video Ad...",
                                        color = NeonCyan,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            is AdStatus.Failed -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = (rewardedStatus as AdStatus.Failed).message,
                                        color = Color(0xFFFF8A8A),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            is AdStatus.Idle -> {
                                Text(
                                    text = "Preparing AdMob video ad...",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Main Action Button: Watch Rewarded Video Ad to Unlock/Extend Pro for 30 Mins
                Button(
                    onClick = {
                        if (activity != null) {
                            onWatchAdForPro()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pro_dialog_watch_ad_button"),
                    enabled = !isRewardedLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isRewardedLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading Video Ad...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = if (rewardedStatus is AdStatus.Failed) Icons.Default.Refresh else Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProUnlocked) "Watch Video Ad (+30 Min Extension)" else "Watch Video Ad (Unlock Pro for 30 Min)",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Direct Developer Demo Option
                OutlinedButton(
                    onClick = {
                        onInstantUnlock()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("pro_dialog_instant_unlock_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Bypass Ad (Direct Demo Unlock)",
                        color = NeonGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close", color = TextMuted)
            }
        }
    )
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = TextWhite,
            fontSize = 12.sp
        )
    }
}
