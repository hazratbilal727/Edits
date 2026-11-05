package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.media.FaceDetector
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.example.model.TransformKeyframe
import com.example.model.VideoClip
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.max
import kotlin.math.pow

/** Produces editable pan and zoom keyframes from sampled frontal face positions. */
@Suppress("DEPRECATION")
suspend fun reframeFaces(context: Context, clip: VideoClip, canvasRatio: Float): List<TransformKeyframe> {
  val retriever = MediaMetadataRetriever()
  try {
    retriever.setDataSource(context, Uri.parse(requireNotNull(clip.uriString)))
    val stepMs = max(750L, (clip.endMs - clip.startMs) / 24)
    var previousFace: PointF? = null
    var previousKey: TransformKeyframe? = null
    val keys = mutableListOf<TransformKeyframe>()

    for (timeMs in clip.startMs until clip.endMs step stepMs) {
      currentCoroutineContext().ensureActive()
      val frame = if (Build.VERSION.SDK_INT >= 27) {
        retriever.getScaledFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST, 480, 480)
      } else {
        retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
      } ?: continue
      try {
        val candidates = faceCenters(frame)
        val last = previousFace
        val center = if (last == null) candidates.maxByOrNull { it.second }?.first
        else candidates.minByOrNull { (point, _) ->
          (point.x - last.x).pow(2) + (point.y - last.y).pow(2)
        }?.first
        if (center == null) continue
        previousFace = center

        val sourceRatio = frame.width.toFloat() / frame.height
        val zoom = max(sourceRatio / canvasRatio, canvasRatio / sourceRatio)
        val width = zoom * minOf(1f, sourceRatio / canvasRatio)
        val height = zoom * minOf(1f, canvasRatio / sourceRatio)
        val maxX = ((width - 1f) / 2f).coerceAtLeast(0f)
        val maxY = ((height - 1f) / 2f).coerceAtLeast(0f)
        val desiredX = ((0.5f - center.x) * width).coerceIn(-maxX, maxX)
        val desiredY = ((0.4f - center.y) * height).coerceIn(-maxY, maxY)
        val x = previousKey?.let { it.x * 0.35f + desiredX * 0.65f } ?: desiredX
        val y = previousKey?.let { it.y * 0.35f + desiredY * 0.65f } ?: desiredY
        val key = TransformKeyframe(timeMs, zoom, x, y)
        previousKey = key
        keys.add(key)
      } finally {
        frame.recycle()
      }
    }
    return keys
  } finally {
    retriever.release()
  }
}

@Suppress("DEPRECATION")
private fun faceCenters(frame: Bitmap): List<Pair<PointF, Float>> {
  if (frame.width < 2 || frame.height < 2) return emptyList()
  val scale = minOf(1f, 480f / max(frame.width, frame.height))
  val width = ((frame.width * scale).toInt() / 2 * 2).coerceAtLeast(2)
  val height = (frame.height * scale).toInt().coerceAtLeast(2)
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
  try {
    android.graphics.Canvas(bitmap).drawBitmap(
      frame, null, android.graphics.Rect(0, 0, width, height),
      android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    )
    val faces = arrayOfNulls<FaceDetector.Face>(5)
    val count = FaceDetector(width, height, faces.size).findFaces(bitmap, faces)
    return faces.take(count).mapNotNull { face ->
      if (face == null || face.confidence() < 0.5f) return@mapNotNull null
      val point = PointF()
      face.getMidPoint(point)
      PointF(point.x / width, point.y / height) to face.eyesDistance()
    }
  } finally {
    bitmap.recycle()
  }
}
