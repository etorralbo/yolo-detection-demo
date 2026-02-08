package com.yolo.demo.yolo.utils

import android.graphics.Bitmap
import android.graphics.Matrix

fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) return this

    val matrix = Matrix().apply {
        postRotate(degrees.toFloat())
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
