package com.yolo.demo.yolo.selection

import com.yolo.demo.yolo.model.YoloDetection
import kotlin.math.pow
import kotlin.math.sqrt

class ObjectSelectionStrategy {

    private var lastTrackedId: Int? = null
    private var framesSinceSelection = 0

    companion object {
        private const val SELECTION_COOLDOWN_FRAMES = 15
        private const val SELECTION_HYSTERESIS_MULTIPLIER = 1.3f
        private const val CENTER_WEIGHT = 0.7f
        private const val AREA_WEIGHT = 0.3f
    }

    fun selectBestObject(
        detections: List<YoloDetection>,
        imageWidth: Int,
        imageHeight: Int
    ): YoloDetection? {
        if (detections.isEmpty()) {
            lastTrackedId = null
            return null
        }

        framesSinceSelection++

        val trackedObject = lastTrackedId?.let { id ->
            detections.find { it.trackingId == id }
        }

        val shouldReconsider = trackedObject == null || framesSinceSelection >= SELECTION_COOLDOWN_FRAMES

        val selectedObject = when {
            !shouldReconsider -> trackedObject
            trackedObject == null -> selectInitialObject(detections, imageWidth, imageHeight)
            else -> considerSwitching(trackedObject, detections, imageWidth, imageHeight)
        }

        lastTrackedId = selectedObject.trackingId
        return selectedObject
    }

    private fun selectInitialObject(
        detections: List<YoloDetection>,
        imageWidth: Int,
        imageHeight: Int
    ): YoloDetection {
        framesSinceSelection = 0
        return selectBestByScore(detections, imageWidth, imageHeight)
    }

    private fun considerSwitching(
        trackedObject: YoloDetection,
        detections: List<YoloDetection>,
        imageWidth: Int,
        imageHeight: Int
    ): YoloDetection {
        val bestCandidate = selectBestByScore(detections, imageWidth, imageHeight)
        val currentScore = calculateScore(trackedObject, imageWidth, imageHeight)
        val bestScore = calculateScore(bestCandidate, imageWidth, imageHeight)

        return if (bestScore > currentScore * SELECTION_HYSTERESIS_MULTIPLIER) {
            framesSinceSelection = 0  // Reset only when switching
            bestCandidate
        } else {
            trackedObject
        }
    }

    private fun selectBestByScore(
        detections: List<YoloDetection>,
        imageWidth: Int,
        imageHeight: Int
    ): YoloDetection {
        return detections.maxBy { calculateScore(it, imageWidth, imageHeight) }
    }

    private fun calculateScore(
        detection: YoloDetection,
        imageWidth: Int,
        imageHeight: Int
    ): Float {
        val rect = detection.boundingBox
        val centerX = rect.centerX()
        val centerY = rect.centerY()

        val imageCenterX = imageWidth / 2f
        val imageCenterY = imageHeight / 2f

        val distanceToCenter = sqrt(
            ((centerX - imageCenterX) / imageWidth).pow(2) +
                ((centerY - imageCenterY) / imageHeight).pow(2)
        )

        val area = rect.width() * rect.height()
        val normalizedArea = area / (imageWidth * imageHeight).toFloat()

        val centerScore = (1f - distanceToCenter).coerceIn(0f, 1f)

        return (centerScore * CENTER_WEIGHT) + (normalizedArea * AREA_WEIGHT)
    }
}
