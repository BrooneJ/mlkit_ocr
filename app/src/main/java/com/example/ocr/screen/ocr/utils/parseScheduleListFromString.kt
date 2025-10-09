package com.example.ocr.screen.ocr.utils

import com.example.ocr.di.AppJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class ScheduleFormat(
  val date: String,
  val duty: String
)

class ScheduleParser @Inject constructor(@AppJson private val json: Json) {
  fun parse(raw: String): List<ScheduleFormat> = try {
    val cleaned = raw
      .replaceFirst(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
      .removeSuffix("```")
      .trim()
    json.decodeFromString(cleaned)
  } catch (_: Exception) {
    emptyList()
  }
}
