package com.emergencysupply.rations

import android.app.Application
import com.emergencysupply.rations.data.auth.GoogleAuthManager
import com.emergencysupply.rations.data.local.RationsDatabase
import com.emergencysupply.rations.data.remote.GoogleSheetsClient
import com.emergencysupply.rations.data.repository.RationsRepository
import com.emergencysupply.rations.updater.GitHubSelfUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RationsApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { RationsDatabase.getInstance(this) }
    val googleAuthManager by lazy { GoogleAuthManager(this) }
    val sheetsClient by lazy { GoogleSheetsClient(this) }
    val repository by lazy { RationsRepository(database, sheetsClient, applicationScope) }
    val updater by lazy { GitHubSelfUpdater(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch(Dispatchers.IO) {
            repository.prewarmCache()
        }
    }
}
