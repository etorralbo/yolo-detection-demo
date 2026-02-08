package com.yolo.demo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yolo.demo.yolo.assetdelivery.ModelDownloadState
import com.yolo.demo.yolo.assetdelivery.YoloModelProvider

@Composable
fun AppScreen() {
    val context = LocalContext.current
    val modelProvider = remember { YoloModelProvider(context) }
    val downloadState by modelProvider.downloadState.collectAsState()

    LaunchedEffect(Unit) {
        if (downloadState is ModelDownloadState.NotStarted) {
            modelProvider.requestModelDownload()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            modelProvider.cleanup()
        }
    }

    when (val state = downloadState) {
        is ModelDownloadState.NotStarted,
        is ModelDownloadState.Downloading -> {
            ModelLoadingScreen()
        }
        is ModelDownloadState.Downloaded -> {
            CameraPreviewScreen()
        }
        is ModelDownloadState.Failed -> {
            ModelErrorScreen(
                errorMessage = state.error,
                onRetry = { modelProvider.requestModelDownload() }
            )
        }
    }
}
