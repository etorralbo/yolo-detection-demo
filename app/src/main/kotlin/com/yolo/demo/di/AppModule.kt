package com.yolo.demo.di

import com.yolo.demo.viewmodel.DetectionViewModel
import com.yolo.demo.yolo.YoloObjectDetectionAnalyzer
import com.yolo.demo.yolo.assetdelivery.YoloModelProvider
import com.yolo.demo.yolo.inference.YoloInferenceEngine
import com.yolo.demo.yolo.inference.YoloOutputParser
import com.yolo.demo.yolo.model.YoloLabels
import com.yolo.demo.yolo.postprocessing.NonMaximumSuppression
import com.yolo.demo.yolo.preprocessing.ImagePreprocessor
import com.yolo.demo.yolo.selection.ObjectSelectionStrategy
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Model provider - handles model download and interpreter creation
    single { YoloModelProvider(androidContext()) }

    // YOLO labels
    single { YoloLabels.loadLabels(androidContext()) }

    // YOLO components
    single { ImagePreprocessor() }
    single { NonMaximumSuppression() }
    single { ObjectSelectionStrategy() }
    single { YoloOutputParser(get()) }

    // Inference engine - uses model provider
    single { YoloInferenceEngine(get()) }

    // Main analyzer
    single {
        YoloObjectDetectionAnalyzer(
            inferenceEngine = get(),
            outputParser = get(),
            preprocessor = get(),
            nms = get(),
            selectionStrategy = get()
        )
    }

    // ViewModels
    viewModel { com.yolo.demo.viewmodel.MainViewModel(get(), get()) }

    viewModel { (yoloAnalyzer: YoloObjectDetectionAnalyzer) ->
        DetectionViewModel(yoloAnalyzer)
    }
}
