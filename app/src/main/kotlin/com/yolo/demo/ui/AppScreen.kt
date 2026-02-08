package com.yolo.demo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yolo.demo.viewmodel.MainViewModel
import com.yolo.demo.yolo.assetdelivery.ModelDownloadState
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppScreen() {
    val mainViewModel: MainViewModel = koinViewModel()
    val modelState by mainViewModel.modelState.collectAsState()

    when (modelState) {
        is ModelDownloadState.NotStarted,
        is ModelDownloadState.Downloading -> {
            ModelLoadingScreen()
        }
        is ModelDownloadState.Downloaded -> {
            CameraPreviewScreen(yoloAnalyzer = mainViewModel.yoloAnalyzer)
        }
        is ModelDownloadState.Failed -> {
            ModelErrorScreen(
                errorMessage = (modelState as ModelDownloadState.Failed).error,
                onRetry = { mainViewModel.requestModelDownload() }
            )
        }
    }
}
