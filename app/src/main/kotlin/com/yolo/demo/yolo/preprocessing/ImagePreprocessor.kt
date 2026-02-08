package com.yolo.demo.yolo.preprocessing

import android.graphics.Bitmap
import android.graphics.Color

class ImagePreprocessor {

    companion object {
        const val INPUT_SIZE = 640
    }

    fun preprocess(bitmap: Bitmap): FloatArray {
        // Scale-fill resize to 640x640 (maintain aspect, center crop)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        // Convert to RGB float array normalized [0, 1]
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaledBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val floatArray = FloatArray(INPUT_SIZE * INPUT_SIZE * 3)
        pixels.forEachIndexed { index, pixel ->
            floatArray[index * 3] = Color.red(pixel) / 255f      // R
            floatArray[index * 3 + 1] = Color.green(pixel) / 255f  // G
            floatArray[index * 3 + 2] = Color.blue(pixel) / 255f   // B
        }

        scaledBitmap.recycle()
        return floatArray
    }
}
