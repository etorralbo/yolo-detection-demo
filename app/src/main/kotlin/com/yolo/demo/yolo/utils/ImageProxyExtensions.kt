package com.yolo.demo.yolo.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

fun ImageProxy.toBitmap(): Bitmap? {
    return runCatching {
        val yuvBytes = yuv420ToByteArray()
        val yuvImage = YuvImage(yuvBytes, ImageFormat.NV21, width, height, null)

        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, outputStream)
        val jpegBytes = outputStream.toByteArray()

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)
    }.getOrNull()
}

private fun ImageProxy.yuv420ToByteArray(): ByteArray {
    val ySize = width * height
    val uvSize = ySize / 2
    val nv21 = ByteArray(ySize + uvSize)

    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    yBuffer.rewind()
    yBuffer.get(nv21, 0, ySize)

    val uvPixelStride = planes[1].pixelStride
    val uvRowStride = planes[1].rowStride

    if (uvPixelStride == 1) {
        vBuffer.rewind()
        vBuffer.get(nv21, ySize, uvSize)
    } else {
        var pos = ySize
        val uvWidth = width / 2
        val uvHeight = height / 2

        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val uvIndex = row * uvRowStride + col * uvPixelStride
                nv21[pos++] = vBuffer.get(uvIndex)
                nv21[pos++] = uBuffer.get(uvIndex)
            }
        }
    }

    return nv21
}
