package com.rafgittools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafgittools.ui.theme.RafGitToolsTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for RafGitTools
 * 
 * This is the entry point of the application. It sets up the Compose UI
 * and handles the main navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            RafGitToolsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RafGitToolsApp()
                }
            }
        }
    }
}

@Composable
fun RafGitToolsApp() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            WelcomeScreen()
        }
    }
}

@Composable
fun WelcomeScreen() {
    Text(
        text = """
            Welcome to RafGitTools! 🚀
            
            A unified Git/GitHub Android client
            combining the best features from:
            
            • FastHub - GitHub integration
            • MGit - Local Git operations
            • PuppyGit - Modern UI/UX
            • Termux - Terminal capabilities
            
            Development in progress...
        """.trimIndent(),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RafGitToolsTheme {
        WelcomeScreen()
    }
}
