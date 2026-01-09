package com.rafgittools

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.rafgittools.core.localization.Language
import com.rafgittools.core.localization.LocalizationManager
import com.rafgittools.data.preferences.PreferencesRepository
import com.rafgittools.ui.components.LanguageFAB
import com.rafgittools.ui.components.LanguageSelector
import com.rafgittools.ui.components.getResponsiveContentWidth
import com.rafgittools.ui.components.getResponsivePadding
import com.rafgittools.ui.theme.RafGitToolsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity for RafGitTools
 * 
 * This is the entry point of the application. It sets up the Compose UI,
 * handles the main navigation, and manages multilingual support.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var localizationManager: LocalizationManager
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val currentLanguage = remember { mutableStateOf(Language.ENGLISH) }
            
            LaunchedEffect(Unit) {
                preferencesRepository.languageFlow.collect { language ->
                    currentLanguage.value = language
                }
            }
            
            RafGitToolsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RafGitToolsApp(
                        currentLanguage = currentLanguage.value,
                        onLanguageChange = { language ->
                            lifecycleScope.launch {
                                preferencesRepository.setLanguage(language)
                                // Note: Activity recreation is used to ensure all resources are properly reloaded.
                                // While this approach is simple and reliable, future iterations could explore
                                // more graceful approaches like configuration changes or recomposition triggers
                                // to improve user experience.
                                recreate()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RafGitToolsApp(
    currentLanguage: Language = Language.ENGLISH,
    onLanguageChange: (Language) -> Unit = {}
) {
    var showLanguageSelector by remember { mutableStateOf(false) }
    val responsivePadding = getResponsivePadding()
    val contentWidth = getResponsiveContentWidth()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            LanguageFAB(
                onClick = { showLanguageSelector = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .let { modifier ->
                        if (contentWidth != null) {
                            modifier.widthIn(max = contentWidth)
                        } else {
                            modifier.fillMaxWidth()
                        }
                    }
                    .padding(responsivePadding),
                contentAlignment = Alignment.Center
            ) {
                WelcomeScreen()
            }
        }
        
        if (showLanguageSelector) {
            LanguageSelector(
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageChange,
                onDismiss = { showLanguageSelector = false }
            )
        }
    }
}

@Composable
fun WelcomeScreen() {
    val context = LocalContext.current
    Text(
        text = context.getString(R.string.app_description) + "\n\n" +
                """
            Welcome to RafGitTools! 🚀
            
            A unified Git/GitHub Android client
            combining the best features from:
            
            • FastHub - GitHub integration
            • MGit - Local Git operations
            • PuppyGit - Modern UI/UX
            • Termux - Terminal capabilities
            
            Development in progress...
            
            Use the language button to change the app language.
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
