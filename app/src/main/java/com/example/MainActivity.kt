package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.RoktoSetuRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.RoktoSetuTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AdminViewModelFactory
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AuthViewModelFactory
import com.example.ui.viewmodel.BloodViewModel
import com.example.ui.viewmodel.BloodViewModelFactory
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory
import com.example.ui.viewmodel.ExtraFeaturesViewModel
import com.example.ui.viewmodel.OrgViewModelFactory
import com.example.ui.viewmodel.OrganizationViewModel

class MainActivity : ComponentActivity() {

    private val repository by lazy { RoktoSetuRepository(applicationContext) }

    private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory(repository) }
    private val bloodViewModel: BloodViewModel by viewModels { BloodViewModelFactory(repository) }
    private val chatViewModel: ChatViewModel by viewModels { ChatViewModelFactory(repository) }
    private val orgViewModel: OrganizationViewModel by viewModels { OrgViewModelFactory(repository) }
    private val adminViewModel: AdminViewModel by viewModels { AdminViewModelFactory(repository) }
    private val extraViewModel: ExtraFeaturesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoktoSetuTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    authViewModel = authViewModel,
                    bloodViewModel = bloodViewModel,
                    chatViewModel = chatViewModel,
                    orgViewModel = orgViewModel,
                    adminViewModel = adminViewModel,
                    extraViewModel = extraViewModel
                )
            }
        }
    }
}
