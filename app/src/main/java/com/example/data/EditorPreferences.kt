package com.example.data

import android.content.Context

class EditorPreferences(context: Context) {
  private val values = context.getSharedPreferences("editor_preferences", Context.MODE_PRIVATE)

  var haptics: Boolean
    get() = values.getBoolean("haptics", true)
    set(value) { values.edit().putBoolean("haptics", value).apply() }

  var highBitrate: Boolean
    get() = values.getBoolean("high_bitrate", false)
    set(value) { values.edit().putBoolean("high_bitrate", value).apply() }
}
