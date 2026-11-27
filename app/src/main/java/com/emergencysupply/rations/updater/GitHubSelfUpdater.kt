package com.emergencysupply.rations.updater

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.emergencysupply.rations.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long
)

data class GitHubRelease(
    val tag_name: String,
    val name: String?,
    val body: String?,
    val html_url: String?,
    val assets: List<GitHubAsset>?
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val release: GitHubRelease, val apkAsset: GitHubAsset) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(val progress: Float) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class GitHubSelfUpdater(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val repoOwner = BuildConfig.GITHUB_REPO_OWNER
    private val repoName = BuildConfig.GITHUB_REPO_NAME
    private val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

    private val prefs = context.getSharedPreferences("app_updater_prefs", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    suspend fun checkForUpdates(force: Boolean = false) = withContext(Dispatchers.IO) {
        val lastCheck = prefs.getLong("last_check_time", 0L)
        val now = System.currentTimeMillis()
        // Throttle automatic checks to every 4 hours unless forced
        if (!force && (now - lastCheck) < 4 * 60 * 60 * 1000) {
            return@withContext
        }

        _status.value = UpdateStatus.Checking

        try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "EmergencyFoodRations-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _status.value = UpdateStatus.Error("GitHub API check returned ${response.code} (Repository may be private).")
                return@withContext
            }

            prefs.edit().putLong("last_check_time", now).apply()

            val body = response.body?.string() ?: return@withContext
            val release = gson.fromJson(body, GitHubRelease::class.java)

            val latestTag = release.tag_name.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

            if (isVersionNewer(latestTag, currentVersion)) {
                val apkAsset = release.assets?.firstOrNull { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    _status.value = UpdateStatus.Available(release, apkAsset)
                } else {
                    _status.value = UpdateStatus.UpToDate
                }
            } else {
                _status.value = UpdateStatus.UpToDate
            }
        } catch (e: Exception) {
            _status.value = UpdateStatus.Error(e.message ?: "Failed to check for updates.")
        }
    }

    suspend fun downloadAndInstallApk(activity: Activity, asset: GitHubAsset) = withContext(Dispatchers.IO) {
        _status.value = UpdateStatus.Downloading(0f)
        try {
            val updateDir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "updates").apply { mkdirs() }
            val destinationFile = File(updateDir, "EmergencySupply-update.apk")
            if (destinationFile.exists()) destinationFile.delete()

            val request = Request.Builder()
                .url(asset.browser_download_url)
                .header("User-Agent", "EmergencyFoodRations-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body ?: throw IllegalStateException("Empty download body")
            val totalBytes = responseBody.contentLength()

            responseBody.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        if (totalBytes > 0) {
                            val progress = (totalDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            _status.value = UpdateStatus.Downloading(progress)
                        }
                    }
                    output.flush()
                }
            }

            _status.value = UpdateStatus.ReadyToInstall(destinationFile)

            withContext(Dispatchers.Main) {
                launchInstaller(activity, destinationFile)
            }
        } catch (e: Exception) {
            _status.value = UpdateStatus.Error("Download failed: ${e.message}")
        }
    }

    fun launchInstaller(activity: Activity, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
                return
            }
        }

        activity.startActivity(installIntent)
    }

    fun dismissUpdate() {
        _status.value = UpdateStatus.Idle
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
