package com.jewer.bodycam.frontend.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jewer.bodycam.frontend.screens.CameraScreen
import com.jewer.bodycam.frontend.screens.SettingScreen
import com.jewer.bodycam.ui.theme.Black

// 導航頁面物件
object NAV {
    const val CAMERA = "camera"
    const val SETTING = "setting"
}

// 導航頁面方法
@Composable
fun Navigation(navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = NAV.CAMERA,
        modifier = Modifier
            .fillMaxSize()
            .background(Black),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable(route = NAV.CAMERA) {
            CameraScreen(navController)
        }
        composable(route = NAV.SETTING) {
            SettingScreen(navController)
        }
    }
}