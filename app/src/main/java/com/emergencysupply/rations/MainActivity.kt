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
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        app.googleAuthManager.handleSignInResult(account)
                        Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
                    } catch (e: ApiException) {
                        Log.e("MainActivity", "Google sign-in failed: ${e.statusCode}", e)
                        Toast.makeText(this, "Sign-in error: ${e.statusCode}", Toast.LENGTH_LONG).show()
                    }
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
