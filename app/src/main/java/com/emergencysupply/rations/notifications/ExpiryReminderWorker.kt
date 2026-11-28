package com.emergencysupply.rations.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.emergencysupply.rations.MainActivity
import com.emergencysupply.rations.data.local.RationsDatabase
import java.util.concurrent.TimeUnit

class ExpiryReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "ExpiryReminderWorker"
        const val CHANNEL_ID = "expiry_alerts"
        const val NOTIFICATION_ID = 2001
        const val UNIQUE_WORK_NAME = "rations_expiry_daily_check"
        private const val PREFS_NAME = "rations_prefs"
        private const val PREF_KEY_ENABLED = "expiry_alerts_enabled"

        fun scheduleDailyCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY_ENABLED, true)
                .apply()

            Log.i(TAG, "Scheduled daily expiry reminder check via WorkManager")
        }

        fun cancelDailyCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY_ENABLED, false)
                .apply()

            Log.i(TAG, "Cancelled daily expiry reminder check")
        }

        fun isScheduled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_KEY_ENABLED, false)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val database = RationsDatabase.getInstance(applicationContext)
            val items = database.inventoryDao().getAllItemsOnce()
            val settings = database.settingsDao().getSettingsOnce()

            val useSoonDays = settings?.useSoonDays?.takeIf { it > 0 } ?: 30

            val expiredItems = items.filter { item ->
                item.isExpired || (item.daysUntilExpiry != null && item.daysUntilExpiry < 0)
            }

            val expiringSoonItems = items.filter { item ->
                !expiredItems.contains(item) && (
                    item.isExpiringSoon ||
                    (item.daysUntilExpiry != null && item.daysUntilExpiry in 0..useSoonDays)
                )
            }

            val totalAlerts = expiredItems.size + expiringSoonItems.size
            Log.d(TAG, "Expiry check completed: $totalAlerts items need attention (${expiredItems.size} expired, ${expiringSoonItems.size} expiring soon)")

            if (totalAlerts > 0) {
                postExpiryNotification(expiredItems.size, expiringSoonItems.size, useSoonDays, expiredItems + expiringSoonItems)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing expiry reminder worker", e)
            Result.failure()
        }
    }

    private fun postExpiryNotification(
        expiredCount: Int,
        expiringSoonCount: Int,
        useSoonDays: Int,
        flaggedItems: List<com.emergencysupply.rations.data.local.entity.InventoryItem>
    ) {
        val context = applicationContext

        // Notification channel creation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Expiry Alerts"
            val descriptionText = "Daily reminders for expired rations and supplies approaching expiry"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Safe permission check for Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot post notification.")
                return
            }
        }

        val title = when {
            expiredCount > 0 && expiringSoonCount > 0 ->
                "Rations Alert: $expiredCount Expired & $expiringSoonCount Expiring"
            expiredCount > 0 ->
                "Rations Alert: $expiredCount Expired Item${if (expiredCount > 1) "s" else ""}"
            else ->
                "Rations Alert: $expiringSoonCount Item${if (expiringSoonCount > 1) "s" else ""} Expiring Soon"
        }

        val summaryText = buildString {
            if (expiredCount > 0) append("$expiredCount expired")
            if (expiringSoonCount > 0) {
                if (isNotEmpty()) append(" • ")
                append("$expiringSoonCount expiring within $useSoonDays days")
            }
        }

        val topFlagged = flaggedItems.take(5)
        val bigText = buildString {
            appendLine(summaryText)
            appendLine()
            for (item in topFlagged) {
                val exp = item.earliestExpiry ?: "Unknown"
                val statusTag = if (item.isExpired) "[EXPIRED]" else "[SOON]"
                appendLine("• $statusTag ${item.name} (${item.location.ifBlank { "Pantry" }}) - $exp")
            }
            if (flaggedItems.size > 5) {
                appendLine("… and ${flaggedItems.size - 5} more items in emergency pantry")
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.i(TAG, "Posted expiry reminder notification successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while notifying expiry reminder", e)
        }
    }
}
