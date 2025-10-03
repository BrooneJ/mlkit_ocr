package com.example.ocr.screen.ocr

enum class OcrType {
  ADAPTIVE,
  FROMVALLEY,
  FROMPEAK,
  FROMWIDTH,
  ENFORCE
}

sealed interface OcrAction {
  data class CardChosen(val type: OcrType) : OcrAction
}