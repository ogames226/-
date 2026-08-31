package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FlashGame
import com.example.ui.theme.*

@Composable
fun GameCard(
    game: FlashGame,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var editedTitle by remember(game.title) { mutableStateOf(game.title) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = BentoSurfaceHero,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (game.isFavorite) BentoLilac.copy(alpha = 0.5f) else BentoBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag("game_card_${game.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bento Style Thumbnail
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (game.fileType == "EXE") BentoSurfaceContainer else BentoSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (game.fileType == "EXE") BentoLilac.copy(alpha = 0.4f) else BentoBorder
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            game.isBuiltIn -> Icons.Default.SportsEsports
                            game.fileType == "EXE" -> Icons.Default.Terminal
                            else -> Icons.Default.Bolt
                        },
                        contentDescription = "Game Type",
                        tint = if (game.fileType == "EXE") BentoLilac else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = game.title.ifEmpty { "Flash Game" },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Type Badge (SWF / EXE)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (game.fileType == "EXE") BentoLilac else BentoSurfaceContainer
                    ) {
                        Text(
                            text = game.fileType,
                            color = if (game.fileType == "EXE") BentoLilacDark else BentoLilac,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = game.description.ifEmpty { "Flash Wasm Application • ${formatBytes(game.fileSize)}" },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (game.lastPlayed > 0) {
                        Text(
                            text = "Played recently",
                            color = BentoGreenPulse,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = if (game.isBuiltIn) "Built-in Classic" else formatBytes(game.fileSize),
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Favorite Button & Options Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (game.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (game.isFavorite) BentoLilac else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(BentoSurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Launch Game", color = TextPrimary) },
                            onClick = {
                                showMenu = false
                                onClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BentoLilac)
                            }
                        )
                        if (!game.isBuiltIn) {
                            DropdownMenuItem(
                                text = { Text("Rename Game", color = TextPrimary) },
                                onClick = {
                                    showMenu = false
                                    editedTitle = game.title
                                    showRenameDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = BentoLilac)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(if (game.isFavorite) "Remove from Favorites" else "Add to Favorites", color = TextPrimary)
                            },
                            onClick = {
                                showMenu = false
                                onToggleFavorite()
                            },
                            leadingIcon = {
                                Icon(
                                    if (game.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = BentoLilac
                                )
                            }
                        )
                        if (!game.isBuiltIn) {
                            HorizontalDivider(color = BentoBorder)
                            DropdownMenuItem(
                                text = { Text("Delete Game", color = Color(0xFFFF5449)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5449))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Edit Game Name", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Game Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoLilac,
                        unfocusedBorderColor = BentoBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedTitle.isNotBlank()) {
                            onRename(editedTitle.trim())
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoLilac, contentColor = BentoLilacDark)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = BentoSurfaceElevated,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
