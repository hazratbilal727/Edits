package com.example.media

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
import kotlin.random.Random

fun createSoundEffect(directory: File, name: String, durationMs: Long): File {
  val rate = 44100
  val samples = (durationMs.coerceIn(100L, 3000L) * rate / 1000).toInt()
  val file = File.createTempFile("sound-", ".wav", directory)
  val random = Random(7)
  val pcm = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN)
  var phase = 0.0
  for (index in 0 until samples) {
    val seconds = index.toDouble() / rate
    val progress = index.toDouble() / samples
    val attack = minOf(1.0, seconds / 0.012)
    val release = minOf(1.0, (1 - progress) * 12)
    val value = when (name) {
      "Soft chime" -> (sin(2 * PI * 880 * seconds) + 0.35 * sin(2 * PI * 1760 * seconds)) * exp(-seconds * 4)
      "Pop" -> sin(2 * PI * (460 * seconds - 160 * seconds * seconds)) * exp(-seconds * 18)
      "Whoosh" -> (random.nextDouble() * 2 - 1) * sin(PI * progress).pow(2) * 0.6
      else -> {
        phase += 2 * PI * (220 + 1500 * progress.pow(2)) / rate
        sin(phase) * sin(PI * progress)
      }
    }
    pcm.putShort((value * attack * release * 15000).toInt().coerceIn(-32768, 32767).toShort())
  }
  val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
  header.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + samples * 2)
  header.put("WAVEfmt ".toByteArray(Charsets.US_ASCII)).putInt(16)
  header.putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
  header.put("data".toByteArray(Charsets.US_ASCII)).putInt(samples * 2)
  try {
    FileOutputStream(file).use { it.write(header.array()); it.write(pcm.array()) }
    return file
  } catch (error: Exception) {
    file.delete()
    throw error
  }
}
