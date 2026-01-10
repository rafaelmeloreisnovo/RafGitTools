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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rafgittools.core.localization.Language
import com.rafgittools.core.localization.LocalizationManager
import com.rafgittools.data.preferences.PreferencesRepository
import com.rafgittools.ui.components.LanguageFAB
import com.rafgittools.ui.components.LanguageSelector
import com.rafgittools.ui.components.getResponsiveContentWidth
import com.rafgittools.ui.components.getResponsivePadding
import com.rafgittools.ui.navigation.Screen
import com.rafgittools.ui.screens.auth.AuthScreen
import com.rafgittools.ui.screens.branches.BranchListScreen
import com.rafgittools.ui.screens.commits.CommitListScreen
import com.rafgittools.ui.screens.home.HomeScreen
import com.rafgittools.ui.screens.issues.IssueListScreen
import com.rafgittools.ui.screens.pullrequests.PullRequestListScreen
import com.rafgittools.ui.screens.repository.RepositoryDetailScreen
import com.rafgittools.ui.screens.repository.RepositoryListScreen
import com.rafgittools.ui.screens.settings.SettingsScreen
import com.rafgittools.ui.theme.RafGitToolsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.net.URLDecoder
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
    val navController = rememberNavController()
    
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    },
                    onNavigateToRepository = { repo ->
                        // Navigate to GitHub repository issues/PRs
                        navController.navigate(Screen.IssueList.createRoute(repo.owner.login, repo.name))
                    }
                )
            }
            
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.RepositoryList.route) {
                RepositoryListScreen(
                    onRepositoryClick = { repo ->
                        navController.navigate(Screen.RepositoryDetail.createRoute(repo.path))
                    },
                    onAddRepository = {
                        // TODO: Navigate to add repository screen
                    }
                )
            }
            
            composable(
                route = Screen.RepositoryDetail.route,
                arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
            ) { backStackEntry ->
                val repoPath = backStackEntry.arguments?.getString("repoPath")?.let {
                    URLDecoder.decode(it, "UTF-8")
                } ?: ""
                RepositoryDetailScreen(
                    repoPath = repoPath,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCommits = { path ->
                        navController.navigate(Screen.CommitList.createRoute(path))
                    },
                    onNavigateToBranches = { path ->
                        navController.navigate(Screen.BranchList.createRoute(path))
                    }
                )
            }
            
            composable(
                route = Screen.CommitList.route,
                arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
            ) { backStackEntry ->
                val repoPath = backStackEntry.arguments?.getString("repoPath")?.let {
                    URLDecoder.decode(it, "UTF-8")
                } ?: ""
                CommitListScreen(
                    repoPath = repoPath,
                    onNavigateBack = { navController.popBackStack() },
                    onCommitClick = { /* TODO: Navigate to commit detail */ }
                )
            }
            
            composable(
                route = Screen.BranchList.route,
                arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
            ) { backStackEntry ->
                val repoPath = backStackEntry.arguments?.getString("repoPath")?.let {
                    URLDecoder.decode(it, "UTF-8")
                } ?: ""
                BranchListScreen(
                    repoPath = repoPath,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLanguageChange = onLanguageChange
                )
            }
            
            composable(
                route = Screen.IssueList.route,
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("repo") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                IssueListScreen(
                    owner = owner,
                    repo = repo,
                    onNavigateBack = { navController.popBackStack() },
                    onIssueClick = { /* TODO: Navigate to issue detail */ },
                    onCreateIssue = { /* TODO: Navigate to create issue */ }
                )
            }
            
            composable(
                route = Screen.PullRequestList.route,
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("repo") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val owner = backStackEntry.arguments?.getString("owner") ?: ""
                val repo = backStackEntry.arguments?.getString("repo") ?: ""
                PullRequestListScreen(
                    owner = owner,
                    repo = repo,
                    onNavigateBack = { navController.popBackStack() },
                    onPullRequestClick = { /* TODO: Navigate to PR detail */ }
                )
            }
        }
        
        // Language FAB
        FloatingActionButton(
            onClick = { showLanguageSelector = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = getLanguageFlag(currentLanguage),
                style = MaterialTheme.typography.titleLarge
            )
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

/**
 * Get flag emoji for language
 */
private fun getLanguageFlag(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "🇺🇸"
        Language.PORTUGUESE -> "🇧🇷"
        Language.SPANISH -> "🇪🇸"
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RafGitToolsTheme {
        RafGitToolsApp()
    }
}
