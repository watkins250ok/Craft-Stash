package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.CraftStashViewModel
import com.example.ui.ideas.IdeasScreen
import com.example.ui.locations.StorageLocationsScreen
import com.example.ui.projects.AddEditProjectScreen
import com.example.ui.projects.ProjectDetailScreen
import com.example.ui.projects.ProjectsScreen
import com.example.ui.scanner.BarcodeScannerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.stash.AddEditSupplyScreen
import com.example.ui.stash.DraftInboxScreen
import com.example.ui.stash.StashScreen
import com.example.ui.stash.SupplyDetailScreen
import com.example.ui.theme.CraftStashTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CraftStashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                com.example.domain.model.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.example.domain.model.ThemeMode.LIGHT -> false
                com.example.domain.model.ThemeMode.DARK -> true
            }

            CraftStashTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val uiMessage by viewModel.message.collectAsState()
                val selectedTab by viewModel.selectedTab.collectAsState()

                // Display snackbar when message is posted
                LaunchedEffect(uiMessage) {
                    uiMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearMessage()
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(
                            windowInsets = NavigationBarDefaults.windowInsets,
                            modifier = Modifier
                                .testTag("bottom_nav_bar")
                        ) {
                            val isMain = currentRoute == "main"
                            NavigationBarItem(
                                selected = isMain && selectedTab == 0,
                                onClick = {
                                    viewModel.selectTab(0)
                                    if (!isMain) navController.navigate("main") { popUpTo("main") { inclusive = false } }
                                },
                                icon = {
                                    Icon(
                                        if (isMain && selectedTab == 0) Icons.Default.Inventory else Icons.Outlined.Inventory2,
                                        contentDescription = "Stash",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text("Stash", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_stash")
                            )
                            NavigationBarItem(
                                selected = isMain && selectedTab == 1,
                                onClick = {
                                    viewModel.selectTab(1)
                                    if (!isMain) navController.navigate("main") { popUpTo("main") { inclusive = false } }
                                },
                                icon = {
                                    Icon(
                                        if (isMain && selectedTab == 1) Icons.Default.DesignServices else Icons.Outlined.DesignServices,
                                        contentDescription = "Projects",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text("Projects", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_projects")
                            )
                            NavigationBarItem(
                                selected = isMain && selectedTab == 2,
                                onClick = {
                                    viewModel.selectTab(2)
                                    if (!isMain) navController.navigate("main") { popUpTo("main") { inclusive = false } }
                                },
                                icon = {
                                    Icon(
                                        if (isMain && selectedTab == 2) Icons.Default.Lightbulb else Icons.Outlined.Lightbulb,
                                        contentDescription = "Ideas",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text("Ideas", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_ideas")
                            )
                            NavigationBarItem(
                                selected = currentRoute == "draft_inbox",
                                onClick = {
                                    navController.navigate("draft_inbox") { launchSingleTop = true }
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = "Drafts",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text("Drafts", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_drafts")
                            )
                            NavigationBarItem(
                                selected = currentRoute == "locations",
                                onClick = {
                                    navController.navigate("locations") { launchSingleTop = true }
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = "Locations",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text("Locs", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_locations")
                            )
                            NavigationBarItem(
                                selected = currentRoute == "settings",
                                onClick = {
                                    navController.navigate("settings") { launchSingleTop = true }
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Main Bottom-Nav Screen (Stash, Projects, Ideas)
                        composable("main") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (selectedTab) {
                                    0 -> StashScreen(
                                        viewModel = viewModel,
                                        onNavigateToDetail = { id -> navController.navigate("supply_detail/$id") },
                                        onNavigateToDetailedAdd = { navController.navigate("add_supply") },
                                        onNavigateToScanner = { navController.navigate("scanner") }
                                    )
                                    1 -> ProjectsScreen(
                                        viewModel = viewModel,
                                        onNavigateToProjectDetail = { id -> navController.navigate("project_detail/$id") },
                                        onNavigateToAddProject = { navController.navigate("add_project") }
                                    )
                                    2 -> IdeasScreen(
                                        viewModel = viewModel,
                                        onNavigateToAddProject = { navController.navigate("add_project") }
                                    )
                                }
                            }
                        }

                        // Supply Detail
                        composable(
                            route = "supply_detail/{supplyId}",
                            arguments = listOf(navArgument("supplyId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val supplyId = backStackEntry.arguments?.getString("supplyId").orEmpty()
                            SupplyDetailScreen(
                                supplyId = supplyId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToEdit = { id -> navController.navigate("edit_supply/$id") },
                                onNavigateToIdeasWithSupply = { id ->
                                    viewModel.clearSelection()
                                    viewModel.toggleSupplySelection(id)
                                    viewModel.selectTab(2)
                                    navController.popBackStack("main", inclusive = false)
                                }
                            )
                        }

                        // Add Supply
                        composable("add_supply") {
                            AddEditSupplyScreen(
                                supplyId = null,
                                prefilledBarcode = null,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Edit Supply
                        composable(
                            route = "edit_supply/{supplyId}",
                            arguments = listOf(navArgument("supplyId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val supplyId = backStackEntry.arguments?.getString("supplyId")
                            AddEditSupplyScreen(
                                supplyId = supplyId,
                                prefilledBarcode = null,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Add Supply with prefilled Barcode
                        composable(
                            route = "add_supply_barcode/{barcode}",
                            arguments = listOf(navArgument("barcode") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val barcode = backStackEntry.arguments?.getString("barcode")
                            AddEditSupplyScreen(
                                supplyId = null,
                                prefilledBarcode = barcode,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Draft Inbox
                        composable("draft_inbox") {
                            DraftInboxScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOrganizeDraft = { id -> navController.navigate("edit_supply/$id") }
                            )
                        }

                        // Storage Locations
                        composable("locations") {
                            StorageLocationsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSelectLocationForFilter = { locId ->
                                    viewModel.setLocationFilter(locId)
                                    viewModel.selectTab(0)
                                }
                            )
                        }

                        // Project Detail
                        composable(
                            route = "project_detail/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
                            ProjectDetailScreen(
                                projectId = projectId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToEdit = { id -> navController.navigate("edit_project/$id") }
                            )
                        }

                        // Add Project
                        composable("add_project") {
                            AddEditProjectScreen(
                                projectId = null,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Edit Project
                        composable(
                            route = "edit_project/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId")
                            AddEditProjectScreen(
                                projectId = projectId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Barcode Scanner
                        composable("scanner") {
                            BarcodeScannerScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToSupplyDetail = { id ->
                                    navController.navigate("supply_detail/$id") {
                                        popUpTo("main")
                                    }
                                },
                                onNavigateToNewSupplyWithBarcode = { barcode ->
                                    navController.navigate("add_supply_barcode/$barcode") {
                                        popUpTo("main")
                                    }
                                }
                            )
                        }

                        // Settings
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
