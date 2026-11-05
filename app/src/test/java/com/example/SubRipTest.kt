package com.example

import com.example.model.SubRip
import com.example.model.SubtitleCue
import org.junit.Assert.assertEquals
import org.junit.Test

class SubRipTest {
  @Test
  fun parsesBomCrLfAndMultilineCue() {
    val input = "\uFEFF1\r\n00:00:01,125 --> 00:00:03,800\r\nFirst line\r\nSecond line\r\n\r\n"
    val cues = listOf(SubtitleCue(1125, 3800, "First line\nSecond line"))
    assertEquals(cues, SubRip.parse(input))
    assertEquals(cues, SubRip.parse(SubRip.encode(cues)))
  }

  @Test(expected = IllegalArgumentException::class)
  fun rejectsBackwardsRange() {
    SubRip.parse("1\n00:00:02,000 --> 00:00:01,000\nCaption")
  }
}
