package com.mayor.kavi.authentication.signin

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import com.mayor.kavi.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.google.android.gms.common.api.ApiException
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.viewmodel.AppViewModel

@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: SignInViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel()
) {
    val signInState by viewModel.signInState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val signInScope = rememberCoroutineScope()
    val context = LocalContext.current
//    val auth = FirebaseAuth.getInstance()

//    // Check for existing session
//    LaunchedEffect(Unit) {
//        if (auth.currentUser != null) {
//            navController.navigate(Routes.MainMenu.route) {
//                popUpTo(Routes.SignIn.route) { inclusive = true }
//            }
//        }
//    }

    BackHandler(enabled = true) {
        // Do nothing to prevent navigation to sign-in page
    }

    // Show toast when message is available
    LaunchedEffect(signInState.toastMessage) {
        signInState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Configure Google Sign-In
    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }

    // Google Sign-In result handler
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    val credential = GoogleAuthProvider.getCredential(token, null)
                    viewModel.googleSignIn(credential, appViewModel)
                }
            } catch (e: ApiException) {
                Toast.makeText(
                    context,
                    "Google sign in failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                appViewModel.onLoginCancel()
            }
        } else {
            appViewModel.onLoginCancel()
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
                    appViewModel.updateLogin()
                    viewModel.signIn(email, password, navController, appViewModel)
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
                appViewModel.updateLogin()
                launcher.launch(googleSignInClient.signInIntent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.primary),
                contentColor = colorResource(id = R.color.on_primary),
                disabledContainerColor = colorResource(id = R.color.outline),
                disabledContentColor = colorResource(id = R.color.on_surface_variant)
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !signInState.isLoading
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
        if (signInState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        }


        // Sign Up Link
        TextButton(
            onClick = { navController.navigate(Routes.SignUp.route) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorResource(id = R.color.primary)
            )
        ) {
            Text(text = "Don't have an account? Sign Up")
        }
    }

    // Handle Google Sign-In result
    signInState.isAuthSuccess?.let {
        signInScope.launch {
            navController.navigate(Routes.MainMenu.route)
            viewModel.updateSignInStateWithError("Google sign-in successful")
            appViewModel.onLoginComplete()
        }
    }
}