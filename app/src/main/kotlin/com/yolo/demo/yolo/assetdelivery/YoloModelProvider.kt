package com.yolo.demo.yolo.assetdelivery

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.assetpacks.AssetPackLocation
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.assetpacks.model.AssetPackStorageMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class YoloModelProvider(context: Context) {

    private val assetPackManager: AssetPackManager = AssetPackManagerFactory.getInstance(context)

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotStarted)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private val updateListener = AssetPackStateUpdateListener { state ->
        when (state.status()) {
            AssetPackStatus.DOWNLOADING -> {
                _downloadState.value = ModelDownloadState.Downloading
            }
            AssetPackStatus.COMPLETED -> {
                val modelPath = getModelPath()
                if (modelPath != null) {
                    _downloadState.value = ModelDownloadState.Downloaded(modelPath)
                } else {
                    _downloadState.value = ModelDownloadState.Failed("Model file not found")
                }
            }
            AssetPackStatus.FAILED -> {
                _downloadState.value = ModelDownloadState.Failed(
                    "Download failed: ${state.errorCode()}"
                )
            }
            AssetPackStatus.CANCELED -> {
                _downloadState.value = ModelDownloadState.Failed("Download was canceled")
            }
            else -> {
                // Other states: PENDING, TRANSFERRING, WAITING_FOR_WIFI, etc.
            }
        }
    }

    init {
        assetPackManager.registerListener(updateListener)
        checkIfAlreadyDownloaded()
    }

    private fun checkIfAlreadyDownloaded() {
        try {
            val packStates = Tasks.await(assetPackManager.getPackStates(listOf(ASSET_PACK_NAME)))
            val state = packStates.packStates()[ASSET_PACK_NAME]
            if (state?.status() == AssetPackStatus.COMPLETED) {
                val modelPath = getModelPath()
                if (modelPath != null) {
                    _downloadState.value = ModelDownloadState.Downloaded(modelPath)
                }
            }
        } catch (e: Exception) {
            // Ignore errors during initial check
        }
    }

    fun requestModelDownload() {
        _downloadState.value = ModelDownloadState.Downloading

        assetPackManager.fetch(listOf(ASSET_PACK_NAME))
            .addOnFailureListener { exception ->
                _downloadState.value = ModelDownloadState.Failed(
                    exception.message ?: "Unknown error"
                )
            }
    }

    private fun getModelPath(): String? {
        val location = assetPackManager.getPackLocation(ASSET_PACK_NAME) ?: return null

        return if (location.packStorageMethod() == AssetPackStorageMethod.STORAGE_FILES) {
            File(location.assetsPath(), MODEL_FILE_NAME).absolutePath
        } else {
            null
        }
    }

    fun cleanup() {
        assetPackManager.unregisterListener(updateListener)
    }

    companion object {
        private const val ASSET_PACK_NAME = "assets_delivery"
        private const val MODEL_FILE_NAME = "yolo11n_int8.tflite"
    }
}
