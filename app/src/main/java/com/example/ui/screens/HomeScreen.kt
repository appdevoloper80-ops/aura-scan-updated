package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DocumentEntity
import com.example.util.DocumentSharingUtils.exportPdfToUri
import com.example.util.DocumentSharingUtils.shareDocumentPdf
import com.example.ads.AdaptiveBanner
import com.example.ui.theme.AiTileGradient
import com.example.ui.theme.GalleryTileGradient
import com.example.ui.theme.HeroCardGradient
import com.example.ui.theme.LibraryTileGradient
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkBackground
import com.example.ui.theme.NeonDarkBorder
import com.example.ui.theme.NeonDarkCardBackground
import com.example.ui.theme.NeonGold
import com.example.ui.theme.ScanTileGradient
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    documents: List<DocumentEntity>,
    onScanClick: () -> Unit,
    onImportGalleryClick: () -> Unit,
    onAiSummaryClick: () -> Unit,
    onMyLibraryClick: () -> Unit,
    onNavigateToImagesToPdf: () -> Unit,
    onNavigateToResizer: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onRenameDocument: (id: String, newName: String) -> Unit,
    onDeleteDocument: (id: String) -> Unit,
    onUnlockPro: () -> Unit,
    onPdfSaved: () -> Unit = {},
    onShareComplete: () -> Unit = {},
    canRequestAds: Boolean = false,
    proRemainingTime: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var documentToRename by rememberSaveable { mutableStateOf<String?>(null) }
    var renameDefaultName by rememberSaveable { mutableStateOf("") }
    var documentToDelete by rememberSaveable { mutableStateOf<DocumentEntity?>(null) }

    // SAF Save PDF launcher
    var pendingExportPdfPath by rememberSaveable { mutableStateOf<String?>(null) }
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        val path = pendingExportPdfPath
        if (uri != null && path != null) {
            exportPdfToUri(context, File(path), uri)
            onPdfSaved()
        }
        pendingExportPdfPath = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeonDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AuraScan",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    // Golden Crown Pro Action (Screen 2)
                    IconButton(
                        onClick = onUnlockPro,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_crown_pro_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4A3200),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "👑",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeonDarkBackground)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Banner: "Scan Smarter, Do More" (Screen 2 from Showcase)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HeroCardGradient)
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("👑", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (proRemainingTime.isNotEmpty()) "Pro Active ($proRemainingTime)" else "Scan Smarter,",
                                            fontSize = if (proRemainingTime.isNotEmpty()) 15.sp else 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (proRemainingTime.isNotEmpty()) NeonCyan else TextWhite
                                        )
                                    }
                                    if (proRemainingTime.isEmpty()) {
                                        Text(
                                            text = "Do More",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Turn paper into productivity with AI ✨",
                                        fontSize = 13.sp,
                                        color = TextMuted,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 3D Isometric Neon Document Illustration
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFF0F2B5C))
                                        .border(1.5.dp, NeonCyan, RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. 4 Dashboard Action Tiles (2x2 Grid matching Screen 2)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Row 1: Scan Document & Import Gallery
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardTile(
                                title = "Scan\nDocument",
                                icon = Icons.Default.CameraAlt,
                                gradient = ScanTileGradient,
                                modifier = Modifier.weight(1f),
                                testTag = "home_tile_scan",
                                onClick = onScanClick
                            )

                            DashboardTile(
                                title = "Import\nGallery",
                                icon = Icons.Default.Image,
                                gradient = GalleryTileGradient,
                                modifier = Modifier.weight(1f),
                                testTag = "home_tile_gallery",
                                onClick = onImportGalleryClick
                            )
                        }

                        // Row 2: AI Summary & My Library
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardTile(
                                title = "AI Summary",
                                icon = Icons.Default.AutoAwesome,
                                gradient = AiTileGradient,
                                modifier = Modifier.weight(1f),
                                testTag = "home_tile_ai",
                                onClick = onAiSummaryClick
                            )

                            DashboardTile(
                                title = "My Library",
                                icon = Icons.Default.Folder,
                                gradient = LibraryTileGradient,
                                modifier = Modifier.weight(1f),
                                testTag = "home_tile_library",
                                onClick = onMyLibraryClick
                            )
                        }
                    }
                }

                // 3. Recent Documents Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Scans",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        if (documents.isNotEmpty()) {
                            Text(
                                text = "View All",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onMyLibraryClick() }
                            )
                        }
                    }
                }

                // Recent Documents List / Empty State
                if (documents.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NeonDarkCardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonDarkBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = NeonCyan.copy(alpha = 0.7f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No scans yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Scan Document' to create your first scan",
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(documents.take(5), key = { it.id }) { doc ->
                        RecentScanItem(
                            doc = doc,
                            onClick = { onDocumentClick(doc.id) },
                            onRename = {
                                documentToRename = doc.id
                                renameDefaultName = doc.name
                            },
                            onShare = { 
                                shareDocumentPdf(context, doc)
                                onShareComplete()
                            },
                            onSaveToDevice = {
                                pendingExportPdfPath = doc.pdfPath
                                savePdfLauncher.launch("${doc.name.replace(" ", "_")}.pdf")
                            },
                            onDelete = { documentToDelete = doc }
                        )
                    }
                }
            }

            // Bottom Banner Ad (Fixed outside scroll)
            AdaptiveBanner(
                canRequestAds = canRequestAds,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Rename Dialog
    if (documentToRename != null) {
        var newNameText by rememberSaveable { mutableStateOf(renameDefaultName) }
        AlertDialog(
            onDismissRequest = { documentToRename = null },
            title = { Text("Rename Scan", color = TextWhite) },
            text = {
                OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = documentToRename
                        if (id != null && newNameText.isNotBlank()) {
                            onRenameDocument(id, newNameText)
                        }
                        documentToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToRename = null }) {
                    Text("Cancel")
                }
            },
            containerColor = NeonDarkCardBackground
        )
    }

    // Delete Dialog
    documentToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text("Delete Document?", color = TextWhite) },
            text = { Text("Permanently delete \"${doc.name}\"?", color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDocument(doc.id)
                        documentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = NeonDarkCardBackground
        )
    }
}

@Composable
private fun DashboardTile(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(20.dp))
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun RecentScanItem(
    doc: DocumentEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val firstPage = doc.getPagePaths().firstOrNull()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
    val dateText = remember(doc.createdAt) { dateFormat.format(Date(doc.createdAt)) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonDarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (firstPage != null && File(firstPage).exists()) {
                    AsyncImage(
                        model = File(firstPage),
                        contentDescription = doc.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateText,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${doc.pageCount}p",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextMuted
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF131D33))
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = TextWhite) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextWhite) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = TextWhite) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = TextWhite) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save to device", color = TextWhite) },
                        leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null, tint = TextWhite) },
                        onClick = {
                            menuExpanded = false
                            onSaveToDevice()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFFF5252)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
