package com.yolo.demo.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BoundingBoxOverlay(
    boundingBox: RectF,
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    strokeWidth: Float = 4.dp.value
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            color = color,
            topLeft = Offset(boundingBox.left, boundingBox.top),
            size = Size(boundingBox.width(), boundingBox.height()),
            style = Stroke(width = strokeWidth * density)
        )
    }
}
