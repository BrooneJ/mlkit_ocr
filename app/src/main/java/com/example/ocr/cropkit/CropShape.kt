package com.example.ocr.cropkit

sealed class CropShape {
  data object FreeForm : CropShape()
  data object Original : CropShape()
  data class AspectRatio(val ratio: Float) : CropShape()
}