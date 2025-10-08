package com.example.ocr.screen.ocr.utils

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ScheduleFormat(
  val date: String,
  val duty: String
)

fun parseScheduleListFromString(raw: String): List<ScheduleFormat> {
  val entries = raw
    .removePrefix("```json")
    .removePrefix("```JSON")
    .removeSuffix("```")
    .trim()

  val json = Json { ignoreUnknownKeys = true }
  val scheduleList: List<ScheduleFormat> =
    try {
      json.decodeFromString(entries)
    } catch (e: Exception) {
      Log.e("JSONDecodeError", "Failed to decode JSON: ${e.message}")
      emptyList()
    }

  return scheduleList
}