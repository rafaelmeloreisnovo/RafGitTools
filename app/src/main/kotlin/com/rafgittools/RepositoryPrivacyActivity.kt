package com.rafgittools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rafgittools.ui.screens.auth.AuthScreen
import com.rafgittools.ui.screens.privacy.RepositoryPrivacyScreen
import com.rafgittools.ui.theme.RafGitToolsTheme
import dagger.hilt.android.AndroidEntryPoint

/** Dedicated launcher surface for privacy governance without disturbing MainActivity navigation. */
@AndroidEntryPoint
class RepositoryPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RafGitToolsTheme {
                var showingAuth by remember { mutableStateOf(false) }
                if (showingAuth) {
                    AuthScreen(
                        onAuthSuccess = {
                            showingAuth = false
                            recreate()
                        }
                    )
                } else {
                    RepositoryPrivacyScreen(
                        onNavigateBack = { finish() },
                        onNavigateToAuth = { showingAuth = true }
                    )
                }
            }
        }
    }
}
