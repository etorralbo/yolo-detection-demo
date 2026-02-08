package com.yolo.demo.ui

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yolo.demo.camera.CameraManager
import com.yolo.demo.camera.YoloCameraAnalyzer

@Composable
fun CameraPreview(
    cameraManager: CameraManager,
    analyzer: YoloCameraAnalyzer,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    LaunchedEffect(Unit) {
        cameraManager.startCameraWithAnalysis(lifecycleOwner, { request ->
            surfaceRequest = request
        }, analyzer)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.stopCamera()
            cameraManager.release()
        }
    }

    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            modifier = modifier
        )
    }
}
