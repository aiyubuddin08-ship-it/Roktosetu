package com.example.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.domain.LocationHelper
import com.example.model.BloodGroup
import com.example.ui.theme.BloodRed
import com.example.ui.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("CredMan requires an Activity context")
}

// ==========================================
// 1. SIGN IN SCREEN
// ==========================================

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onAdminLoginSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.default_web_client_id)

    val uiState by authViewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (onNavigateBack != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_auth_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "RoktoSetu",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "রক্তসেতু",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "RoktoSetu • Emergency Blood Network",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Sign in with Google or your registered email",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            if (webClientId.isBlank() || webClientId.startsWith("YOUR_", ignoreCase = true)) {
                                authViewModel.reportError("Google Sign-In Web Client ID is not configured.")
                                return@OutlinedButton
                            }
                            coroutineScope.launch {
                                try {
                                    val activity = context.findActivity()
                                    val credentialManager = CredentialManager.create(activity)
                                    val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(signInOption)
                                        .build()
                                    val result = try {
                                        credentialManager.getCredential(activity, request)
                                    } catch (e: NoCredentialException) {
                                        val fallbackOption = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId(webClientId)
                                            .build()
                                        val fallbackReq = GetCredentialRequest.Builder()
                                            .addCredentialOption(fallbackOption)
                                            .build()
                                        credentialManager.getCredential(activity, fallbackReq)
                                    }
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                    authViewModel.signInWithGoogle(googleIdTokenCredential.idToken, onLoginSuccess)
                                } catch (e: GetCredentialCancellationException) {
                                    authViewModel.reportError(context.getString(R.string.auth_google_cancelled))
                                } catch (e: NoCredentialException) {
                                    authViewModel.reportError(context.getString(R.string.auth_google_no_credential))
                                } catch (e: Exception) {
                                    authViewModel.reportError(e.localizedMessage ?: "Google Sign-In failed.")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_google_sign_in"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue with Google", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  OR EMAIL  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_email"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_password"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onNavigateToForgotPassword,
                            modifier = Modifier.testTag("btn_forgot_password")
                        ) {
                            Text("Forgot Password?")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            authViewModel.signIn(
                                email = email,
                                pass = password,
                                onSuccess = onLoginSuccess,
                                onAdminSuccess = onAdminLoginSuccess ?: onLoginSuccess
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_submit_login"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.testTag("btn_goto_register")
                ) {
                    Text("Register as Donor / User", fontWeight = FontWeight.Bold)
                }
            }

            if (onNavigateBack != null) {
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("btn_continue_as_guest")
                ) {
                    Text("Continue Browsing as Guest (অতিথি হিসেবে ব্রাউজ করুন)", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// 2. REGISTRATION SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by authViewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var division by remember { mutableStateOf("Dhaka") }
    var district by remember { mutableStateOf("Dhaka") }
    var upazila by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf(BloodGroup.O_POSITIVE) }
    var isDonor by remember { mutableStateOf(true) }
    var termsAccepted by remember { mutableStateOf(false) }

    var expandedBloodDropdown by remember { mutableStateOf(false) }
    var expandedDivisionDropdown by remember { mutableStateOf(false) }
    var expandedDistrictDropdown by remember { mutableStateOf(false) }

    val availableDistricts = remember(division) {
        LocationHelper.getDistrictsForDivision(division)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onNavigateBack != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_reg_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = "Join রক্তসেতু (RoktoSetu)",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Join Bangladesh's emergency blood donation network",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("input_reg_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("input_reg_email"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (e.g., 01712345678) *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("input_reg_phone"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (DD/MM/YYYY)") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("input_reg_dob"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (Min 8 chars, 1 Upper, 1 Digit) *") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("input_reg_password"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password *") },
                        leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("input_reg_confirm_password"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedBloodDropdown,
                        onExpandedChange = { expandedBloodDropdown = !expandedBloodDropdown },
                        modifier = Modifier.fillMaxWidth().testTag("blood_group_dropdown")
                    ) {
                        OutlinedTextField(
                            value = "${selectedBloodGroup.display} (${selectedBloodGroup.bengaliDisplay})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Blood Group (রক্তের গ্রুপ) *") },
                            leadingIcon = { Icon(Icons.Default.Bloodtype, contentDescription = null, tint = BloodRed) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBloodDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBloodDropdown,
                            onDismissRequest = { expandedBloodDropdown = false }
                        ) {
                            BloodGroup.entries.forEach { bg ->
                                DropdownMenuItem(
                                    text = { Text("${bg.display} - ${bg.bengaliDisplay}") },
                                    onClick = {
                                        selectedBloodGroup = bg
                                        expandedBloodDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedDivisionDropdown,
                            onExpandedChange = { expandedDivisionDropdown = !expandedDivisionDropdown },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = division,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Division (বিভাগ)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDivisionDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDivisionDropdown,
                                onDismissRequest = { expandedDivisionDropdown = false }
                            ) {
                                LocationHelper.DIVISIONS.forEach { div ->
                                    DropdownMenuItem(
                                        text = { Text(div) },
                                        onClick = {
                                            division = div
                                            district = LocationHelper.getDistrictsForDivision(div).firstOrNull() ?: div
                                            expandedDivisionDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedDistrictDropdown,
                            onExpandedChange = { expandedDistrictDropdown = !expandedDistrictDropdown },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = district,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("District (জেলা)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistrictDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor().testTag("input_reg_city"),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDistrictDropdown,
                                onDismissRequest = { expandedDistrictDropdown = false }
                            ) {
                                availableDistricts.forEach { dist ->
                                    DropdownMenuItem(
                                        text = { Text(dist) },
                                        onClick = {
                                            district = dist
                                            expandedDistrictDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = upazila,
                            onValueChange = { upazila = it },
                            label = { Text("Upazila / Thana") },
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                            modifier = Modifier.weight(1f).testTag("input_reg_upazila"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = area,
                            onValueChange = { area = it },
                            label = { Text("Area / Locality") },
                            modifier = Modifier.weight(1f).testTag("input_reg_area"),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDonor,
                            onCheckedChange = { isDonor = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Register as Voluntary Blood Donor",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Allow patients in your district to find your donor profile",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            modifier = Modifier.testTag("checkbox_terms")
                        )
                        Text(
                            text = "I agree to the Terms of Service & Privacy Policy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            authViewModel.register(
                                name = name,
                                email = email,
                                pass = password,
                                confirmPass = confirmPassword,
                                phone = phone,
                                bloodGroup = selectedBloodGroup,
                                dob = dob,
                                isDonor = isDonor,
                                termsAccepted = termsAccepted,
                                division = division,
                                district = district,
                                upazila = upazila,
                                city = district,
                                area = area,
                                onSuccess = onRegisterSuccess
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_submit_register"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already registered?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Sign In")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
