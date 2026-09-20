package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.model.AuthState
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.AccountDisabledScreen
import com.example.ui.screens.auth.EmailVerificationScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.ProfileCompletionScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.chat.ChatConversationScreen
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.donors.DonorDirectoryScreen
import com.example.ui.screens.emergency.EmergencyScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.organizations.OrganizationsScreen
import com.example.ui.screens.profile.DonationHistoryScreen
import com.example.ui.screens.profile.MyProfileScreen
import com.example.ui.screens.requests.CreateRequestScreen
import com.example.ui.screens.requests.RequestDetailScreen
import com.example.ui.screens.security.SecurityCenterScreen
import com.example.ui.screens.bloodbanks.BloodBankScreen
import com.example.ui.screens.camps.BloodCampScreen
import com.example.ui.screens.donorcard.DonorCardScreen
import com.example.ui.screens.offline.OfflineSmsScreen
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BloodViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ExtraFeaturesViewModel
import com.example.ui.viewmodel.OrganizationViewModel

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Emergency : BottomNavItem(Screen.EmergencySearch.route, "Emergency", Icons.Filled.Emergency, Icons.Outlined.Emergency)
    object CreateRequest : BottomNavItem(Screen.CreateRequest.route, "Request", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline)
    object Donors : BottomNavItem(Screen.DonorDirectory.route, "Donors", Icons.Filled.People, Icons.Outlined.People)
    object Profile : BottomNavItem(Screen.MyProfile.route, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Emergency,
    BottomNavItem.CreateRequest,
    BottomNavItem.Donors,
    BottomNavItem.Profile
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    bloodViewModel: BloodViewModel,
    chatViewModel: ChatViewModel,
    orgViewModel: OrganizationViewModel,
    adminViewModel: AdminViewModel,
    extraViewModel: ExtraFeaturesViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    if (authState is AuthState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "RoktoSetu",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    if (authState is AuthState.EmailVerificationRequired) {
        EmailVerificationScreen(
            authViewModel = authViewModel,
            onVerifiedSuccess = { /* Automatically handled by auth state */ },
            onSignOut = { authViewModel.signOut() }
        )
        return
    }

    if (authState is AuthState.ProfileCompletionRequired) {
        ProfileCompletionScreen(
            authViewModel = authViewModel,
            onCompletionSuccess = { /* Automatically handled by auth state */ }
        )
        return
    }

    if (authState is AuthState.AccountDisabled) {
        AccountDisabledScreen(
            authViewModel = authViewModel,
            onSignOut = { authViewModel.signOut() }
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_${item.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Main Guest-accessible Tabs
            composable(Screen.Home.route) {
                HomeScreen(
                    authViewModel = authViewModel,
                    bloodViewModel = bloodViewModel,
                    extraViewModel = extraViewModel,
                    onNavigateToCreateRequest = { navController.navigate(Screen.CreateRequest.route) },
                    onNavigateToEmergency = { navController.navigate(Screen.EmergencySearch.route) },
                    onNavigateToDonors = { navController.navigate(Screen.DonorDirectory.route) },
                    onNavigateToOrganizations = { navController.navigate(Screen.Organizations.route) },
                    onNavigateToDonationHistory = { navController.navigate(Screen.DonationHistory.route) },
                    onNavigateToRequestDetails = { reqId ->
                        navController.navigate(Screen.RequestDetails.createRoute(reqId))
                    },
                    onNavigateToAdmin = { navController.navigate(Screen.AdminDashboard.route) },
                    onNavigateToSecurity = { navController.navigate(Screen.SecurityCenter.route) },
                    onNavigateToChat = { navController.navigate(Screen.ChatList.route) },
                    onNavigateToBloodBanks = { navController.navigate(Screen.BloodBanks.route) },
                    onNavigateToCamps = { navController.navigate(Screen.BloodCamps.route) },
                    onNavigateToDonorCard = { navController.navigate(Screen.DonorCard.route) },
                    onNavigateToOfflineSms = { navController.navigate(Screen.OfflineSms.route) },
                    onNavigateToLogin = { navController.navigate(Screen.AuthLogin.route) },
                    onNavigateToRegister = { navController.navigate(Screen.AuthRegister.route) }
                )
            }

            composable(Screen.EmergencySearch.route) {
                EmergencyScreen(
                    authViewModel = authViewModel,
                    bloodViewModel = bloodViewModel,
                    chatViewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChatConversation = { convId, name, blood ->
                        navController.navigate(Screen.ChatConversation.createRoute(convId, name, blood))
                    },
                    onNavigateToCreateRequest = { navController.navigate(Screen.CreateRequest.route) },
                    onNavigateToLogin = { navController.navigate(Screen.AuthLogin.route) },
                    onNavigateToRegister = { navController.navigate(Screen.AuthRegister.route) }
                )
            }

            composable(Screen.CreateRequest.route) {
                CreateRequestScreen(
                    authViewModel = authViewModel,
                    bloodViewModel = bloodViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onRequestCreated = { reqId ->
                        navController.navigate(Screen.RequestDetails.createRoute(reqId)) {
                            popUpTo(Screen.CreateRequest.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.navigate(Screen.AuthLogin.route) },
                    onNavigateToRegister = { navController.navigate(Screen.AuthRegister.route) }
                )
            }

            composable(Screen.DonorDirectory.route) {
                DonorDirectoryScreen(
                    authViewModel = authViewModel,
                    bloodViewModel = bloodViewModel,
                    chatViewModel = chatViewModel,
                    onNavigateToChatConversation = { convId, name, blood ->
                        navController.navigate(Screen.ChatConversation.createRoute(convId, name, blood))
                    },
                    onNavigateToSecurity = { navController.navigate(Screen.SecurityCenter.route) },
                    onNavigateToChat = { navController.navigate(Screen.ChatList.route) },
                    onNavigateToLogin = { navController.navigate(Screen.AuthLogin.route) },
                    onNavigateToRegister = { navController.navigate(Screen.AuthRegister.route) }
                )
            }

            composable(Screen.MyProfile.route) {
                MyProfileScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { navController.navigate(Screen.AuthLogin.route) },
                    onNavigateToRegister = { navController.navigate(Screen.AuthRegister.route) },
                    onNavigateToHistory = { navController.navigate(Screen.DonationHistory.route) },
                    onNavigateToSecurity = { navController.navigate(Screen.SecurityCenter.route) },
                    onSignedOut = {
                        authViewModel.signOut()
                    }
                )
            }

            // Authentication Screens (accessible directly on action or from profile/topbar)
            composable(Screen.AuthLogin.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate(Screen.AuthRegister.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.AuthForgotPassword.route) },
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home.route)
                        }
                    },
                    onLoginSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.AuthRegister.route) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.AuthLogin.route)
                        }
                    },
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home.route)
                        }
                    },
                    onRegisterSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.AuthForgotPassword.route) {
                ForgotPasswordScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AuthEmailVerification.route) {
                EmailVerificationScreen(
                    authViewModel = authViewModel,
                    onVerifiedSuccess = { navController.popBackStack() },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.AuthProfileCompletion.route) {
                ProfileCompletionScreen(
                    authViewModel = authViewModel,
                    onCompletionSuccess = { navController.popBackStack() }
                )
            }

            composable(Screen.AuthAccountDisabled.route) {
                AccountDisabledScreen(
                    authViewModel = authViewModel,
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Sub-destinations
            composable(
                route = Screen.RequestDetails.route,
                arguments = listOf(navArgument("requestId") { type = NavType.StringType })
            ) { backStackEntry ->
                val reqId = backStackEntry.arguments?.getString("requestId") ?: ""
                RequestDetailScreen(
                    requestId = reqId,
                    authViewModel = authViewModel,
                    bloodViewModel = bloodViewModel,
                    chatViewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChatConversation = { convId, name, blood ->
                        navController.navigate(Screen.ChatConversation.createRoute(convId, name, blood))
                    },
                    onNavigateToLogin = { navController.navigate(Screen.AuthLogin.route) },
                    onNavigateToRegister = { navController.navigate(Screen.AuthRegister.route) }
                )
            }

            composable(Screen.DonationHistory.route) {
                DonationHistoryScreen(
                    bloodViewModel = bloodViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Organizations.route) {
                OrganizationsScreen(
                    orgViewModel = orgViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ChatList.route) {
                ChatListScreen(
                    chatViewModel = chatViewModel,
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToConversation = { convId, name, blood ->
                        navController.navigate(Screen.ChatConversation.createRoute(convId, name, blood))
                    }
                )
            }

            composable(
                route = Screen.ChatConversation.route,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.StringType },
                    navArgument("otherName") { type = NavType.StringType },
                    navArgument("otherBloodGroup") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val convId = backStackEntry.arguments?.getString("conversationId") ?: ""
                val otherName = backStackEntry.arguments?.getString("otherName") ?: "User"
                val otherBlood = backStackEntry.arguments?.getString("otherBloodGroup") ?: ""

                ChatConversationScreen(
                    conversationId = convId,
                    otherName = otherName,
                    otherBloodGroup = otherBlood,
                    chatViewModel = chatViewModel,
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    adminViewModel = adminViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SecurityCenter.route) {
                SecurityCenterScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BloodBanks.route) {
                BloodBankScreen(
                    extraViewModel = extraViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BloodCamps.route) {
                BloodCampScreen(
                    extraViewModel = extraViewModel,
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.AuthLogin.route) },
                    onNavigateToRegister = { navController.navigate(Screen.AuthRegister.route) }
                )
            }

            composable(Screen.DonorCard.route) {
                DonorCardScreen(
                    authViewModel = authViewModel,
                    extraViewModel = extraViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.OfflineSms.route) {
                OfflineSmsScreen(
                    extraViewModel = extraViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
