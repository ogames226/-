package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.FlashGame
import com.example.ui.components.GameCard
import com.example.ui.theme.*
import com.example.viewmodel.FlashLibraryUiState
import com.example.viewmodel.FlashPlayerViewModel
import com.example.viewmodel.GameFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: FlashPlayerViewModel,
    uiState: FlashLibraryUiState,
    onLaunchGame: (FlashGame) -> Unit,
    onNavigateSettings: () -> Unit
) {
    val context = LocalContext.current
    var showUrlDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // SAF File Picker for .swf and .exe files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var displayName = ""
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            displayName = name
                        }
                    }
                }
            } catch (_: Exception) {}

            if (displayName.isBlank()) {
                val lastPath = uri.lastPathSegment
                if (!lastPath.isNullOrBlank()) {
                    val raw = lastPath.substringAfterLast("/").substringAfterLast(":")
                    if (raw.isNotBlank()) {
                        displayName = raw
                    }
                }
            }

            if (displayName.isBlank()) {
                displayName = "game.swf"
            }

            viewModel.importGameFromUri(uri, displayName)
        }
    }

    Scaffold(
        topBar = {
            // Bento Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BentoCanvas)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RUFFLE ENGINE",
                            color = BentoLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Flash Player",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Search Button in Bento Circular Tile
                        Surface(
                            shape = CircleShape,
                            color = BentoSurfaceContainer,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { isSearchExpanded = !isSearchExpanded }
                                .testTag("search_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Settings Button in Bento Lilac Rounded Tile
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BentoLilac,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateSettings() }
                                .testTag("settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = BentoLilacDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/x-shockwave-flash",
                            "application/x-msdownload",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = BentoLilacDark) },
                text = { Text("Import SWF / EXE", fontWeight = FontWeight.Bold, color = BentoLilacDark) },
                containerColor = BentoLilac,
                contentColor = BentoLilacDark,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("import_file_fab")
            )
        },
        containerColor = BentoCanvas
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 4.dp,
                bottom = paddingValues.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            // Bento Hero Card (Spans full width)
            item {
                val featuredGame = uiState.games.firstOrNull { it.isFavorite } ?: uiState.games.firstOrNull()
                BentoHeroCard(
                    featuredGame = featuredGame,
                    onLaunch = {
                        if (featuredGame != null) {
                            viewModel.launchGame(featuredGame)
                            onLaunchGame(featuredGame)
                        } else {
                            filePickerLauncher.launch(
                                arrayOf("application/x-shockwave-flash", "application/x-msdownload", "application/octet-stream", "*/*")
                            )
                        }
                    },
                    onOpenUrl = { showUrlDialog = true }
                )
            }

            // Bento Metric Tiles (Engine FPS + Key Mapping in a Row)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        BentoFpsCard()
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        BentoKeyMappingCard(onClick = onNavigateSettings)
                    }
                }
            }

            // Bento Library Storage Status Card (Spans full width)
            item {
                BentoStorageCard(
                    totalGames = uiState.games.size,
                    onImportClick = {
                        filePickerLauncher.launch(
                            arrayOf("application/x-shockwave-flash", "application/x-msdownload", "application/octet-stream", "*/*")
                        )
                    },
                    onUrlClick = { showUrlDialog = true }
                )
            }

            // Search Bar & Filter Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnimatedVisibility(visible = isSearchExpanded || uiState.searchQuery.isNotEmpty()) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search Flash games or projectors...", color = TextMuted) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = BentoLilac)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_games_field"),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoLilac,
                                unfocusedBorderColor = BentoBorder,
                                focusedContainerColor = BentoSurfaceElevated,
                                unfocusedContainerColor = BentoSurfaceElevated,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }

                    // Filter chips row in Bento style
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoFilterChip(
                            label = "All Games",
                            isSelected = uiState.selectedFilter == GameFilter.ALL,
                            onClick = { viewModel.setFilter(GameFilter.ALL) },
                            modifier = Modifier.weight(1f)
                        )
                        BentoFilterChip(
                            label = "Favorites",
                            isSelected = uiState.selectedFilter == GameFilter.FAVORITES,
                            onClick = { viewModel.setFilter(GameFilter.FAVORITES) },
                            modifier = Modifier.weight(1f)
                        )
                        BentoFilterChip(
                            label = "SWF",
                            isSelected = uiState.selectedFilter == GameFilter.SWF_ONLY,
                            onClick = { viewModel.setFilter(GameFilter.SWF_ONLY) },
                            modifier = Modifier.weight(0.8f)
                        )
                        BentoFilterChip(
                            label = "EXE",
                            isSelected = uiState.selectedFilter == GameFilter.EXE_PROJECTORS,
                            onClick = { viewModel.setFilter(GameFilter.EXE_PROJECTORS) },
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }

            // Games Grid Section
            if (uiState.games.isEmpty()) {
                item {
                    EmptyStateBentoPlaceholder(
                        searchQuery = uiState.searchQuery,
                        onImportClick = {
                            filePickerLauncher.launch(
                                arrayOf("application/x-shockwave-flash", "application/x-msdownload", "application/octet-stream", "*/*")
                            )
                        }
                    )
                }
            } else {
                items(uiState.games, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        onClick = {
                            viewModel.launchGame(game)
                            onLaunchGame(game)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(game) },
                        onDelete = { viewModel.deleteGame(game) },
                        onRename = { newTitle -> viewModel.updateGameTitle(game, newTitle) }
                    )
                }
            }
        }
    }

    // Alerts and Dialogs
    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("Notice", color = Color(0xFFFF5449), fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("OK", color = BentoLilac)
                }
            },
            containerColor = BentoSurfaceElevated,
            shape = RoundedCornerShape(24.dp)
        )
    }

    uiState.successMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("Success", color = BentoGreenPulse, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("Great", color = BentoLilac)
                }
            },
            containerColor = BentoSurfaceElevated,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showUrlDialog) {
        UrlImportDialog(
            onDismiss = { showUrlDialog = false },
            onImport = { url, title ->
                viewModel.importDirectSwfUrl(url, title)
                showUrlDialog = false
            }
        )
    }
}

