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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdConfig
import com.example.ads.AdMobManager
import com.example.ads.AdStatus
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkCardBackground
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun AdMobDiagnosticsDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val initStatus by AdMobManager.initStatus.collectAsState()
    val bannerStatus by AdMobManager.bannerStatus.collectAsState()
    val interstitialStatus by AdMobManager.interstitialStatus.collectAsState()
    val rewardedStatus by AdMobManager.rewardedStatus.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = NeonDarkCardBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "AdMob Diagnostics",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Notice Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F2B5C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Using Official Google Test IDs\nVerification mode active in real-time.",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // SDK Init Status
                StatusRow(
                    label = "SDK Init Status",
                    status = initStatus,
                    unitId = AdConfig.appId
                )

                // Adaptive Banner Status
                StatusRow(
                    label = "Adaptive Banner",
                    status = bannerStatus,
                    unitId = AdConfig.bannerAdUnitId
                )

                // Interstitial Status
                StatusRow(
                    label = "Interstitial Ad",
                    status = interstitialStatus,
                    unitId = AdConfig.interstitialAdUnitId
                )

                // Rewarded Status
                StatusRow(
                    label = "Rewarded Video Ad",
                    status = rewardedStatus,
                    unitId = AdConfig.rewardedAdUnitId
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Refresh / Open Inspector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            AdMobManager.preloadAds(context)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("diag_reload_ads_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reload Ads", fontSize = 12.sp)
                    }

                    if (activity != null) {
                        Button(
                            onClick = {
                                AdMobManager.openAdInspector(activity) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("diag_inspector_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Inspector", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close", color = NeonCyan)
            }
        }
    )
}

@Composable
private fun StatusRow(
    label: String,
    status: AdStatus,
    unitId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (status) {
                        is AdStatus.Ready -> {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ready", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        is AdStatus.Loading -> {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Loading...", color = NeonCyan, fontSize = 12.sp)
                        }
                        is AdStatus.Failed -> {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Failed", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        is AdStatus.Idle -> {
                            Text("Idle", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (status is AdStatus.Failed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Code ${status.code}: ${status.message}",
                    color = Color(0xFFFF8A8A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ID: $unitId",
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
