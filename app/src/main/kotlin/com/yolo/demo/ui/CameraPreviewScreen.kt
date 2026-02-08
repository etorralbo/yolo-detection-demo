package com.yolo.demo.ui

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.yolo.demo.camera.BoundingBoxTransformer
import com.yolo.demo.camera.CameraManager
import com.yolo.demo.model.DetectionDisplayState
import com.yolo.demo.viewmodel.DetectionViewModel
import com.yolo.demo.yolo.YoloObjectDetectionAnalyzer
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPreviewScreen(yoloAnalyzer: YoloObjectDetectionAnalyzer) {
    val context = LocalContext.current
    val cameraManager = CameraManager(context)
    val viewModel: DetectionViewModel = koinViewModel { parametersOf(yoloAnalyzer) }

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            cameraPermissionState.status.isGranted -> {
                DetectionCameraScreen(
                    cameraManager = cameraManager,
                    viewModel = viewModel
                )
            }
            else -> {
                PermissionDeniedContent(
                    onRequestPermission = {
                        cameraPermissionState.launchPermissionRequest()
                    }
                )
            }
        }
    }
}

@Composable
fun DetectionCameraScreen(
    cameraManager: CameraManager,
    viewModel: DetectionViewModel
) {
    val displayState by viewModel.displayState.collectAsState()
    val analyzer = viewModel.createCameraAnalyzer()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewWidthPx = with(density) { maxWidth.toPx().toInt() }
        val viewHeightPx = with(density) { maxHeight.toPx().toInt() }

        // Camera preview
        CameraPreview(
            cameraManager = cameraManager,
            analyzer = analyzer,
            modifier = Modifier.fillMaxSize()
        )

        // Detection overlay (bounding box + label)
        if (displayState is DetectionDisplayState.Displaying) {
            val detection = (displayState as DetectionDisplayState.Displaying).detection

            // Transform bounding box to screen coordinates using actual image dimensions
            val transformer = BoundingBoxTransformer(
                imageWidth = detection.sourceImageWidth,
                imageHeight = detection.sourceImageHeight,
                viewWidth = viewWidthPx,
                viewHeight = viewHeightPx,
                rotation = detection.sourceImageRotation
            )

            val transformedBox = transformer.transform(detection.boundingBox)

            // Draw bounding box if transformation succeeded
            transformedBox?.let { box ->
                BoundingBoxOverlay(
                    boundingBox = box,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Detection label at top
            DetectionLabel(
                label = detection.label,
                confidence = detection.confidence,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
            )
        }
    }
}

@Composable
fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This app needs camera access to detect objects.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}
