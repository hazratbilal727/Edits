package com.example

import com.example.model.SpeedPoint
import com.example.model.TransformKeyframe
import com.example.model.VideoClip
import com.example.model.sourceTimeAt
import com.example.model.speedSegments
import com.example.model.timelineTimeAt
import com.example.model.transformAt
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipAnimationTest {
  @Test
  fun speedPointsMapTimelineToSourceAcrossTrimAndSplit() {
    val clip = VideoClip("clip", "Video", 1000, 5000, 5000,
      speedPoints = listOf(SpeedPoint(2000, 2f), SpeedPoint(4000, 0.5f)))
    val segments = clip.speedSegments()
    assertEquals(4000L, clip.timelineDurationMs)
    assertEquals(3000L, segments.sourceTimeAt(1500))
    assertEquals(4500L, segments.sourceTimeAt(3000))
    assertEquals(3000L, segments.timelineTimeAt(4500))

    val left = clip.copy(endMs = 3000)
    val right = clip.copy(startMs = 3000)
    assertEquals(clip.timelineDurationMs, left.timelineDurationMs + right.timelineDurationMs)
    assertEquals(3500L, right.speedSegments().sourceTimeAt(250))
  }

  @Test
  fun keyframesInterpolateInSourceTimeAndSurviveTrim() {
    val clip = VideoClip("clip", "Video", 1500, 3500, 4000, transformKeyframes = listOf(
      TransformKeyframe(1000, 1f, 0f, 0f),
      TransformKeyframe(3000, 3f, 0.5f, -0.5f)
    ))
    assertEquals(TransformKeyframe(2000, 2f, 0.25f, -0.25f), clip.transformAt(2000))
    assertEquals(3f, clip.transformAt(3300).scale, 0f)
  }
}
