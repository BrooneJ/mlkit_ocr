package com.example.ocr.screen.draw.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun CustomAlertDialog(
  onDismissRequest: () -> Unit,
  onConfirm: () -> Unit,
  dialogTitle: String,
  dialogMessage: String,
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(text = dialogTitle) },
    text = { Text(text = dialogMessage) },
    confirmButton = {
      Button(onClick = onConfirm) {
        Text("Confirm")
      }
    },
    dismissButton = {
      Button(onClick = onDismissRequest) {
        Text("Dismiss")
      }
    }
  )
}