/**
 * Hero Bento Card: bg-[#332D41] rounded-[28px] p-5 relative overflow-hidden
 */
@Composable
fun BentoHeroCard(
    featuredGame: FlashGame?,
    onLaunch: () -> Unit,
    onOpenUrl: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = BentoSurfaceHero,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onLaunch() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Watermark Controller Silhouette on bottom right
            Icon(
                imageVector = Icons.Outlined.SportsEsports,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 18.dp, y = 18.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top header of Hero Card
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BentoGreenPulse.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = if (featuredGame != null) "NOW PLAYING" else "FEATURED ENGINE",
                            color = BentoGreenPulse,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Text(
                        text = featuredGame?.title ?: "Classic Flash Arcade",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = featuredGame?.description?.ifEmpty { "Emulating via WebAssembly (Ruffle Engine)" }
                            ?: "Emulating via WebAssembly (ActionScript 1-3 & Projectors)",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                }

                // Bottom Action row of Hero Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BentoLilac,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onLaunch() }
                    ) {
                        Text(
                            text = if (featuredGame != null) "RESUME GAME" else "PLAY ARCADE",
                            color = BentoLilacDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }

                    Text(
                        text = "4:3 Aspect Ratio",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Engine FPS Bento Card: bg-[#49454F] rounded-[28px] p-5
 */
@Composable
fun BentoFpsCard() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = BentoSurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BentoCanvas,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Engine FPS",
                        tint = BentoLilac,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Engine FPS",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "60.0",
                    color = BentoLilac,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Key Mapping Bento Card: bg-[#D0BCFF] rounded-[28px] p-5 text-[#381E72]
 */
@Composable
fun BentoKeyMappingCard(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = BentoLilac,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BentoLilacDark,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Key Mapping",
                        tint = BentoLilac,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Key Mapping",
                    color = BentoLilacDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Standard Overlay",
                    color = BentoLilacDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Bento Storage Card: bg-[#49454F] rounded-[28px] p-5 flex items-center justify-between
 */
@Composable
fun BentoStorageCard(
    totalGames: Int,
    onImportClick: () -> Unit,
    onUrlClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = BentoSurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "LIBRARY STORAGE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$totalGames",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SWF / EXE files",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Overlapping Badges Stack: FLASH & RUFFLE
            Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BentoSurfaceHero,
                    border = androidx.compose.foundation.BorderStroke(2.dp, BentoSurfaceContainer),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "FLASH",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BentoLilac,
                    border = androidx.compose.foundation.BorderStroke(2.dp, BentoSurfaceContainer),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "RUFFLE",
                            color = BentoLilacDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bento Filter Chip
 */
@Composable
fun BentoFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) BentoLilac else BentoSurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) BentoLilac else BentoBorder
        ),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) BentoLilacDark else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyStateBentoPlaceholder(
    searchQuery: String,
    onImportClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = BentoSurfaceHero,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = BentoSurfaceContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = BentoLilac,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = if (searchQuery.isNotEmpty()) "No games matching '$searchQuery'" else "No Flash Games in Library",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Tap 'Import SWF / EXE' to choose Flash movies or projectors from your device storage.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Button(
                onClick = onImportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoLilac,
                    contentColor = BentoLilacDark
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select Flash File (.swf / .exe)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UrlImportDialog(
    onDismiss: () -> Unit,
    onImport: (url: String, title: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BentoSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Load Online Flash Game",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Enter a direct URL to a .swf Shockwave Flash file:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("SWF URL (https://...)") },
                    placeholder = { Text("https://example.com/game.swf") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoLilac,
                        unfocusedBorderColor = BentoBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Game Title (Optional)") },
                    placeholder = { Text("My Online Game") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoLilac,
                        unfocusedBorderColor = BentoBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (url.isNotBlank()) {
                                onImport(url.trim(), title.trim())
                            }
                        },
                        enabled = url.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoLilac,
                            contentColor = BentoLilacDark
                        )
                    ) {
                        Text("Add to Library", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
