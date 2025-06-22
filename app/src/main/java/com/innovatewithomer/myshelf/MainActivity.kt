// MainActivity.kt
package com.innovatewithomer.myshelf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.innovatewithomer.myshelf.ui.home.ExploreScreen
import com.innovatewithomer.myshelf.ui.home.HomeScreenContent
import com.innovatewithomer.myshelf.ui.home.SplashScreen
import com.innovatewithomer.myshelf.ui.login.SignInScreen
import com.innovatewithomer.myshelf.ui.theme.MyShelfTheme
import com.innovatewithomer.myshelf.utils.HomeScreenRoute
import com.innovatewithomer.myshelf.utils.LoginScreenRoute
import com.innovatewithomer.myshelf.utils.SplashScreenRoute
import com.innovatewithomer.myshelf.viewmodel.BookViewModel
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
        composable<LoginScreenRoute> {
            SignInScreen {
                navController.navigate(HomeScreenRoute) {
                    popUpTo(LoginScreenRoute) { inclusive = true }
                }
            }
        }
        composable<HomeScreenRoute> {
            MainTabScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem(title = "My Books", icon = Icons.Default.Book),
        TabItem(title = "Explore", icon = Icons.Default.Explore)
    )

    // Get context and ViewModel
    val context = LocalContext.current
    val bookViewModel: BookViewModel = hiltViewModel()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "offline_user"

    // Create the PDF picker launcher here
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            bookViewModel.addBook(userId, it)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Text(
                    text = "My Shelf",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        bottomBar = {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(64.dp),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = Color.Black,
                ) {
                    tabs.forEachIndexed { index, tabItem ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = tabItem.title,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    color = if (selectedTabIndex == index) Color.Black else Color.Gray
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = tabItem.icon,
                                    contentDescription = tabItem.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (selectedTabIndex == index) Color.Black else Color.Gray
                                )
                            }
                        )
                    }
                }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = {
                        // Launch the PDF picker directly from here
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Book")
                }
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                when (selectedTabIndex) {
                    0 -> HomeScreenContent(
                        modifier = Modifier.fillMaxSize(),
                        bookViewModel = bookViewModel,
                        userId = userId
                    )
                    1 -> ExploreScreen(Modifier.fillMaxSize())
                }
            }
        }
    )
}

data class TabItem(val title: String, val icon: ImageVector)