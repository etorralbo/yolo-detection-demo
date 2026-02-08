package com.yolo.demo.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.yolo.demo.yolo.YoloObjectDetectionAnalyzer
import com.yolo.demo.yolo.model.YoloDetection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YoloCameraAnalyzer(
    private val yoloAnalyzer: YoloObjectDetectionAnalyzer,
    private val onDetection: (YoloDetection?) -> Unit
) : ImageAnalysis.Analyzer {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun analyze(image: ImageProxy) {
        coroutineScope.launch {
            val detection = yoloAnalyzer.analyze(image)
            withContext(Dispatchers.Main.immediate) {
                onDetection(detection)
            }
        }
    }
}
