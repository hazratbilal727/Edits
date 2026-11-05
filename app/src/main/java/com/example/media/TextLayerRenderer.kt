package com.example.media

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.util.LruCache
import com.example.model.TextTrackItem
import kotlin.math.*

data class TextMotion(val alpha: Float, val scale: Float, val offsetY: Float, val visibleCharacters: Int)

fun textMotion(track: TextTrackItem, timeMs: Long, editing: Boolean = false): TextMotion {
  if (editing) return TextMotion(track.opacity, 1f, 0f, track.text.length)
  val duration = minOf(track.animationDurationMs.coerceAtLeast(1), (track.endMs - track.startMs).coerceAtLeast(1) / 3).coerceAtLeast(1)
  val entered = ((timeMs - track.startMs).toFloat() / duration).coerceIn(0f, 1f)
  val leaving = ((track.endMs - timeMs).toFloat() / duration).coerceIn(0f, 1f)
  val eased = 1f - (1f - entered).pow(3)
  var alpha = track.opacity
  var scale = 1f
  var y = 0f
  var characters = track.text.length
  when (track.entrance) {
    "Fade" -> alpha *= entered
    "Slide up" -> { y += (1 - eased) * 0.12f; alpha *= entered }
    "Pop" -> { scale *= 0.5f + eased * 0.5f; alpha *= entered }
    "Typewriter" -> characters = track.text.offsetByCodePoints(0, (track.text.codePointCount(0, track.text.length) * entered).toInt())
  }
  when (track.exit) {
    "Fade" -> alpha *= leaving
    "Slide down" -> { y += (1 - leaving) * 0.12f; alpha *= leaving }
    "Shrink" -> { scale *= 0.5f + leaving * 0.5f; alpha *= leaving }
  }
  val seconds = (timeMs - track.startMs) / 1000.0
  when (track.loop) {
    "Pulse" -> scale *= (1 + 0.045 * sin(seconds * 2 * PI)).toFloat()
    "Float" -> y += (0.015 * sin(seconds * 2 * PI / 2.5)).toFloat()
  }
  return TextMotion(alpha.coerceIn(0f, 1f), scale, y, characters)
}

data class TextGeometry(val width: Float, val height: Float, val padding: Float)

/** Uses the same Android text layout and canvas coordinates for preview and MP4 rendering. */
class TextLayerRenderer {
  private data class Key(val width: Int, val track: TextTrackItem, val characters: Int)
  private val cache = LruCache<Key, StaticLayout>(48)
  private val background = Paint(Paint.ANTI_ALIAS_FLAG)

  fun geometry(track: TextTrackItem, width: Int): TextGeometry {
    val layout = layout(track, width, track.text.length)
    val padding = width / 360f * 6f
    return TextGeometry(layout.width + padding * 2, layout.height + padding * 2, padding)
  }

  private fun layout(track: TextTrackItem, width: Int, characters: Int): StaticLayout {
    // Position, timing, scale and rotation do not change glyph layout.
    val style = track.copy(posX = 0f, posY = 0f, scale = 1f, rotation = 0f, startMs = 0, endMs = 0)
    val key = Key(width, style, characters)
    cache.get(key)?.let { return it }
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = parse(track.colorHex, Color.WHITE)
      textSize = track.fontSizeSp * width / 360f
      typeface = Typeface.create(track.fontFamily, when {
        track.bold && track.italic -> Typeface.BOLD_ITALIC
        track.bold -> Typeface.BOLD
        track.italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
      })
      letterSpacing = track.letterSpacing
    }
    val textWidth = ceil(Layout.getDesiredWidth(track.text, paint).toDouble()).toInt()
      .coerceIn(1, (width * 0.9f).toInt().coerceAtLeast(1))
    val content = if (characters < track.text.length) SpannableString(track.text).apply {
      setSpan(ForegroundColorSpan(Color.TRANSPARENT), characters.coerceAtLeast(0), length, 0)
    } else track.text
    val result = StaticLayout.Builder.obtain(content, 0, content.length, paint, textWidth)
      .setAlignment(when (track.alignment) {
        "left" -> Layout.Alignment.ALIGN_NORMAL
        "right" -> Layout.Alignment.ALIGN_OPPOSITE
        else -> Layout.Alignment.ALIGN_CENTER
      })
      .setLineSpacing(0f, track.lineSpacing)
      .setIncludePad(false)
      .build()
    cache.put(key, result)
    return result
  }

  fun draw(canvas: Canvas, track: TextTrackItem, timeMs: Long, editing: Boolean = false, frameWidth: Int = canvas.width, frameHeight: Int = canvas.height) {
    val motion = textMotion(track, timeMs, editing)
    val layout = layout(track, frameWidth, motion.visibleCharacters)
    val padding = frameWidth / 360f * 6f
    val width = layout.width + padding * 2
    val height = layout.height + padding * 2
    val paint = layout.paint
    canvas.save()
    canvas.translate(track.posX * frameWidth, (track.posY + motion.offsetY) * frameHeight)
    canvas.rotate(track.rotation)
    canvas.scale(track.scale * motion.scale, track.scale * motion.scale)
    canvas.translate(-width / 2f, -height / 2f)
    background.color = parse(track.backgroundHex, Color.TRANSPARENT)
    background.alpha = (Color.alpha(background.color) * motion.alpha).toInt()
    canvas.drawRoundRect(0f, 0f, width, height, padding, padding, background)
    canvas.translate(padding, padding)
    paint.alpha = (motion.alpha * Color.alpha(paint.color)).toInt()
    paint.style = Paint.Style.FILL
    paint.color = parse(track.colorHex, Color.WHITE)
    paint.alpha = (motion.alpha * Color.alpha(paint.color)).toInt()
    if (track.shadow) paint.setShadowLayer(frameWidth / 180f, 0f, frameWidth / 360f, Color.BLACK)
    else paint.clearShadowLayer()
    if (track.strokeWidth > 0) {
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = track.strokeWidth * frameWidth / 360f
      paint.strokeJoin = Paint.Join.ROUND
      paint.color = parse(track.strokeHex, Color.BLACK)
      paint.alpha = (motion.alpha * Color.alpha(paint.color)).toInt()
      layout.draw(canvas)
      paint.style = Paint.Style.FILL
      paint.color = parse(track.colorHex, Color.WHITE)
      paint.alpha = (motion.alpha * Color.alpha(paint.color)).toInt()
    }
    layout.draw(canvas)
    canvas.restore()
  }

  fun localPoint(track: TextTrackItem, point: PointF, width: Int, height: Int): PointF {
    val x = point.x - track.posX * width
    val y = point.y - track.posY * height
    val angle = Math.toRadians(-track.rotation.toDouble())
    return PointF(((x * cos(angle) - y * sin(angle)) / track.scale).toFloat(),
      ((x * sin(angle) + y * cos(angle)) / track.scale).toFloat())
  }

  companion object {
    fun parse(value: String, fallback: Int): Int = runCatching { Color.parseColor(value) }.getOrDefault(fallback)
  }
}
