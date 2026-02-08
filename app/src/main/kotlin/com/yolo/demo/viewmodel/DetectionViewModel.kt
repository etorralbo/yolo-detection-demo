package com.yolo.demo.viewmodel

import androidx.lifecycle.ViewModel
import com.yolo.demo.camera.YoloCameraAnalyzer
import com.yolo.demo.yolo.YoloObjectDetectionAnalyzer
import com.yolo.demo.yolo.model.YoloDetection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DetectionViewModel(
    private val yoloAnalyzer: YoloObjectDetectionAnalyzer
) : ViewModel() {

    private val _detectionState = MutableStateFlow<YoloDetection?>(null)
    val detectionState: StateFlow<YoloDetection?> = _detectionState.asStateFlow()

    fun createCameraAnalyzer(): YoloCameraAnalyzer {
        return YoloCameraAnalyzer(yoloAnalyzer) { detection ->
            _detectionState.value = detection
        }
    }
}
