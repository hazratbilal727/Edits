package com.example

import com.example.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineTrimTest {
  @Test fun videoTrimmingUsesTheSourceSpeedCurve() {
    val clip = VideoClip("clip", "Video", startMs = 1000, endMs = 9000, originalDurationMs = 10000,
      speedPoints = listOf(SpeedPoint(0, 2f), SpeedPoint(5000, 0.5f)))
    assertEquals(3000L to 9000L, clip.trimAtTimelineOffset(true, 1000))
    assertEquals(1000L to 8500L, clip.trimAtTimelineOffset(false, -1000))
    assertEquals(0L to 9000L, clip.trimAtTimelineOffset(true, -10000))
    assertEquals(1000L to 10000L, clip.trimAtTimelineOffset(false, 10000))
  }

  @Test fun photoDurationCanExtendWithoutChangingSourceMedia() {
    val clip = VideoClip("photo", "Photo", endMs = 3000, originalDurationMs = 3000, isVideo = false)
    assertEquals(0L to 5000L, clip.trimAtTimelineOffset(false, 2000))
    assertEquals(2900L to 3000L, clip.trimAtTimelineOffset(true, 9000))
  }
}
