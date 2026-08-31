package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun VirtualMouseOverlay(
    modifier: Modifier = Modifier,
    onMouseMove: (x: Float, y: Float) -> Unit,
    onMouseDown: (x: Float, y: Float, button: Int) -> Unit,
    onMouseUp: (x: Float, y: Float, button: Int) -> Unit
) {
    var cursorPosition by remember { mutableStateOf(Offset(400f, 300f)) }

    Box(modifier = modifier.fillMaxSize()) {
        // Trackpad area on bottom center in Bento rounded card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .width(260.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(BentoSurfaceHero.copy(alpha = 0.92f))
                .border(1.5.dp, BentoLilac.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (cursorPosition.x + dragAmount.x * 1.5f).coerceIn(0f, 2000f)
                        val newY = (cursorPosition.y + dragAmount.y * 1.5f).coerceIn(0f, 2000f)
                        cursorPosition = Offset(newX, newY)
                        onMouseMove(newX, newY)
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top trackpad guide
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = null,
                            tint = BentoLilac,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Trackpad Area", color = TextSecondary, fontSize = 11.sp)
                    }
                    Text("Drag to aim", color = TextMuted, fontSize = 10.sp)
                }

                // Left & Right Mouse Click Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Left Click
                    var isLeftPressed by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(bottomStart = 18.dp, topStart = 8.dp, bottomEnd = 6.dp, topEnd = 6.dp))
                            .background(if (isLeftPressed) BentoLilac else BentoSurfaceContainer)
                            .border(1.dp, BentoBorder, RoundedCornerShape(bottomStart = 18.dp, topStart = 8.dp, bottomEnd = 6.dp, topEnd = 6.dp))
                            .pointerInput(cursorPosition) {
                                detectTapGestures(
                                    onPress = {
                                        isLeftPressed = true
                                        onMouseDown(cursorPosition.x, cursorPosition.y, 0)
                                        tryAwaitRelease()
                                        isLeftPressed = false
                                        onMouseUp(cursorPosition.x, cursorPosition.y, 0)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LEFT CLICK",
                            color = if (isLeftPressed) BentoLilacDark else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    // Right Click
                    var isRightPressed by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(bottomEnd = 18.dp, topEnd = 8.dp, bottomStart = 6.dp, topStart = 6.dp))
                            .background(if (isRightPressed) BentoLilac else BentoSurfaceContainer)
                            .border(1.dp, BentoBorder, RoundedCornerShape(bottomEnd = 18.dp, topEnd = 8.dp, bottomStart = 6.dp, topStart = 6.dp))
                            .pointerInput(cursorPosition) {
                                detectTapGestures(
                                    onPress = {
                                        isRightPressed = true
                                        onMouseDown(cursorPosition.x, cursorPosition.y, 2)
                                        tryAwaitRelease()
                                        isRightPressed = false
                                        onMouseUp(cursorPosition.x, cursorPosition.y, 2)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RIGHT CLICK",
                            color = if (isRightPressed) BentoLilacDark else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Virtual Cursor Pointer Dot
        Box(
            modifier = Modifier
                .offset(
                    x = (cursorPosition.x / 10).coerceIn(0f, 320f).dp,
                    y = (cursorPosition.y / 10).coerceIn(0f, 600f).dp
                )
                .size(14.dp)
                .clip(CircleShape)
                .background(BentoLilac)
                .border(2.dp, BentoLilacDark, CircleShape)
        )
    }
}
