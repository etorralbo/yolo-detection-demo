package com.yolo.demo.yolo.inference

import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class YoloInferenceEngine(private val modelPath: String) {

    companion object {
        const val INPUT_SIZE = 640
        const val NUM_DETECTIONS = 8400
        const val OUTPUT_SIZE = 84  // 4 bbox + 80 classes
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    fun initialize() {
        val options = Interpreter.Options()

        // Try GPU acceleration
        try {
            gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate)
            options.setNumThreads(2)  // GPU handles most work
        } catch (e: Exception) {
            options.setNumThreads(4)  // CPU needs more threads
        }

        val model = loadModelFile(File(modelPath))
        interpreter = Interpreter(model, options)
    }

    fun detect(preprocessedImage: FloatArray): FloatArray {
        val inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
            .order(ByteOrder.nativeOrder())
        val floatBuffer = inputBuffer.asFloatBuffer()
        floatBuffer.put(preprocessedImage)

        val outputBuffer = Array(1) { Array(OUTPUT_SIZE) { FloatArray(NUM_DETECTIONS) } }
        interpreter?.run(inputBuffer, outputBuffer)

        // Transpose output from [1, 84, 8400] to [8400, 84]
        val transposed = FloatArray(NUM_DETECTIONS * OUTPUT_SIZE)
        for (i in 0 until NUM_DETECTIONS) {
            for (j in 0 until OUTPUT_SIZE) {
                transposed[i * OUTPUT_SIZE + j] = outputBuffer[0][j][i]
            }
        }

        return transposed
    }

    private fun loadModelFile(modelFile: File): MappedByteBuffer {
        val inputStream = FileInputStream(modelFile)
        val fileChannel = inputStream.channel
        val declaredLength = modelFile.length()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, declaredLength)
    }

    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
    }
}
