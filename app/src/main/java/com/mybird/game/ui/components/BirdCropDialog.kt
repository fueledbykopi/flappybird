package com.mybird.game.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mybird.game.data.BirdCropTransform
import com.mybird.game.data.CustomMediaManager
import com.mybird.game.data.PendingBirdImage
import kotlin.math.min
import kotlin.math.roundToInt

private val CropOutline = Color(0xFF4A3214)
private val CropCream = Color(0xFFFFF8E7)
private val CropGreen = Color(0xFF4CAF50)
private val CropOrange = Color(0xFFFF9800)
private const val CROP_CIRCLE_FRACTION = 0.66f

/**
 * Interactive circular crop/fit editor shown after picking a custom bird image.
 *
 * Lets the user pan (1 finger), pinch-to-zoom (2 fingers), fine-tune with the zoom slider,
 * rotate 90°, and preview the exact circular cutout with the game's wing and beak before
 * confirming and applying the character.
 */
@Composable
fun BirdCropDialog(
    pendingBird: PendingBirdImage,
    isSaving: Boolean,
    onRotate90: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: (BirdCropTransform) -> Unit
) {
    val previewBitmap = pendingBird.previewBitmap
    val bitmapWidth = previewBitmap.width.coerceAtLeast(1)
    val bitmapHeight = previewBitmap.height.coerceAtLeast(1)

    var zoom by remember { mutableFloatStateOf(1f) }
    var panNormX by remember { mutableFloatStateOf(0f) }
    var panNormY by remember { mutableFloatStateOf(0f) }

    // Reset framing cleanly whenever a new photo or 90-degree rotated bitmap arrives
    LaunchedEffect(pendingBird) {
        zoom = 1f
        panNormX = 0f
        panNormY = 0f
    }

    val maskPath = remember { Path() }
    val beakPath = remember { Path() }

    Dialog(
        onDismissRequest = { if (!isSaving) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = CropCream,
            border = BorderStroke(3.dp, CropOutline),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Paskan Karakter Burung",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = CropOutline
                        )
                        Text(
                            text = "Geser & cubit (zoom) foto agar pas di dalam lingkaran",
                            fontSize = 12.sp,
                            color = Color(0xFF6B563E)
                        )
                    }
                    IconButton(
                        onClick = onCancel,
                        enabled = !isSaving,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup",
                            tint = CropOutline
                        )
                    }
                }

                // Interactive circular crop viewport with live wing & beak preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF4EC0CA))
                        .border(2.5.dp, CropOutline, RoundedCornerShape(18.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pendingBird, isSaving) {
                                if (isSaving) return@pointerInput
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    val newZoom = (zoom * gestureZoom).coerceIn(
                                        CustomMediaManager.MIN_BIRD_CROP_ZOOM,
                                        CustomMediaManager.MAX_BIRD_CROP_ZOOM
                                    )
                                    val circleDiameterPx =
                                        (min(size.width, size.height) * CROP_CIRCLE_FRACTION)
                                            .coerceAtLeast(1f)
                                    val (maxPanX, maxPanY) = CustomMediaManager.computeMaxBirdPanNormalized(
                                        srcWidth = bitmapWidth,
                                        srcHeight = bitmapHeight,
                                        zoom = newZoom
                                    )
                                    val zoomRatio = if (zoom > 0f) newZoom / zoom else 1f
                                    val nextPanX = panNormX * zoomRatio + (pan.x / circleDiameterPx)
                                    val nextPanY = panNormY * zoomRatio + (pan.y / circleDiameterPx)
                                    zoom = newZoom
                                    panNormX = nextPanX.coerceIn(-maxPanX, maxPanX)
                                    panNormY = nextPanY.coerceIn(-maxPanY, maxPanY)
                                }
                            }
                    ) {
                        val minSide = min(size.width, size.height)
                        val circleDiameter = minSide * CROP_CIRCLE_FRACTION
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.5f
                        val borderWidth = (circleDiameter / (1f - 2f * 0.055f)) * 0.055f
                        val outerRadius = circleDiameter * 0.5f + borderWidth
                        val photoRadius = circleDiameter * 0.5f

                        val placement = CustomMediaManager.computeBirdCropPlacement(
                            srcWidth = bitmapWidth,
                            srcHeight = bitmapHeight,
                            circleCenterX = cx,
                            circleCenterY = cy,
                            circleDiameter = circleDiameter,
                            crop = BirdCropTransform(
                                zoom = zoom,
                                panNormX = panNormX,
                                panNormY = panNormY
                            )
                        )

                        // 1. User photo clipped to square editor viewport
                        clipRect {
                            drawImage(
                                image = previewBitmap,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(bitmapWidth, bitmapHeight),
                                dstOffset = IntOffset(
                                    placement.left.roundToInt(),
                                    placement.top.roundToInt()
                                ),
                                dstSize = IntSize(
                                    placement.width.roundToInt().coerceAtLeast(1),
                                    placement.height.roundToInt().coerceAtLeast(1)
                                )
                            )
                        }

                        // 2. Dim area outside the circular bird cutout
                        maskPath.reset()
                        maskPath.fillType = PathFillType.EvenOdd
                        maskPath.addRect(Rect(0f, 0f, size.width, size.height))
                        maskPath.addOval(
                            Rect(
                                left = cx - photoRadius,
                                top = cy - photoRadius,
                                right = cx + photoRadius,
                                bottom = cy + photoRadius
                            )
                        )
                        drawPath(path = maskPath, color = Color(0xAA1D1308))

                        // 3. Subtle centering crosshair inside the circle
                        drawLine(
                            color = Color(0x45FFFFFF),
                            start = Offset(cx - photoRadius * 0.28f, cy),
                            end = Offset(cx + photoRadius * 0.28f, cy),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = Color(0x45FFFFFF),
                            start = Offset(cx, cy - photoRadius * 0.28f),
                            end = Offset(cx, cy + photoRadius * 0.28f),
                            strokeWidth = 1.5f
                        )

                        // 4. Preview outer wing (left) and beak (right) exactly like in-game
                        drawPreviewWingAndBeak(
                            cx = cx,
                            cy = cy,
                            birdRadius = outerRadius / 1.06f,
                            beakPath = beakPath
                        )

                        // 5. Crisp white ring + dark cartoon outline matching saved sprite
                        drawCircle(
                            color = Color.White,
                            radius = outerRadius - borderWidth * 0.65f,
                            center = Offset(cx, cy),
                            style = Stroke(width = borderWidth * 0.85f)
                        )
                        drawCircle(
                            color = Color(0xFF3A2714),
                            radius = outerRadius - borderWidth * 0.2f,
                            center = Offset(cx, cy),
                            style = Stroke(width = borderWidth * 0.35f)
                        )
                    }
                }

                // Zoom slider for precise one-handed fitting
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = "Zoom",
                        tint = CropOutline,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = zoom,
                        onValueChange = { newZoom ->
                            val zoomRatio = if (zoom > 0f) newZoom / zoom else 1f
                            val (maxPanX, maxPanY) = CustomMediaManager.computeMaxBirdPanNormalized(
                                srcWidth = bitmapWidth,
                                srcHeight = bitmapHeight,
                                zoom = newZoom
                            )
                            zoom = newZoom
                            panNormX = (panNormX * zoomRatio).coerceIn(-maxPanX, maxPanX)
                            panNormY = (panNormY * zoomRatio).coerceIn(-maxPanY, maxPanY)
                        },
                        enabled = !isSaving,
                        valueRange = CustomMediaManager.MIN_BIRD_CROP_ZOOM..CustomMediaManager.MAX_BIRD_CROP_ZOOM,
                        colors = SliderDefaults.colors(
                            thumbColor = CropOrange,
                            activeTrackColor = CropOrange,
                            inactiveTrackColor = Color(0xFFD9C79E)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${((zoom * 10f).roundToInt() / 10f)}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CropOutline
                    )
                }

                // Quick tools: Rotate 90° & Reset position
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRotate90,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, CropOutline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RotateRight,
                            contentDescription = null,
                            tint = CropOutline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Putar 90°",
                            color = CropOutline,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            zoom = 1f
                            panNormX = 0f
                            panNormY = 0f
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, CropOutline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = CropOutline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset Posisi",
                            color = CropOutline,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Cancel / Confirm buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, CropOutline),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Batal",
                            color = CropOutline,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            onConfirm(
                                BirdCropTransform(
                                    zoom = zoom,
                                    panNormX = panNormX,
                                    panNormY = panNormY
                                )
                            )
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CropGreen,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, CropOutline),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pasang Karakter",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPreviewWingAndBeak(
    cx: Float,
    cy: Float,
    birdRadius: Float,
    beakPath: Path
) {
    val outlineWidth = (birdRadius * 0.08f).coerceAtLeast(3f)
    val outlineColor = Color(0xFF3E2912)
    val stroke = Stroke(
        width = outlineWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    // Left outer wing preview
    drawOval(
        color = Color(0xFFFFF8DC),
        topLeft = Offset(cx - birdRadius * 1.48f, cy - birdRadius * 0.24f),
        size = Size(birdRadius * 0.88f, birdRadius * 0.54f)
    )
    drawOval(
        color = outlineColor,
        topLeft = Offset(cx - birdRadius * 1.48f, cy - birdRadius * 0.24f),
        size = Size(birdRadius * 0.88f, birdRadius * 0.54f),
        style = Stroke(width = outlineWidth)
    )

    // Right outer beak preview (upper + lower mandible)
    val baseX = cx + birdRadius * 0.90f
    val centerY = cy + birdRadius * 0.05f
    val beakLength = birdRadius * 0.56f
    val beakHalfH = birdRadius * 0.24f

    beakPath.reset()
    beakPath.moveTo(baseX, centerY - beakHalfH)
    beakPath.lineTo(baseX + beakLength, centerY)
    beakPath.lineTo(baseX, centerY + beakHalfH * 0.06f)
    beakPath.close()
    drawPath(path = beakPath, color = Color(0xFFFF7A29))
    drawPath(path = beakPath, color = outlineColor, style = stroke)

    beakPath.reset()
    beakPath.moveTo(baseX, centerY - beakHalfH * 0.05f)
    beakPath.lineTo(baseX + beakLength * 0.86f, centerY + beakHalfH * 0.28f)
    beakPath.lineTo(baseX, centerY + beakHalfH)
    beakPath.close()
    drawPath(path = beakPath, color = Color(0xFFE65100))
    drawPath(path = beakPath, color = outlineColor, style = stroke)
}
