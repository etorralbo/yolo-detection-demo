package com.yolo.demo.yolo.postprocessing

import android.graphics.RectF
import com.yolo.demo.yolo.model.YoloDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NonMaximumSuppressionTest {

    private lateinit var nms: NonMaximumSuppression

    @Before
    fun setup() {
        nms = NonMaximumSuppression()
    }

    @Test
    fun `apply returns empty list for empty input`() {
        val result = nms.apply(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `apply returns single detection unchanged`() {
        val detection = createDetection(
            bbox = RectF(100f, 100f, 200f, 200f),
            confidence = 0.9f
        )

        val result = nms.apply(listOf(detection))

        assertEquals(1, result.size)
        assertEquals(detection, result[0])
    }

    @Test
    fun `apply keeps both detections when they dont overlap`() {
        val detection1 = createDetection(
            bbox = RectF(100f, 100f, 200f, 200f),
            confidence = 0.9f
        )
        val detection2 = createDetection(
            bbox = RectF(300f, 300f, 400f, 400f),
            confidence = 0.8f
        )

        val result = nms.apply(listOf(detection1, detection2))

        assertEquals(2, result.size)
    }

    @Test
    fun `apply removes overlapping detection with lower confidence`() {
        val detection1 = createDetection(
            bbox = RectF(100f, 100f, 200f, 200f),
            confidence = 0.9f,
            label = "high_conf"
        )
        val detection2 = createDetection(
            bbox = RectF(110f, 110f, 210f, 210f),  // Overlaps significantly with detection1
            confidence = 0.7f,
            label = "low_conf"
        )

        val result = nms.apply(listOf(detection1, detection2))

        assertEquals(1, result.size)
        assertEquals("high_conf", result[0].label)
        assertEquals(0.9f, result[0].confidence, 0.001f)
    }

    @Test
    fun `apply handles multiple overlapping detections`() {
        val detection1 = createDetection(
            bbox = RectF(100f, 100f, 200f, 200f),
            confidence = 0.95f,
            label = "best"
        )
        val detection2 = createDetection(
            bbox = RectF(110f, 110f, 210f, 210f),
            confidence = 0.85f,
            label = "second"
        )
        val detection3 = createDetection(
            bbox = RectF(105f, 105f, 205f, 205f),
            confidence = 0.80f,
            label = "third"
        )

        val result = nms.apply(listOf(detection1, detection2, detection3))

        assertEquals(1, result.size)
        assertEquals("best", result[0].label)
    }

    @Test
    fun `apply keeps detection with slightly lower confidence if no overlap`() {
        val detection1 = createDetection(
            bbox = RectF(100f, 100f, 200f, 200f),
            confidence = 0.9f
        )
        val detection2 = createDetection(
            bbox = RectF(250f, 250f, 350f, 350f),  // No overlap
            confidence = 0.8f
        )
        val detection3 = createDetection(
            bbox = RectF(400f, 400f, 500f, 500f),  // No overlap
            confidence = 0.7f
        )

        val result = nms.apply(listOf(detection1, detection2, detection3))

        assertEquals(3, result.size)
    }

    @Test
    fun `apply sorts by confidence before processing`() {
        val detection1 = createDetection(
            bbox = RectF(100f, 100f, 200f, 200f),
            confidence = 0.7f,
            label = "low"
        )
        val detection2 = createDetection(
            bbox = RectF(110f, 110f, 210f, 210f),
            confidence = 0.95f,
            label = "high"
        )

        // Input is NOT sorted by confidence
        val result = nms.apply(listOf(detection1, detection2))

        assertEquals(1, result.size)
        assertEquals("high", result[0].label)  // Higher confidence wins
    }

    private fun createDetection(
        bbox: RectF,
        confidence: Float,
        label: String = "test"
    ) = YoloDetection(
        boundingBox = bbox,
        label = label,
        confidence = confidence,
        trackingId = 0
    )
}
