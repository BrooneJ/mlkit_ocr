package com.example.ocr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ocr.navigation.CropRoute
import com.example.ocr.navigation.DrawRoute
import com.example.ocr.navigation.MainRoute
import com.example.ocr.navigation.OcrRoute
import com.example.ocr.navigation.TakenPictureRoute
import com.example.ocr.screen.captured.CapturedScreen
import com.example.ocr.screen.crop.CropScreen
import com.example.ocr.screen.draw.DrawScreen
import com.example.ocr.screen.draw.DrawViewModel
import com.example.ocr.screen.main.MainScreen
import com.example.ocr.screen.ocr.OcrScreen
import com.example.ocr.ui.theme.OCRTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      OCRTheme {
        val navController = rememberNavController()
        val drawViewModel = viewModel<DrawViewModel>()
        val state by drawViewModel.state.collectAsStateWithLifecycle()

        NavHost(
          navController = navController,
          startDestination = MainRoute,
          enterTransition = { EnterTransition.None },
          exitTransition = { ExitTransition.None },
        ) {
          composable<MainRoute> {
            MainScreen(
              onCaptured = { uri ->
                navController.navigate(
                  TakenPictureRoute.create(uri)
                )
              }
            )
          }

          composable<TakenPictureRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TakenPictureRoute>()
            val capturedImageUri = route.uri
            CapturedScreen(
              capturedImageUri = capturedImageUri,
              onCrop = {
                navController.navigate(
                  CropRoute.create(capturedImageUri)
                )
              },
              onDraw = {
                navController.navigate(
                  DrawRoute.create(capturedImageUri)
                )
              },
              onBack = {
                navController.popBackStack()
              },
              onConfirm = {
                navController.navigate(
                  OcrRoute.create(capturedImageUri)
                )
              }
            )
          }

          composable<CropRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CropRoute>()
            val capturedImageUri = route.uri
            CropScreen(
              capturedImageUri = capturedImageUri,
              onCropComplete = { croppedUri ->
                navController.navigate(
                  TakenPictureRoute.create(croppedUri)
                ) {
                  popUpTo<TakenPictureRoute> { inclusive = true }
                  launchSingleTop = true
                }
              },
              onBack = {
                navController.popBackStack()
              }
            )
          }

          composable<DrawRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DrawRoute>()
            val capturedImageUri = route.uri
            DrawScreen(
              capturedImageUri = capturedImageUri,
              onAction = drawViewModel::onAction,
              onExported = { exportedUri ->
                navController.navigate(
                  TakenPictureRoute.create(exportedUri)
                ) {
                  popUpTo<TakenPictureRoute> { inclusive = true }
                  launchSingleTop = true
                }
              },
              onBack = {
                navController.popBackStack()
              },
              paths = state.paths,
              currentPath = state.currentPath,
              thickness = state.thickness
            )
          }

          composable<OcrRoute> { backStackEntry ->
            OcrScreen(
              onBack = {
                navController.popBackStack()
              }
            )
          }
        }
      }
    }
  }
}