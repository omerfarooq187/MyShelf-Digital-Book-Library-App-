package com.innovatewithomer.myshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.innovatewithomer.myshelf.ui.home.HomeScreen
import com.innovatewithomer.myshelf.ui.home.SplashScreen
import com.innovatewithomer.myshelf.ui.theme.MyShelfTheme
import com.innovatewithomer.myshelf.utils.HomeScreenRoute
import com.innovatewithomer.myshelf.utils.SplashScreenRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyShelfTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SplashScreenRoute) {
        composable<SplashScreenRoute> {
            SplashScreen(navController = navController)
        }
        composable<HomeScreenRoute> {
            HomeScreen()
        }
    }
}
