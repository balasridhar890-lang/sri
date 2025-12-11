package com.voiceassistant.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.voiceassistant.android.ui.screens.*

/**
 * Navigation routes
 */
object AppRoutes {
    const val HOME_DASHBOARD = "home_dashboard"
    const val CONTACTS = "contacts"
    const val CONVERSATION_HISTORY = "conversation_history"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
}

/**
 * Main navigation host for the app
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME_DASHBOARD,
        modifier = modifier
    ) {
        composable(AppRoutes.HOME_DASHBOARD) {
            HomeDashboardScreen(
                onNavigateToContacts = { navController.navigate(AppRoutes.CONTACTS) },
                onNavigateToConversationHistory = { navController.navigate(AppRoutes.CONVERSATION_HISTORY) },
                onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) }
            )
        }
        
        composable(AppRoutes.CONTACTS) {
            ContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(AppRoutes.CONVERSATION_HISTORY) {
            ConversationHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiagnostics = { navController.navigate(AppRoutes.DIAGNOSTICS) }
            )
        }
        
        composable(AppRoutes.DIAGNOSTICS) {
            DiagnosticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}