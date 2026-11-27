package com.emergencysupply.rations

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.emergencysupply.rations.ui.MainScreen
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.MainViewModelFactory
import com.emergencysupply.rations.ui.theme.EmergencySupplyTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {

    private val app by lazy { application as RationsApplication }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            repository = app.repository,
            authManager = app.googleAuthManager,
            updater = app.updater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val signInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.data != null) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        app.googleAuthManager.handleSignInResult(account)
                        viewModel.setSignInError(null)
                        Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
                    } catch (e: ApiException) {
                        Log.e("MainActivity", "Google sign-in failed: ${e.statusCode} (${e.statusMessage})", e)
                        val errorDetail = when (e.statusCode) {
                            10 -> "Developer Error (10): Release SHA-1 is not registered in Google Cloud Console OAuth Client ID."
                            12500 -> "Sign-in Failed (12500): Google rejected sign-in. Check SHA-1 & OAuth consent screen."
                            7 -> "Network Error (7): Could not contact Google auth servers. Check internet."
                            12501 -> "Sign-in Canceled (12501): Account selection was dismissed."
                            12502 -> "Sign-in In Progress (12502)."
                            else -> "Sign-in error: code ${e.statusCode} (${e.statusMessage ?: e.message})"
                        }
                        viewModel.setSignInError(errorDetail)
                        Toast.makeText(this, errorDetail, Toast.LENGTH_LONG).show()
                    }
                } else {
                    viewModel.setSignInError("Sign-in canceled: No account data returned.")
                }
            }

            EmergencySupplyTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSignInClick = {
                        signInLauncher.launch(app.googleAuthManager.getSignInIntent())
                    },
                    onSignOutClick = {
                        app.googleAuthManager.signOut {
                            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
