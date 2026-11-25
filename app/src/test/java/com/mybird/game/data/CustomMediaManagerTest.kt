package com.mybird.game.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomMediaManagerTest {

    @Test
    fun calculateInSampleSizeDownsamplesLargeImagesInPowersOfTwo() {
        assertEquals(1, CustomMediaManager.calculateInSampleSize(512, 512, 512))
        assertEquals(2, CustomMediaManager.calculateInSampleSize(1800, 1200, 512))
        assertEquals(4, CustomMediaManager.calculateInSampleSize(4000, 3000, 512))
    }

    @Test
    fun computeDecodeTargetSizePreservesAspectAndCapsMaxSide() {
        val (w1, h1) = CustomMediaManager.computeDecodeTargetSize(4000, 2000, 1000)
        assertEquals(1000, w1)
        assertEquals(500, h1)

        val (w2, h2) = CustomMediaManager.computeDecodeTargetSize(600, 400, 1000)
        assertEquals(600, w2)
        assertEquals(400, h2)
    }

    @Test
    fun computeBackgroundTargetSizePreservesLandscapeAndPortraitAspects() {
        // Standard 16:9 landscape stays 16:9 (1920x1080)
        val (landW, landH) = CustomMediaManager.computeBackgroundTargetSize(3840, 2160)
        assertEquals(1920, landW)
        assertEquals(1080, landH)

        // Standard 9:16 portrait stays portrait without forcing square crop
        val (portW, portH) = CustomMediaManager.computeBackgroundTargetSize(1080, 1920)
        assertEquals(1080, portH)
        assertEquals(608, portW)
        assertTrue(portW < portH)

        // Ultra-wide panorama respects max width cap while staying within BG_MAX_ASPECT
        val (panoW, panoH) = CustomMediaManager.computeBackgroundTargetSize(6000, 1200)
        assertTrue(panoW <= CustomMediaManager.BG_MAX_WIDTH_PX)
        assertTrue(panoH <= CustomMediaManager.BG_MAX_HEIGHT_PX)
        val panoAspect = panoW.toFloat() / panoH.toFloat()
        assertEquals(CustomMediaManager.BG_MAX_ASPECT, panoAspect, 0.05f)
    }

    @Test
    fun computeCenterCropBoundsAlwaysProducesPositiveInBoundsCrop() {
        val cropWide = CustomMediaManager.computeCenterCropBounds(4000, 1000, 2.0f)
        assertEquals(2000, cropWide.width)
        assertEquals(1000, cropWide.height)
        assertEquals(1000, cropWide.left)
        assertEquals(0, cropWide.top)

        val cropTiny = CustomMediaManager.computeCenterCropBounds(1, 1, 2.0f)
        assertEquals(1, cropTiny.width)
        assertEquals(1, cropTiny.height)
        assertEquals(0, cropTiny.left)
        assertEquals(0, cropTiny.top)
    }

    @Test
    fun birdCropPanBoundsAndPlacementMatchCircleGeometry() {
        // 1:1 square at 1x zoom has zero pan slack; at 2x zoom allows +/-0.5 diameter pan
        val (sqPanX1, sqPanY1) = CustomMediaManager.computeMaxBirdPanNormalized(512, 512, 1f)
        assertEquals(0f, sqPanX1, 0.001f)
        assertEquals(0f, sqPanY1, 0.001f)

        val (sqPanX2, sqPanY2) = CustomMediaManager.computeMaxBirdPanNormalized(512, 512, 2f)
        assertEquals(0.5f, sqPanX2, 0.001f)
        assertEquals(0.5f, sqPanY2, 0.001f)

        // 2:1 landscape at 1x zoom allows horizontal pan of +/-0.5 diameter and 0 vertical
        val (widePanX, widePanY) = CustomMediaManager.computeMaxBirdPanNormalized(1000, 500, 1f)
        assertEquals(0.5f, widePanX, 0.001f)
        assertEquals(0f, widePanY, 0.001f)

        // Excessive pan gets clamped so circle never exposes empty borders
        val placement = CustomMediaManager.computeBirdCropPlacement(
            srcWidth = 1000,
            srcHeight = 500,
            circleCenterX = 100f,
            circleCenterY = 100f,
            circleDiameter = 200f,
            crop = BirdCropTransform(zoom = 1f, panNormX = 9f, panNormY = -5f)
        )
        assertEquals(400f, placement.width, 0.01f)
        assertEquals(200f, placement.height, 0.01f)
        assertEquals(0f, placement.left, 0.01f)
        assertEquals(0f, placement.top, 0.01f)
    }
}
