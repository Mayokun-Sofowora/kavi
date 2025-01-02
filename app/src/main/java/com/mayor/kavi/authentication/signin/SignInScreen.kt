package com.mayor.kavi.authentication.signin

import android.widget.Toast
import androidx.activity.compose.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import com.mayor.kavi.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import com.mayor.kavi.util.*
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.tasks.await

@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val signInState by viewModel.signInState.collectAsState(initial = null)

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val signInScope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(enabled = true) {
        // Do nothing to prevent navigation to sign-in page
    }

    // Configure Google Sign-In
    val oneTapClient = remember { Identity.getSignInClient(context) }
    val signInRequest = remember {
        BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }

    // Google Sign-In result handler
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            credential.googleIdToken?.let { token ->
                val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
                viewModel.googleSignIn(firebaseCredential)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Google sign in failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Main UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "WELCOME", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Sign In To Continue", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password Field with visibility toggle
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                if (password.isNotEmpty()) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.Visibility
                            } else {
                                Icons.Filled.VisibilityOff
                            },
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sign In Button
        Button(
            onClick = {
                signInScope.launch {
                    viewModel.signIn(email, password)
                }
            }, colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.primary),
                contentColor = colorResource(id = R.color.on_primary),
                disabledContainerColor = colorResource(id = R.color.outline),
                disabledContentColor = colorResource(id = R.color.on_surface_variant)
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotEmpty() && password.isNotEmpty()
        ) {
            Text("Sign In", color = colorResource(id = R.color.on_primary))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign In Button
        Button(
            onClick = {
                signInScope.launch {
                    try {
                        val result = oneTapClient.beginSignIn(signInRequest).await()
                        launcher.launch(
                            IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        )
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "No Google accounts found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.primary),
                contentColor = colorResource(id = R.color.on_primary),
                disabledContainerColor = colorResource(id = R.color.outline),
                disabledContentColor = colorResource(id = R.color.on_surface_variant)
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = signInState?.isLoading == false
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In with Google")
            }
        }

        // Loading Indicator
        if (signInState?.isLoading == true) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        }

        // Sign Up Link
        TextButton(
            onClick = { navController.navigateToSignUp() },  // Using extension function
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorResource(id = R.color.primary)
            )
        ) {
            Text(text = "Don't have an account? Sign Up")
        }
    }

    // Handle sign-in result
    LaunchedEffect(signInState?.isAuthSuccess) {
        signInState?.isAuthSuccess?.let { authResult ->
            if (authResult.user != null) {
                // Check if user has a profile in Firestore
                launch {
                    val userProfile = viewModel.getUserProfile(authResult.user?.uid ?: "")
                    if (userProfile != null) {
                        navController.navigateToMainMenu()
                    } else {
                        // User doesn't have a profile, navigate to sign up
                        navController.navigateToSignUp()
                    }
                }
            }
        }
    }

    // Show toast when message is available
    LaunchedEffect(signInState?.toastMessage) {
        signInState?.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Prevent back navigation when signed in
    BackHandler(enabled = signInState?.isAuthSuccess != null) {
        // Do nothing to prevent navigation back
    }
}
