package com.example.ocr

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ocr.navigation.MainRoute
import com.example.ocr.navigation.TakenPictureRoute
import com.example.ocr.ui.CapturedScreen
import com.example.ocr.ui.MainScreen
import com.example.ocr.ui.theme.OCRTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      OCRTheme {
        val navController = rememberNavController()
        NavHost(
          navController = navController,
          startDestination = MainRoute,
          enterTransition = { EnterTransition.None },
          exitTransition = { ExitTransition.None },
        ) {
          composable<MainRoute> {
            MainScreen(onCaptured = { uri ->
              Log.d("MainActivity", "Captured image URI: $uri")
              navController.navigate(
                TakenPictureRoute.create(uri)
              )
            })
          }

          composable<TakenPictureRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TakenPictureRoute>()
            val capturedImageUri = route.uri
            CapturedScreen(capturedImageUri = capturedImageUri)
          }
        }
      }
    }
  }
}