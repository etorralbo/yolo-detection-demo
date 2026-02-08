package com.yolo.demo.yolo.postprocessing

import android.graphics.RectF
import com.yolo.demo.yolo.model.YoloDetection
import kotlin.math.max
import kotlin.math.min

class NonMaximumSuppression {

    companion object {
        const val IOU_THRESHOLD = 0.45f
    }

    fun apply(detections: List<YoloDetection>): List<YoloDetection> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }
        val retained = mutableListOf<YoloDetection>()
        val suppressed = BooleanArray(sorted.size) { false }

        for (i in sorted.indices) {
            if (suppressed[i]) continue

            val current = sorted[i]
            retained.add(current)

            for (j in (i + 1) until sorted.size) {
                if (suppressed[j]) continue

                val candidate = sorted[j]
                val iou = calculateIoU(current.boundingBox, candidate.boundingBox)

                if (iou > IOU_THRESHOLD) {
                    suppressed[j] = true
                }
            }
        }

        return retained
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectLeft = max(box1.left, box2.left)
        val intersectTop = max(box1.top, box2.top)
        val intersectRight = min(box1.right, box2.right)
        val intersectBottom = min(box1.bottom, box2.bottom)

        if (intersectLeft >= intersectRight || intersectTop >= intersectBottom) {
            return 0f
        }

        val intersectArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectArea

        if (unionArea <= 0f) return 0f

        return intersectArea / unionArea
    }
}
