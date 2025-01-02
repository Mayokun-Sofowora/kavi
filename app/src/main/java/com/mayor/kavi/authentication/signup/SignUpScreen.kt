package com.mayor.kavi.authentication.signup

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mayor.kavi.R
import kotlinx.coroutines.launch
import com.mayor.kavi.util.*

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val signUpState by viewModel.signUpState.collectAsState(initial=null)
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val signUpScope = rememberCoroutineScope()

    // Show toast when message is available
    LaunchedEffect(signUpState?.toastMessage) {
        signUpState?.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
        Text(text = "Create an Account", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Username Field
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email Field
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password Field
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Button
        Button(
            onClick = {
                signUpScope.launch {
                    viewModel.signUp(username, email, password)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.primary),
                contentColor = colorResource(id = R.color.on_primary),
                disabledContainerColor = colorResource(id = R.color.outline),
                disabledContentColor = colorResource(id = R.color.on_surface_variant)
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
        ) {
            Text("Sign Up", color = colorResource(id = R.color.on_primary))
        }

        // Loading Indicator
        if (signUpState?.isLoading == true) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Already have an account? Sign In Link
        TextButton(
            onClick = { navController.navigateToSignIn() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorResource(id = R.color.primary)
            )
        ) {
            Text(text = "Already have an account? Sign In")
        }
    }

    if (signUpState?.toastMessage?.contains("Account created successfully") == true) {
        navController.navigateToSignIn()
    }
}