package com.rafgittools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rafgittools.ui.screens.actions.ActionsControlScreen
import com.rafgittools.ui.screens.auth.AuthScreen
import com.rafgittools.ui.theme.RafGitToolsTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Independent launcher for workflow operator controls. No PAT is copied out
 * of repository Secrets; all requests use the existing device login.
 */
@AndroidEntryPoint
class ActionsControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RafGitToolsTheme {
                var showingAuth by remember { mutableStateOf(false) }
                if (showingAuth) {
                    AuthScreen(onAuthSuccess = { showingAuth = false })
                } else {
                    ActionsControlScreen(
                        onBack = { finish() },
                        onAuthentication = { showingAuth = true }
                    )
                }
            }
        }
    }
}
