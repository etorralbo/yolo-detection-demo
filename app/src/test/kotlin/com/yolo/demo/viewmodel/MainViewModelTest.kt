package com.yolo.demo.viewmodel

import app.cash.turbine.test
import com.yolo.demo.yolo.YoloObjectDetectionAnalyzer
import com.yolo.demo.yolo.assetdelivery.ModelDownloadState
import com.yolo.demo.yolo.assetdelivery.YoloModelProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var modelProvider: YoloModelProvider
    private lateinit var yoloAnalyzer: YoloObjectDetectionAnalyzer
    private lateinit var downloadStateFlow: MutableStateFlow<ModelDownloadState>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        downloadStateFlow = MutableStateFlow(ModelDownloadState.NotStarted)
        modelProvider = mockk(relaxed = true)
        yoloAnalyzer = mockk(relaxed = true)
        every { modelProvider.downloadState } returns downloadStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is NotStarted`() = runTest {
        val viewModel = MainViewModel(modelProvider, yoloAnalyzer)

        viewModel.modelState.test {
            assertEquals(ModelDownloadState.NotStarted, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `requestModelDownload calls modelProvider`() = runTest {
        val viewModel = MainViewModel(modelProvider, yoloAnalyzer)
        advanceUntilIdle()

        verify(atLeast = 1) { modelProvider.requestModelDownload() }
    }

    @Test
    fun `model state updates when download completes`() = runTest {
        val viewModel = MainViewModel(modelProvider, yoloAnalyzer)

        viewModel.modelState.test {
            assertEquals(ModelDownloadState.NotStarted, awaitItem())

            downloadStateFlow.value = ModelDownloadState.Downloading
            assertEquals(ModelDownloadState.Downloading, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `model state updates when download fails`() = runTest {
        val viewModel = MainViewModel(modelProvider, yoloAnalyzer)

        viewModel.modelState.test {
            assertEquals(ModelDownloadState.NotStarted, awaitItem())

            downloadStateFlow.value = ModelDownloadState.Failed("Network error")
            val failedState = awaitItem() as ModelDownloadState.Failed
            assertEquals("Network error", failedState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `yoloAnalyzer is provided via constructor`() = runTest {
        val viewModel = MainViewModel(modelProvider, yoloAnalyzer)

        assertNotNull(viewModel.yoloAnalyzer)
        assertEquals(yoloAnalyzer, viewModel.yoloAnalyzer)
    }
}
