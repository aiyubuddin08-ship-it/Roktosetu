package com.example.ui.navigation

sealed class Screen(val route: String) {
    // Authentication Destinations
    object AuthLogin : Screen("auth_login")
    object AuthRegister : Screen("auth_register")
    object AuthForgotPassword : Screen("auth_forgot_password")
    object AuthEmailVerification : Screen("auth_email_verification")
    object AuthProfileCompletion : Screen("auth_profile_completion")
    object AuthAccountDisabled : Screen("auth_account_disabled")
    
    // Main Bottom Nav Destinations
    object Home : Screen("home")
    object EmergencySearch : Screen("emergency_search")
    object CreateRequest : Screen("create_request")
    object DonorDirectory : Screen("donor_directory")
    object MyProfile : Screen("my_profile")

    // Feature Detail Screens
    object RequestDetails : Screen("request_details/{requestId}") {
        fun createRoute(requestId: String) = "request_details/$requestId"
    }
    object DonationHistory : Screen("donation_history")
    object Organizations : Screen("organizations")
    object ChatList : Screen("chat_list")
    object ChatConversation : Screen("chat_conversation/{conversationId}/{otherName}/{otherBloodGroup}") {
        fun createRoute(conversationId: String, otherName: String, otherBloodGroup: String) =
            "chat_conversation/$conversationId/${otherName.replace('/', '-')}/${otherBloodGroup.replace('/', '-')}"
    }
    object AdminDashboard : Screen("admin_dashboard")
    object SecurityCenter : Screen("security_center")
    object BloodBanks : Screen("blood_banks")
    object BloodCamps : Screen("blood_camps")
    object DonorCard : Screen("donor_card")
    object OfflineSms : Screen("offline_sms")
}
