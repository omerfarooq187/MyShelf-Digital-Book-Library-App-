package com.innovatewithomer.myshelf.ui.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.innovatewithomer.myshelf.R
import com.innovatewithomer.myshelf.utils.HomeScreenRoute
import com.innovatewithomer.myshelf.utils.LoginScreenRoute
import com.innovatewithomer.myshelf.viewmodel.AuthState
import com.innovatewithomer.myshelf.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(authViewModel: AuthViewModel = hiltViewModel(), navController: NavController) {
    val state by authViewModel.userState.collectAsState()
    val context = LocalContext.current
    when(state) {
        is AuthState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AuthState.Authenticated -> {
            navController.navigate(HomeScreenRoute) {
                popUpTo(0) {inclusive = true}
            }
        }
        is AuthState.Error -> {
            Toast.makeText(context, "Authentication error for backup", Toast.LENGTH_SHORT).show()
        }
        is AuthState.UnAuthenticated -> {
           navController.navigate(LoginScreenRoute) {
               popUpTo(0) { inclusive = true }
           }
        }
    }

    LaunchedEffect(Unit) {
        delay(3500)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column {
            Text(
                text = "My Shelf",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.book),
                contentDescription = "myShelf_icon",
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My personal Library",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}