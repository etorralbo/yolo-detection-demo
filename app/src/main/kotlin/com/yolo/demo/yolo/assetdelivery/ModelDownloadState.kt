package com.yolo.demo.yolo.assetdelivery

sealed class ModelDownloadState {
    data object NotStarted : ModelDownloadState()
    data object Downloading : ModelDownloadState()
    data class Downloaded(val modelPath: String) : ModelDownloadState()
    data class Failed(val error: String) : ModelDownloadState()
}
