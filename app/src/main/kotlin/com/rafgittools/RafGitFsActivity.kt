package com.rafgittools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rafgittools.ui.screens.rafgitfs.RafGitFsUiPaths
import com.rafgittools.ui.screens.rafgitfs.RepositoryStorageScreen
import com.rafgittools.ui.screens.rafgitfs.StorageProfilesScreen
import com.rafgittools.ui.screens.rafgitfs.StorageSettingsScreen
import com.rafgittools.ui.screens.rafgitfs.VirtualFileBrowserScreen
import com.rafgittools.ui.screens.rafgitfs.VirtualFileViewerScreen
import com.rafgittools.ui.theme.RafGitToolsTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder

@AndroidEntryPoint
class RafGitFsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RafGitToolsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = RafGitFsRoute.Profiles.route
                    ) {
                        composable(RafGitFsRoute.Profiles.route) {
                            StorageProfilesScreen(
                                onNavigateBack = { finish() },
                                onOpenRepositories = { profileId ->
                                    navController.navigate(RafGitFsRoute.Repositories.create(profileId))
                                },
                                onOpenSettings = { profileId ->
                                    navController.navigate(RafGitFsRoute.Settings.create(profileId))
                                }
                            )
                        }
                        composable(
                            route = RafGitFsRoute.Repositories.route,
                            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
                        ) {
                            RepositoryStorageScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onOpenRepository = { repository ->
                                    navController.navigate(
                                        RafGitFsRoute.Browser.create(
                                            profileId = it.arguments?.getString("profileId").orEmpty(),
                                            repository = repository.fullName,
                                            ref = repository.defaultBranch,
                                            path = ""
                                        )
                                    )
                                }
                            )
                        }
                        composable(
                            route = RafGitFsRoute.Browser.route,
                            arguments = listOf(
                                navArgument("profileId") { type = NavType.StringType },
                                navArgument("repositoryFullName") { type = NavType.StringType },
                                navArgument("refName") { type = NavType.StringType },
                                navArgument("path") { type = NavType.StringType }
                            )
                        ) {
                            VirtualFileBrowserScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onOpenFile = { profileId, repository, ref, path ->
                                    navController.navigate(
                                        RafGitFsRoute.Viewer.create(profileId, repository, ref, path)
                                    )
                                },
                                onOpenSettings = { profileId ->
                                    navController.navigate(RafGitFsRoute.Settings.create(profileId))
                                }
                            )
                        }
                        composable(
                            route = RafGitFsRoute.Viewer.route,
                            arguments = listOf(
                                navArgument("profileId") { type = NavType.StringType },
                                navArgument("repositoryFullName") { type = NavType.StringType },
                                navArgument("refName") { type = NavType.StringType },
                                navArgument("path") { type = NavType.StringType }
                            )
                        ) {
                            VirtualFileViewerScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = RafGitFsRoute.Settings.route,
                            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
                        ) {
                            StorageSettingsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed class RafGitFsRoute(val route: String) {
    object Profiles : RafGitFsRoute("rafgitfs_profiles")
    object Repositories : RafGitFsRoute("rafgitfs_repositories/{profileId}") {
        fun create(profileId: String) = "rafgitfs_repositories/${encode(profileId)}"
    }
    object Browser : RafGitFsRoute(
        "rafgitfs_browser/{profileId}/{repositoryFullName}/{refName}/{path}"
    ) {
        fun create(profileId: String, repository: String, ref: String, path: String) =
            "rafgitfs_browser/${encode(profileId)}/${encode(repository)}/${encode(ref)}/${encode(RafGitFsUiPaths.routeValue(path))}"
    }
    object Viewer : RafGitFsRoute(
        "rafgitfs_viewer/{profileId}/{repositoryFullName}/{refName}/{path}"
    ) {
        fun create(profileId: String, repository: String, ref: String, path: String) =
            "rafgitfs_viewer/${encode(profileId)}/${encode(repository)}/${encode(ref)}/${encode(RafGitFsUiPaths.routeValue(path))}"
    }
    object Settings : RafGitFsRoute("rafgitfs_settings/{profileId}") {
        fun create(profileId: String) = "rafgitfs_settings/${encode(profileId)}"
    }
}

private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
