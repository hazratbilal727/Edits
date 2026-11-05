package com.example.media

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.example.model.ChromaKey
import com.example.model.VideoClip

@UnstableApi
fun VideoClip.cleanupEffect(): GlEffect? =
  if (chromaKey == null && videoDenoise == 0f) null else VideoCleanupEffect(chromaKey, videoDenoise)

@UnstableApi
private data class VideoCleanupEffect(val key: ChromaKey?, val denoise: Float) : GlEffect {
  override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
    CleanupShader(key, denoise, useHdr)
}

@UnstableApi
private class CleanupShader(key: ChromaKey?, denoise: Float, useHdr: Boolean) :
  BaseGlShaderProgram(useHdr, 1) {
  private val program = try {
    GlProgram(VERTEX_SHADER, FRAGMENT_SHADER)
  } catch (error: GlUtil.GlException) {
    throw VideoFrameProcessingException(error)
  }

  init {
    program.setBufferAttribute("aPosition", GlUtil.getNormalizedCoordinateBounds(), 4)
    program.setFloatUniform("uDenoise", denoise)
    program.setFloatUniform("uKeyEnabled", if (key == null) 0f else 1f)
    program.setFloatUniform("uBlueKey", if (key?.blue == true) 1f else 0f)
    program.setFloatUniform("uThreshold", key?.threshold ?: 0f)
    program.setFloatUniform("uBackground", if (key?.whiteBackground == true) 1f else 0f)
  }

  override fun configure(inputWidth: Int, inputHeight: Int): Size {
    program.setFloatUniform("uTexelX", 1f / inputWidth)
    program.setFloatUniform("uTexelY", 1f / inputHeight)
    return Size(inputWidth, inputHeight)
  }

  override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
    try {
      program.use()
      program.setSamplerTexIdUniform("uTexture", inputTexId, 0)
      program.bindAttributesAndUniforms()
      GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
      GlUtil.checkGlError()
    } catch (error: GlUtil.GlException) {
      throw VideoFrameProcessingException(error, presentationTimeUs)
    }
  }

  override fun release() {
    try {
      super.release()
    } finally {
      try {
        program.delete()
      } catch (error: GlUtil.GlException) {
        throw VideoFrameProcessingException(error)
      }
    }
  }
}

private const val VERTEX_SHADER = """
attribute vec4 aPosition;
varying vec2 vUv;
void main() {
  gl_Position = aPosition;
  vUv = aPosition.xy * 0.5 + 0.5;
}
"""

private const val FRAGMENT_SHADER = """
precision mediump float;
uniform sampler2D uTexture;
uniform float uDenoise;
uniform float uKeyEnabled;
uniform float uBlueKey;
uniform float uThreshold;
uniform float uBackground;
uniform float uTexelX;
uniform float uTexelY;
varying vec2 vUv;

void main() {
  vec4 pixel = texture2D(uTexture, vUv);
  vec3 rgb = pixel.rgb;
  if (uDenoise > 0.0) {
    vec3 sum = rgb;
    float total = 1.0;
    for (int y = -1; y <= 1; y++) {
      for (int x = -1; x <= 1; x++) {
        if (x != 0 || y != 0) {
          vec2 offset = vec2(float(x) * uTexelX, float(y) * uTexelY);
          vec3 neighbor = texture2D(uTexture, clamp(vUv + offset, 0.0, 1.0)).rgb;
          vec3 difference = rgb - neighbor;
          float weight = exp(-dot(difference, difference) / (0.002 + 0.025 * uDenoise));
          sum += neighbor * weight;
          total += weight;
        }
      }
    }
    rgb = mix(rgb, sum / total, uDenoise);
  }
  if (uKeyEnabled > 0.5) {
    float target = mix(rgb.g, rgb.b, uBlueKey);
    float other = max(rgb.r, mix(rgb.b, rgb.g, uBlueKey));
    float dominance = (target - other) / max(target, 0.08);
    float amount = smoothstep(uThreshold, uThreshold + 0.12, dominance);
    rgb = mix(rgb, vec3(uBackground), amount);
  }
  gl_FragColor = vec4(rgb, pixel.a);
}
"""
