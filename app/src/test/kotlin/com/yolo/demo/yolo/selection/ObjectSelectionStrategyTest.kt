package com.yolo.demo.yolo.selection

import android.graphics.RectF
import com.yolo.demo.yolo.model.YoloDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ObjectSelectionStrategyTest {

    private lateinit var strategy: ObjectSelectionStrategy
    private val imageWidth = 640
    private val imageHeight = 640

    @Before
    fun setup() {
        strategy = ObjectSelectionStrategy()
    }

    @Test
    fun `selectBestObject returns null for empty list`() {
        val result = strategy.selectBestObject(emptyList(), imageWidth, imageHeight)
        assertNull(result)
    }

    @Test
    fun `selectBestObject returns single detection`() {
        val detection = createDetection(
            centerX = 320f,
            centerY = 320f,
            size = 100f,
            label = "test"
        )

        val result = strategy.selectBestObject(listOf(detection), imageWidth, imageHeight)

        assertEquals(detection, result)
    }

    @Test
    fun `selectBestObject prefers centered objects`() {
        val centered = createDetection(
            centerX = 320f,  // Center of 640x640
            centerY = 320f,
            size = 50f,
            label = "centered",
            trackingId = 1
        )
        val corner = createDetection(
            centerX = 100f,  // Corner
            centerY = 100f,
            size = 50f,
            label = "corner",
            trackingId = 2
        )

        val result = strategy.selectBestObject(listOf(centered, corner), imageWidth, imageHeight)

        assertEquals("centered", result?.label)
    }

    @Test
    fun `selectBestObject prefers larger objects when both centered`() {
        val large = createDetection(
            centerX = 320f,
            centerY = 320f,
            size = 200f,
            label = "large",
            trackingId = 1
        )
        val small = createDetection(
            centerX = 320f,
            centerY = 320f,
            size = 50f,
            label = "small",
            trackingId = 2
        )

        val result = strategy.selectBestObject(listOf(large, small), imageWidth, imageHeight)

        assertEquals("large", result?.label)
    }

    @Test
    fun `selectBestObject maintains tracking with hysteresis`() {
        val current = createDetection(
            centerX = 320f,
            centerY = 320f,
            size = 100f,
            label = "current",
            trackingId = 1
        )

        // First selection
        val result1 = strategy.selectBestObject(listOf(current), imageWidth, imageHeight)
        assertEquals("current", result1?.label)

        // Slightly better candidate appears
        val slightlyBetter = createDetection(
            centerX = 330f,  // Slightly off-center
            centerY = 330f,
            size = 110f,     // Slightly larger
            label = "better",
            trackingId = 2
        )

        // Should stick with current due to hysteresis
        val result2 = strategy.selectBestObject(
            listOf(current, slightlyBetter),
            imageWidth,
            imageHeight
        )
        assertEquals("current", result2?.label)
    }

    @Test
    fun `selectBestObject switches when new object significantly better`() {
        val current = createDetection(
            centerX = 320f,
            centerY = 320f,
            size = 50f,
            label = "current",
            trackingId = 1
        )

        // First selection
        strategy.selectBestObject(listOf(current), imageWidth, imageHeight)

        // Much better candidate (centered AND MUCH larger - needs to be 540x540 to overcome 1.3x hysteresis)
        val muchBetter = createDetection(
            centerX = 320f,
            centerY = 320f,
            size = 540f,  // Large enough to overcome hysteresis threshold
            label = "much_better",
            trackingId = 2
        )

        // After cooldown frames, should switch
        repeat(20) {
            strategy.selectBestObject(listOf(current, muchBetter), imageWidth, imageHeight)
        }

        val finalResult = strategy.selectBestObject(
            listOf(current, muchBetter),
            imageWidth,
            imageHeight
        )
        assertEquals("much_better", finalResult?.label)
    }

    @Test
    fun `selectBestObject reselects when tracked object disappears`() {
        val first = createDetection(
            centerX = 320f,
            centerY = 320f,
            size = 100f,
            label = "first",
            trackingId = 1
        )

        // First selection
        strategy.selectBestObject(listOf(first), imageWidth, imageHeight)

        // First object disappears, new object appears
        val second = createDetection(
            centerX = 320f,
            centerY = 320f,
            size = 100f,
            label = "second",
            trackingId = 2
        )

        val result = strategy.selectBestObject(listOf(second), imageWidth, imageHeight)
        assertEquals("second", result?.label)
    }

    private fun createDetection(
        centerX: Float,
        centerY: Float,
        size: Float,
        label: String,
        trackingId: Int = 0
    ): YoloDetection {
        val halfSize = size / 2
        return YoloDetection(
            boundingBox = RectF(
                centerX - halfSize,
                centerY - halfSize,
                centerX + halfSize,
                centerY + halfSize
            ),
            label = label,
            confidence = 0.9f,
            trackingId = trackingId
        )
    }
}
