package com.example

import com.example.media.textMotion
import com.example.model.TextTrackItem
import org.junit.Assert.*
import org.junit.Test

class TextMotionTest {
  @Test fun fadeHasAReadingHoldAndIndependentExit() {
    val track = TextTrackItem("title", "Hello", 1000, 5000, entrance = "Fade", exit = "Fade", animationDurationMs = 500)
    assertEquals(0f, textMotion(track, 1000).alpha, 0.001f)
    assertEquals(0.5f, textMotion(track, 1250).alpha, 0.001f)
    assertEquals(1f, textMotion(track, 2500).alpha, 0.001f)
    assertEquals(0.5f, textMotion(track, 4750).alpha, 0.001f)
    assertEquals(1f, textMotion(track, 1000, editing = true).alpha, 0.001f)
  }

  @Test fun typewriterDoesNotSplitEmojiSurrogates() {
    val track = TextTrackItem("title", "A😀B", 0, 3000, entrance = "Typewriter", animationDurationMs = 1000)
    assertEquals(1, textMotion(track, 500).visibleCharacters)
    assertEquals(3, textMotion(track, 700).visibleCharacters)
    assertEquals(4, textMotion(track, 1500).visibleCharacters)
  }
}
