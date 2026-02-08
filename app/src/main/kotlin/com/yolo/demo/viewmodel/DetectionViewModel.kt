package com.yolo.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yolo.demo.camera.YoloCameraAnalyzer
import com.yolo.demo.model.DetectionDisplayState
import com.yolo.demo.yolo.YoloObjectDetectionAnalyzer
import com.yolo.demo.yolo.model.YoloDetection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetectionViewModel(
    private val yoloAnalyzer: YoloObjectDetectionAnalyzer
) : ViewModel() {

    companion object {
        private const val DISPLAY_DURATION_MS = 4000L  // 4 seconds
        private const val COOLDOWN_DURATION_MS = 1000L  // 1 second
    }

    private val _displayState = MutableStateFlow<DetectionDisplayState>(DetectionDisplayState.Idle)
    val displayState: StateFlow<DetectionDisplayState> = _displayState.asStateFlow()

    private var displayJob: Job? = null

    fun createCameraAnalyzer(): YoloCameraAnalyzer {
        return YoloCameraAnalyzer(yoloAnalyzer) { detection ->
            handleNewDetection(detection)
        }
    }

    private fun handleNewDetection(detection: YoloDetection?) {
        // Only process new detections when idle
        if (_displayState.value !is DetectionDisplayState.Idle) {
            return
        }

        if (detection != null) {
            displayDetection(detection)
        }
    }

    private fun displayDetection(detection: YoloDetection) {
        displayJob?.cancel()
        displayJob = viewModelScope.launch {
            // Display phase (4 seconds)
            _displayState.value = DetectionDisplayState.Displaying(detection)
            delay(DISPLAY_DURATION_MS)

            // Cooldown phase (1 second)
            _displayState.value = DetectionDisplayState.Cooldown
            delay(COOLDOWN_DURATION_MS)

            // Back to idle
            _displayState.value = DetectionDisplayState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        displayJob?.cancel()
    }
}

