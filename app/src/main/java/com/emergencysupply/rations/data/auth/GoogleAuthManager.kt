package com.emergencysupply.rations.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoogleAuthManager(private val context: Context) {

    private val _currentAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val currentAccount: StateFlow<GoogleSignInAccount?> = _currentAccount.asStateFlow()

    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
        .build()

    private val client: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    init {
        checkCurrentAccount()
    }

    fun checkCurrentAccount(): GoogleSignInAccount? {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null && GoogleSignIn.hasPermissions(lastAccount, Scope(SheetsScopes.SPREADSHEETS))) {
            _currentAccount.value = lastAccount
            return lastAccount
        }
        _currentAccount.value = null
        return null
    }

    fun getSignInIntent(): Intent {
        return client.signInIntent
    }

    fun handleSignInResult(account: GoogleSignInAccount?) {
        _currentAccount.value = account
    }

    fun signOut(onComplete: () -> Unit) {
        client.signOut().addOnCompleteListener {
            _currentAccount.value = null
            onComplete()
        }
    }
}
