package com.turnout.android.core.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.turnout.android.core.components.AdaptiveNavItem
import com.turnout.android.core.components.AdaptiveScaffold
import com.turnout.android.core.components.TwoPaneLayout
import com.turnout.android.core.theme.SignalGreen
import com.turnout.android.core.utils.AdaptiveLayoutConfig
import com.turnout.android.core.utils.LiveActivityState
import com.turnout.android.core.utils.TurnoutWindowSize
import com.turnout.android.presentation.ai.AiScreen
import com.turnout.android.presentation.auth.login.LoginScreen
import com.turnout.android.presentation.auth.forgot.ForgotPasswordScreen
import com.turnout.android.presentation.auth.otp.OtpVerificationScreen
import com.turnout.android.presentation.auth.register.RegisterScreen
import com.turnout.android.presentation.auth.reset.ResetPasswordScreen
import com.turnout.android.presentation.dashboard.DashboardScreen
import com.turnout.android.presentation.events.create.CreateEventScreen
import com.turnout.android.presentation.events.detail.EventDetailScreen
import com.turnout.android.presentation.events.list.EventsListScreen
import com.turnout.android.presentation.guests.GuestListScreen
import com.turnout.android.presentation.guests.ImportCsvScreen
import com.turnout.android.presentation.onboarding.OnboardingScreen
import com.turnout.android.presentation.payments.PaymentsScreen
import com.turnout.android.presentation.payments.UpgradeScreen
import com.turnout.android.presentation.rsvp.RsvpScreen
import com.turnout.android.presentation.settings.SettingsScreen

// Routes where bottom nav / nav rail should be hidden entirely — auth, onboarding,
// RSVP deep link, and CreateEvent (a focused single-task flow, not a "main" destination).
private val screensWithoutNav = setOf(
    Screen.Login.route,
    Screen.Register.route,
    Screen.OtpVerification.route,
    Screen.ForgotPassword.route,
    Screen.ResetPassword.route,
    Screen.Rsvp.route,
    Screen.Onboarding.route,
    Screen.CreateEvent.route
)

private data class BottomNavEntry(val screen: Screen, val icon: ImageVector, val label: String)

private val bottomNavEntries = listOf(
    BottomNavEntry(Screen.Dashboard, Icons.Default.Home, "Home"),
    BottomNavEntry(Screen.Events, Icons.Default.CalendarMonth, "Events"),
    BottomNavEntry(Screen.Ai, Icons.Default.AutoAwesome, "AI"),
    BottomNavEntry(Screen.Settings, Icons.Default.Settings, "Settings")
)

@Composable
fun NavGraph(
    startDestination: String,
    adaptiveConfig: AdaptiveLayoutConfig
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val showNav = screensWithoutNav.none { currentRoute?.startsWith(it.substringBefore("{")) == true }

    val newActivityCount by LiveActivityState.wsNewActivityCount.collectAsStateWithLifecycle()

    val navItems = bottomNavEntries.map { entry ->
        AdaptiveNavItem(
            label = entry.label,
            icon = entry.icon,
            selected = currentRoute == entry.screen.route,
            showBadge = entry.screen == Screen.Dashboard && newActivityCount > 0,
            onClick = {
                if (entry.screen == Screen.Dashboard) LiveActivityState.clear()
                navController.navigate(entry.screen.route) {
                    popUpTo(Screen.Dashboard.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }

    if (showNav) {
        AdaptiveScaffold(
            windowSize = adaptiveConfig.windowSize,
            navItems = navItems,
            topBar = {}
        ) { modifier ->
            TurnoutNavHost(
                navController = navController,
                startDestination = startDestination,
                adaptiveConfig = adaptiveConfig,
                modifier = modifier
            )
        }
    } else {
        // No AdaptiveScaffold at all on auth/onboarding/RSVP/CreateEvent — these screens
        // own their own full-screen layout rather than sitting inside the nav shell.
        TurnoutNavHost(
            navController = navController,
            startDestination = startDestination,
            adaptiveConfig = adaptiveConfig,
            modifier = Modifier
        )
    }
}

@Composable
private fun TurnoutNavHost(
    navController: NavHostController,
    startDestination: String,
    adaptiveConfig: AdaptiveLayoutConfig,
    modifier: Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300))
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToOtp = { email -> navController.navigate(Screen.OtpVerification.createRoute(email)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStack ->
            val email = backStack.arguments?.getString("email") ?: ""
            OtpVerificationScreen(
                email = email,
                // Routes through Onboarding, not straight to Dashboard — Onboarding
                // itself decides whether to actually show the walkthrough or skip
                // straight through, based on whether it's already been completed.
                onNavigateToDashboard = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStack ->
            val token = backStack.arguments?.getString("token") ?: ""
            ResetPasswordScreen(
                token = token,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToCreateEvent = { navController.navigate(Screen.CreateEvent.route) },
                onNavigateToEventsList = { navController.navigate(Screen.Events.route) },
                onNavigateToAi = { navController.navigate(Screen.Ai.route) },
                onNavigateToEventDetail = { id -> navController.navigate(Screen.EventDetail.createRoute(id)) }
            )
        }

        composable(Screen.Events.route) {
            // EventsListScreen now owns its own Expanded two-pane branching internally
            // (via EventsListViewModel's selectedEventId), so NavGraph just delegates
            // once here instead of duplicating that branching logic itself.
            EventsListScreen(
                onNavigateToCreate = { navController.navigate(Screen.CreateEvent.route) },
                onNavigateToDetail = { id -> navController.navigate(Screen.EventDetail.createRoute(id)) },
                onNavigateToImport = { id -> navController.navigate(Screen.ImportCsv.createRoute(id)) },
                adaptiveConfig = adaptiveConfig
            )
        }

        composable(Screen.CreateEvent.route) {
            CreateEventScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.EventDetail.deepLinkUriPattern })
        ) { backStack ->
            val eventId = backStack.arguments?.getLong("eventId") ?: 0L
            EventDetailScreen(
                eventId = eventId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToImport = { id -> navController.navigate(Screen.ImportCsv.createRoute(id)) }
            )
        }

        composable(
            route = Screen.ImportCsv.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStack ->
            val eventId = backStack.arguments?.getLong("eventId") ?: 0L
            ImportCsvScreen(eventId = eventId, onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.GuestList.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStack ->
            val eventId = backStack.arguments?.getLong("eventId") ?: 0L
            GuestListScreen(eventId = eventId)
        }

        composable(Screen.Ai.route) { AiScreen() }
        composable(
            route = Screen.Payments.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.Payments.deepLinkUri })
        ) {
            PaymentsScreen(onNavigateToUpgrade = { navController.navigate(Screen.Upgrade.route) })
        }
        composable(Screen.Upgrade.route) { UpgradeScreen() }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(
            route = Screen.Rsvp.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "${Screen.Rsvp.deepLinkUri}?token={token}" })
        ) { backStack ->
            val token = backStack.arguments?.getString("token") ?: ""
            RsvpScreen(token = token)
        }
    }
}
