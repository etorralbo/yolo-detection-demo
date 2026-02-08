package com.yolo.demo.yolo.assetdelivery

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.assetpacks.AssetPackLocation
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.assetpacks.model.AssetPackStorageMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class YoloModelProvider(context: Context) {

    private val assetPackManager: AssetPackManager = AssetPackManagerFactory.getInstance(context)

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotStarted)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private val mutex = Mutex()
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

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

    suspend fun tryGetInterpreter(): Interpreter? {
        return mutex.withLock {
            // Return cached interpreter if available
            interpreter?.let { return@withLock it }

            // Try to load model
            loadModelFromAssetPack().also {
                interpreter = it
            }
        }
    }

    private suspend fun loadModelFromAssetPack(): Interpreter? {
        return runCatching {
            withContext(Dispatchers.IO) {
                val modelPath = getModelPath() ?: return@withContext null
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    Log.e(TAG, "Model file not found: $modelPath")
                    return@withContext null
                }
                createInterpreter(modelFile)
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to load model from asset pack", e)
        }.getOrNull()
    }

    private fun createInterpreter(modelFile: File): Interpreter {
        val options = Interpreter.Options().apply {
            // Try to use GPU delegate for better performance
            // Falls back to CPU if GPU is unavailable or initialization fails
            val delegate = runCatching {
                GpuDelegate()
            }.onSuccess { delegate ->
                addDelegate(delegate)
                gpuDelegate = delegate
                Log.d(TAG, "GPU delegate initialized successfully")
            }.onFailure { e ->
                Log.w(TAG, "GPU delegate failed, falling back to CPU", e)
            }.getOrNull()

            // With GPU: 2 threads (GPU handles most work)
            // Without GPU: 4 threads (CPU needs more threads for performance)
            val threadCount = if (delegate != null) 2 else 4
            setNumThreads(threadCount)
            Log.d(TAG, "Using $threadCount threads")
        }

        val modelBuffer = loadModelFile(modelFile)
        return Interpreter(modelBuffer, options)
    }

    private fun loadModelFile(modelFile: File): MappedByteBuffer {
        val inputStream = FileInputStream(modelFile)
        val fileChannel = inputStream.channel
        val declaredLength = modelFile.length()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, declaredLength)
    }

    fun cleanup() {
        assetPackManager.unregisterListener(updateListener)
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
    }

    companion object {
        private const val TAG = "YoloModelProvider"
        private const val ASSET_PACK_NAME = "assets_delivery"
        private const val MODEL_FILE_NAME = "yolo11n_int8.tflite"
    }
}
