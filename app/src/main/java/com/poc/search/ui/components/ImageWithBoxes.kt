package com.poc.search.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import com.poc.search.ui.model.BoxInstance
import kotlin.math.max

@Composable
fun ImageWithBoxes(
    imageModel: Any,
    imageWidth: Int?,
    imageHeight: Int?,
    instances: List<BoxInstance>,
    selectedInstanceId: String?,
    exemplarInstanceIds: Set<String> = emptySet(),
    onSelectInstance: (instanceId: String?) -> Unit
) {
    val w = (imageWidth ?: 1).coerceAtLeast(1)
    val h = (imageHeight ?: 1).coerceAtLeast(1)
    val aspect = (w.toFloat() / h.toFloat()).coerceAtLeast(0.1f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        val tapHandler = remember(instances) {
            { x: Float, y: Float, boxSize: Size ->
                if (boxSize.width <= 0f || boxSize.height <= 0f) return@remember
                val nx = (x / boxSize.width).coerceIn(0f, 1f)
                val ny = (y / boxSize.height).coerceIn(0f, 1f)

                val hit = instances
                    .filter { nx >= it.x1 && nx <= it.x2 && ny >= it.y1 && ny <= it.y2 }
                    .maxByOrNull { it.confidence }

                onSelectInstance(hit?.instanceId)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(instances) {
                    detectTapGestures { offset ->
                        // IntSize를 Size로 변환하여 전달
                        tapHandler(offset.x, offset.y, size.toSize())
                    }
                }
        ) {
            val strokeW = max(2f, size.minDimension * 0.004f)
            for (inst in instances) {
                val left = (inst.x1.toFloat() * size.width)
                val top = (inst.y1.toFloat() * size.height)
                val right = (inst.x2.toFloat() * size.width)
                val bottom = (inst.y2.toFloat() * size.height)

                val color = when {
                    inst.instanceId == selectedInstanceId -> Color.Yellow
                    exemplarInstanceIds.contains(inst.instanceId) -> Color.Green
                    else -> Color.Red
                }
                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = strokeW)
                )
            }
        }
    }
}
