package com.yolo.demo.model

import com.yolo.demo.yolo.model.YoloDetection

sealed class DetectionDisplayState {
    object Idle : DetectionDisplayState()
    data class Displaying(val detection: YoloDetection) : DetectionDisplayState()
    object Cooldown : DetectionDisplayState()
}
