package com.example.model

import java.util.Locale

data class SubtitleCue(val startMs: Long, val endMs: Long, val text: String)

object SubRip {
  private val timing = Regex(
    """^(\d{1,6}:\d{2}:\d{2}[,.]\d{3})\s*-->\s*(\d{1,6}:\d{2}:\d{2}[,.]\d{3})(?:\s+.*)?$"""
  )

  fun parse(input: String): List<SubtitleCue> {
    val cues = mutableListOf<SubtitleCue>()
    val lines = input.removePrefix("\uFEFF").lineSequence().iterator()
    while (lines.hasNext()) {
      var line = lines.next().trim()
      if (line.isEmpty()) continue
      if (line.all(Char::isDigit) && lines.hasNext()) line = lines.next().trim()
      val match = requireNotNull(timing.matchEntire(line)) { "Invalid subtitle timing" }
      val start = milliseconds(match.groupValues[1])
      val end = milliseconds(match.groupValues[2])
      val body = mutableListOf<String>()
      while (lines.hasNext()) {
        val next = lines.next()
        if (next.isBlank()) break
        body.add(next)
      }
      require(end > start && body.isNotEmpty()) { "Invalid subtitle cue" }
      cues.add(SubtitleCue(start, end, body.joinToString("\n")))
    }
    return cues.sortedBy { it.startMs }
  }

  fun encode(cues: List<SubtitleCue>): String = buildString {
    cues.sortedBy { it.startMs }.forEachIndexed { index, cue ->
      appendLine(index + 1)
      appendLine("${timestamp(cue.startMs)} --> ${timestamp(cue.endMs)}")
      // SubRip uses blank lines as cue separators.
      appendLine(cue.text.lineSequence().filter { it.isNotBlank() }.joinToString("\n"))
      appendLine()
    }
  }

  private fun milliseconds(timestamp: String): Long {
    val (hours, minutes, seconds, millis) = timestamp.split(':', ',', '.').map(String::toLong)
    require(minutes < 60 && seconds < 60)
    return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
  }

  private fun timestamp(ms: Long): String = String.format(
    Locale.ROOT, "%02d:%02d:%02d,%03d", ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000
  )
}
