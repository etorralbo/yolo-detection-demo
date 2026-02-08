package com.yolo.demo.yolo

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.yolo.demo.yolo.inference.YoloInferenceEngine
import com.yolo.demo.yolo.inference.YoloOutputParser
import com.yolo.demo.yolo.model.YoloDetection
import com.yolo.demo.yolo.postprocessing.NonMaximumSuppression
import com.yolo.demo.yolo.preprocessing.ImagePreprocessor
import com.yolo.demo.yolo.selection.ObjectSelectionStrategy
import com.yolo.demo.yolo.utils.rotate
import com.yolo.demo.yolo.utils.toBitmap

class YoloObjectDetectionAnalyzer(
    private val inferenceEngine: YoloInferenceEngine,
    private val outputParser: YoloOutputParser,
    private val preprocessor: ImagePreprocessor,
    private val nms: NonMaximumSuppression,
    private val selectionStrategy: ObjectSelectionStrategy
) {

    suspend fun analyze(imageProxy: ImageProxy): YoloDetection? {
        var bitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null

        return try {
            // 1. Convert ImageProxy to Bitmap
            bitmap = imageProxy.toBitmap() ?: return null

            // 2. Rotate bitmap to match device orientation
            val rotation = imageProxy.imageInfo.rotationDegrees
            rotatedBitmap = bitmap.rotate(rotation)

            // 3. Preprocess for YOLO
            val preprocessed = preprocessor.preprocess(rotatedBitmap)

            // 4. Run inference
            val output = inferenceEngine.detect(preprocessed) ?: return null

            // 5. Parse output
            val detections = outputParser.parse(output, rotatedBitmap.width, rotatedBitmap.height)

            if (detections.isEmpty()) return null

            // 6. Apply NMS to remove duplicate detections
            val filteredDetections = nms.apply(detections)

            if (filteredDetections.isEmpty()) return null

            // 7. Select best object with tracking
            val selectedObject = selectionStrategy.selectBestObject(
                filteredDetections,
                rotatedBitmap.width,
                rotatedBitmap.height
            )

            // 8. Add source image metadata for coordinate transformation
            selectedObject?.copy(
                sourceImageWidth = rotatedBitmap.width,
                sourceImageHeight = rotatedBitmap.height,
                sourceImageRotation = rotation
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            bitmap?.recycle()
            if (rotatedBitmap != null && rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            imageProxy.close()
        }
    }
}
