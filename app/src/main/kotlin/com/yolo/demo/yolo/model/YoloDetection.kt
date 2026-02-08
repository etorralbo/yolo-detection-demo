package com.yolo.demo.yolo.model

import android.graphics.RectF

data class YoloDetection(
    val boundingBox: RectF,
    val label: String,
    val confidence: Float,
    val trackingId: Int,
    val sourceImageWidth: Int = 640,
    val sourceImageHeight: Int = 640,
    val sourceImageRotation: Int = 0
)
