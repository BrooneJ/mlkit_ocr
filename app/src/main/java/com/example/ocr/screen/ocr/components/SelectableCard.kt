package com.example.ocr.screen.ocr.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun SelectableCard(
  modifier: Modifier = Modifier,
  bitmap: Bitmap?,
  onClick: () -> Unit,
) {
  if (bitmap != null) {
    Card(
      shape = RoundedCornerShape(10.dp),
      elevation = CardDefaults.cardElevation(12.dp),
      colors = CardDefaults.cardColors(Color.White),
      onClick = onClick
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 8.dp)
      ) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = null,
          modifier = Modifier
            .fillMaxWidth()
        )
      }
    }
  } else {
    Text(text = "No test image available", modifier = Modifier.padding(12.dp))
  }
}