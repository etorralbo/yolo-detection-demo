package com.yolo.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yolo.demo.yolo.YoloObjectDetectionAnalyzer
import com.yolo.demo.yolo.assetdelivery.ModelDownloadState
import com.yolo.demo.yolo.assetdelivery.YoloModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val modelProvider: YoloModelProvider,
    val yoloAnalyzer: YoloObjectDetectionAnalyzer
) : ViewModel() {

    private val _modelState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotStarted)
    val modelState: StateFlow<ModelDownloadState> = _modelState.asStateFlow()

    init {
        viewModelScope.launch {
            modelProvider.downloadState.collect { state ->
                _modelState.value = state
            }
        }

        requestModelDownload()
    }

    fun requestModelDownload() {
        modelProvider.requestModelDownload()
    }

    override fun onCleared() {
        super.onCleared()
        modelProvider.cleanup()
    }
}
