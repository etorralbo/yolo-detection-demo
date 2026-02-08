package com.yolo.demo.camera

import android.graphics.RectF

/**
 * Transforms bounding box coordinates from camera/image space to screen space.
 *
 * Handles:
 * - CENTER_CROP scaling (camera preview fills screen, cropping edges)
 * - Rotation transformation
 * - Coordinate translation
 */
class BoundingBoxTransformer(
    private val imageWidth: Int,
    private val imageHeight: Int,
    private val viewWidth: Int,
    private val viewHeight: Int,
    private val rotation: Int
) {
    /**
     * Transform a bounding box from image coordinates to view coordinates.
     *
     * @param modelBox Bounding box in image coordinates
     * @return Transformed bounding box in view coordinates, or null if invalid
     */
    fun transform(modelBox: RectF): RectF? {
        // 1. Apply rotation transformation
        val rotated = when (rotation) {
            90 -> RectF(
                modelBox.top,
                imageWidth - modelBox.right,
                modelBox.bottom,
                imageWidth - modelBox.left
            )
            180 -> RectF(
                imageWidth - modelBox.right,
                imageHeight - modelBox.bottom,
                imageWidth - modelBox.left,
                imageHeight - modelBox.top
            )
            270 -> RectF(
                imageHeight - modelBox.bottom,
                modelBox.left,
                imageHeight - modelBox.top,
                modelBox.right
            )
            else -> modelBox
        }

        // Get dimensions after rotation
        val rotatedImageWidth = if (rotation == 90 || rotation == 270) imageHeight else imageWidth
        val rotatedImageHeight = if (rotation == 90 || rotation == 270) imageWidth else imageHeight

        // 2. Calculate CENTER_CROP scale factor
        val imageAspect = rotatedImageWidth.toFloat() / rotatedImageHeight
        val viewAspect = viewWidth.toFloat() / viewHeight
        val scale = if (imageAspect > viewAspect) {
            // Image is wider - fit to height, crop sides
            viewHeight.toFloat() / rotatedImageHeight
        } else {
            // Image is taller - fit to width, crop top/bottom
            viewWidth.toFloat() / rotatedImageWidth
        }

        // 3. Calculate centering offsets
        val scaledImageWidth = rotatedImageWidth * scale
        val scaledImageHeight = rotatedImageHeight * scale
        val offsetX = (viewWidth - scaledImageWidth) / 2
        val offsetY = (viewHeight - scaledImageHeight) / 2

        // 4. Transform coordinates
        val transformedLeft = rotated.left * scale + offsetX
        val transformedTop = rotated.top * scale + offsetY
        val transformedRight = rotated.right * scale + offsetX
        val transformedBottom = rotated.bottom * scale + offsetY
        val transformed = RectF(transformedLeft, transformedTop, transformedRight, transformedBottom)

        // 5. Clip to view bounds
        val clippedLeft = transformed.left.coerceIn(0f, viewWidth.toFloat())
        val clippedTop = transformed.top.coerceIn(0f, viewHeight.toFloat())
        val clippedRight = transformed.right.coerceIn(0f, viewWidth.toFloat())
        val clippedBottom = transformed.bottom.coerceIn(0f, viewHeight.toFloat())
        val clipped = RectF(clippedLeft, clippedTop, clippedRight, clippedBottom)

        // 6. Filter out invalid or too small boxes
        val width = clipped.right - clipped.left
        val height = clipped.bottom - clipped.top

        return if (width >= 20 && height >= 20) clipped else null
    }
}
