package com.yolo.demo.yolo.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class YoloOutputParserTest {

    private lateinit var parser: YoloOutputParser
    private val testLabels = listOf("person", "bicycle", "car", "motorcycle")

    @Before
    fun setup() {
        parser = YoloOutputParser(testLabels)
    }

    @Test
    fun `parse returns empty list when all confidences are below threshold`() {
        // Create output with all low confidence scores
        val output = FloatArray(YoloOutputParser.NUM_DETECTIONS * YoloOutputParser.OUTPUT_SIZE) { 0.1f }

        val detections = parser.parse(output, 640, 640)

        assertTrue(detections.isEmpty())
    }

    @Test
    fun `parse returns detection when confidence exceeds threshold`() {
        val output = FloatArray(YoloOutputParser.NUM_DETECTIONS * YoloOutputParser.OUTPUT_SIZE) { 0f }

        // Create a detection: [x_center, y_center, width, height, ...class_scores]
        val baseIndex = 0 * YoloOutputParser.OUTPUT_SIZE
        output[baseIndex] = 0.5f      // x_center (normalized)
        output[baseIndex + 1] = 0.5f  // y_center
        output[baseIndex + 2] = 0.2f  // width
        output[baseIndex + 3] = 0.3f  // height
        output[baseIndex + 4] = 0.8f  // class 0 score (person)

        val detections = parser.parse(output, 640, 640)

        assertEquals(1, detections.size)
        assertEquals("person", detections[0].label)
        assertEquals(0.8f, detections[0].confidence, 0.001f)
    }

    @Test
    fun `parse correctly converts normalized coordinates to pixels`() {
        val output = FloatArray(YoloOutputParser.NUM_DETECTIONS * YoloOutputParser.OUTPUT_SIZE) { 0f }

        val baseIndex = 0
        output[baseIndex] = 0.5f      // x_center = 0.5 * 640 = 320
        output[baseIndex + 1] = 0.5f  // y_center = 0.5 * 640 = 320
        output[baseIndex + 2] = 0.2f  // width = 0.2 * 640 = 128
        output[baseIndex + 3] = 0.3f  // height = 0.3 * 640 = 192
        output[baseIndex + 4] = 0.9f  // high confidence

        val detections = parser.parse(output, 640, 640)

        assertEquals(1, detections.size)
        val bbox = detections[0].boundingBox

        // left = (0.5 - 0.1) * 640 = 256
        // top = (0.5 - 0.15) * 640 = 224
        // right = (0.5 + 0.1) * 640 = 384
        // bottom = (0.5 + 0.15) * 640 = 416
        assertEquals(256f, bbox.left, 1f)
        assertEquals(224f, bbox.top, 1f)
        assertEquals(384f, bbox.right, 1f)
        assertEquals(416f, bbox.bottom, 1f)
    }

    @Test
    fun `parse selects class with highest score`() {
        val output = FloatArray(YoloOutputParser.NUM_DETECTIONS * YoloOutputParser.OUTPUT_SIZE) { 0f }

        val baseIndex = 0
        output[baseIndex] = 0.5f
        output[baseIndex + 1] = 0.5f
        output[baseIndex + 2] = 0.2f
        output[baseIndex + 3] = 0.2f
        output[baseIndex + 4] = 0.5f  // person (class 0)
        output[baseIndex + 5] = 0.3f  // bicycle (class 1)
        output[baseIndex + 6] = 0.9f  // car (class 2) - highest
        output[baseIndex + 7] = 0.4f  // motorcycle (class 3)

        val detections = parser.parse(output, 640, 640)

        assertEquals(1, detections.size)
        assertEquals("car", detections[0].label)
        assertEquals(0.9f, detections[0].confidence, 0.001f)
    }

    @Test
    fun `parse handles multiple detections`() {
        val output = FloatArray(YoloOutputParser.NUM_DETECTIONS * YoloOutputParser.OUTPUT_SIZE) { 0f }

        // First detection
        output[0] = 0.3f
        output[1] = 0.3f
        output[2] = 0.1f
        output[3] = 0.1f
        output[4] = 0.85f  // person

        // Second detection
        val baseIndex2 = YoloOutputParser.OUTPUT_SIZE
        output[baseIndex2] = 0.7f
        output[baseIndex2 + 1] = 0.7f
        output[baseIndex2 + 2] = 0.1f
        output[baseIndex2 + 3] = 0.1f
        output[baseIndex2 + 6] = 0.9f  // car

        val detections = parser.parse(output, 640, 640)

        assertEquals(2, detections.size)
        assertEquals("person", detections[0].label)
        assertEquals("car", detections[1].label)
    }
}
