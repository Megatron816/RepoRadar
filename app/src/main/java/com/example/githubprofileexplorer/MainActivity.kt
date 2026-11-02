package com.example.githubprofileexplorer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import com.example.githubprofileexplorer.di.AppViewModelFactory
import com.example.githubprofileexplorer.domain.DeepLinkValidator
import com.example.githubprofileexplorer.ui.AppRoot
import com.example.githubprofileexplorer.ui.SplashScreen
import com.example.githubprofileexplorer.ui.theme.GitHubProfileExplorerTheme

class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy {
        AppViewModelFactory((application as GitHubExplorerApp).container)
    }

    private var deepLinkUsername by mutableStateOf<String?>(null)
    private var showSplash by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkUsername = DeepLinkValidator.username(intent?.data)
        setContent {
            GitHubProfileExplorerTheme {
                if (showSplash) {
                    SplashScreen()
                    LaunchedEffect(Unit) {
                        delay(900)
                        showSplash = false
                    }
                } else {
                    AppRoot(
                        viewModelFactory = viewModelFactory,
                        deepLinkUsername = deepLinkUsername
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUsername = DeepLinkValidator.username(intent.data)
    }
}
