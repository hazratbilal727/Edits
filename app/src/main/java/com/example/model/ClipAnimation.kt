package com.example.model

data class SpeedPoint(val sourceMs: Long, val rate: Float)

data class SpeedSegment(val startMs: Long, val endMs: Long, val rate: Float) {
  val timelineMs: Double get() = (endMs - startMs) / rate.toDouble()
}

data class TransformKeyframe(
  val sourceMs: Long,
  val scale: Float = 1f,
  val x: Float = 0f,
  val y: Float = 0f
)

fun VideoClip.speedSegments(): List<SpeedSegment> {
  val rateAtStart = speedPoints.lastOrNull { it.sourceMs <= startMs }?.rate ?: speed
  val points = listOf(SpeedPoint(startMs, rateAtStart)) +
    speedPoints.filter { it.sourceMs > startMs && it.sourceMs < endMs }
  return points.mapIndexed { index, point ->
    SpeedSegment(point.sourceMs, points.getOrNull(index + 1)?.sourceMs ?: endMs, point.rate)
  }
}

fun List<SpeedSegment>.sourceTimeAt(timelineMs: Long): Long {
  var remaining = timelineMs.coerceAtLeast(0).toDouble()
  for (segment in this) {
    if (remaining < segment.timelineMs) {
      return (segment.startMs + remaining * segment.rate).toLong().coerceAtMost(segment.endMs - 1)
    }
    remaining -= segment.timelineMs
  }
  return last().endMs
}

fun List<SpeedSegment>.timelineTimeAt(sourceMs: Long): Long {
  var elapsed = 0.0
  for (segment in this) {
    if (sourceMs <= segment.endMs) {
      return (elapsed + (sourceMs - segment.startMs).coerceAtLeast(0) / segment.rate.toDouble()).toLong()
    }
    elapsed += segment.timelineMs
  }
  return elapsed.toLong()
}

fun VideoClip.transformAt(sourceMs: Long): TransformKeyframe {
  val right = transformKeyframes.indexOfFirst { it.sourceMs > sourceMs }
  if (transformKeyframes.isEmpty()) return TransformKeyframe(sourceMs)
  if (right == -1) return transformKeyframes.last()
  if (right == 0) return transformKeyframes.first()
  val from = transformKeyframes[right - 1]
  val to = transformKeyframes[right]
  val progress = (sourceMs - from.sourceMs).toFloat() / (to.sourceMs - from.sourceMs)
  return TransformKeyframe(
    sourceMs,
    from.scale + (to.scale - from.scale) * progress,
    from.x + (to.x - from.x) * progress,
    from.y + (to.y - from.y) * progress
  )
}

data class ChromaKey(
  val blue: Boolean = false,
  val threshold: Float = 0.3f,
  val whiteBackground: Boolean = false
)

fun VideoClip.fadeLevelAt(localMs: Long): Float {
  val position = localMs.coerceIn(0L, timelineDurationMs)
  val fadeIn = if (fadeInMs > 0) position.toFloat() / minOf(fadeInMs, timelineDurationMs) else 1f
  val fadeOut = if (fadeOutMs > 0) (timelineDurationMs - position).toFloat() / minOf(fadeOutMs, timelineDurationMs) else 1f
  return minOf(fadeIn, fadeOut).coerceIn(0f, 1f)
}
