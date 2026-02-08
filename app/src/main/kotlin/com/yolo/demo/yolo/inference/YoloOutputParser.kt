package com.yolo.demo.yolo.inference

import android.graphics.RectF
import com.yolo.demo.yolo.model.YoloDetection

class YoloOutputParser(private val labels: List<String>) {

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.75f
        const val NUM_DETECTIONS = 8400
        const val OUTPUT_SIZE = 84
    }

    fun parse(output: FloatArray, originalWidth: Int, originalHeight: Int): List<YoloDetection> {
        val detections = mutableListOf<YoloDetection>()

        // YOLO output: [8400, 84] where each row is [x_center, y_center, width, height, class1_score, ..., class80_score]
        for (i in 0 until NUM_DETECTIONS) {
            val baseIndex = i * OUTPUT_SIZE

            // Extract bbox (normalized 0-1)
            val xCenter = output[baseIndex]
            val yCenter = output[baseIndex + 1]
            val width = output[baseIndex + 2]
            val height = output[baseIndex + 3]

            // Find best class (80 classes starting at index 4)
            var maxScore = 0f
            var classId = -1
            for (j in 4 until OUTPUT_SIZE) {
                if (output[baseIndex + j] > maxScore) {
                    maxScore = output[baseIndex + j]
                    classId = j - 4
                }
            }

            if (maxScore >= CONFIDENCE_THRESHOLD) {
                // Convert to pixel coordinates
                val left = (xCenter - width / 2) * originalWidth
                val top = (yCenter - height / 2) * originalHeight
                val right = (xCenter + width / 2) * originalWidth
                val bottom = (yCenter + height / 2) * originalHeight

                detections.add(
                    YoloDetection(
                        boundingBox = RectF(left, top, right, bottom),
                        label = labels.getOrElse(classId) { "unknown" },
                        confidence = maxScore,
                        trackingId = (xCenter * 1000 + yCenter * 1000).toInt()
                    )
                )
            }
        }

        return detections
    }
}
