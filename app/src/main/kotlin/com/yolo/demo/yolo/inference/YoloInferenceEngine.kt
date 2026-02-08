package com.yolo.demo.yolo.inference

import com.yolo.demo.yolo.assetdelivery.YoloModelProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder

class YoloInferenceEngine(private val modelProvider: YoloModelProvider) {

    companion object {
        const val INPUT_SIZE = 640
        const val NUM_DETECTIONS = 8400
        const val OUTPUT_SIZE = 84  // 4 bbox + 80 classes
    }

    suspend fun detect(preprocessedImage: FloatArray): FloatArray? {
        val interpreter = modelProvider.tryGetInterpreter() ?: return null

        val inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
            .order(ByteOrder.nativeOrder())
        val floatBuffer = inputBuffer.asFloatBuffer()
        floatBuffer.put(preprocessedImage)

        val outputBuffer = Array(1) { Array(OUTPUT_SIZE) { FloatArray(NUM_DETECTIONS) } }
        interpreter.run(inputBuffer, outputBuffer)

        // Transpose output from [1, 84, 8400] to [8400, 84]
        val transposed = FloatArray(NUM_DETECTIONS * OUTPUT_SIZE)
        for (i in 0 until NUM_DETECTIONS) {
            for (j in 0 until OUTPUT_SIZE) {
                transposed[i * OUTPUT_SIZE + j] = outputBuffer[0][j][i]
            }
        }

        return transposed
    }
}
