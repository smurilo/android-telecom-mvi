package com.smsmurilo.telecom.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smsmurilo.telecom.data.DataStoreSettingsRepository
import com.smsmurilo.telecom.domain.TelecomRepository
import com.smsmurilo.telecom.domain.UsageAlertPolicy
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/** Runs every 6h (only with network, battery-friendly) and warns before the data allowance runs out. */
class UsageAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {

    private val repository: TelecomRepository by inject()
    private val settings: DataStoreSettingsRepository by inject()

    override suspend fun doWork(): Result = runCatching {
        val usage = repository.usage()
        val decision = UsageAlertPolicy.evaluate(usage, settings.alertThreshold.first(), settings.lastAlertedPercent.first())
        if (decision.notify) {
            notify(decision.message!!)
            settings.setLastAlertedPercent(usage.usedPercent)
        }
        Result.success()
    }.getOrElse { if (runAttemptCount < 3) Result.retry() else Result.failure() }

    private fun notify(message: String) {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Alertas de consumo", NotificationManager.IMPORTANCE_DEFAULT))
        val granted = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        NotificationManagerCompat.from(applicationContext).notify(
            1,
            NotificationCompat.Builder(applicationContext, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Meu Plano")
                .setContentText(message)
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        private const val CHANNEL = "usage"
        private const val NAME = "usage-alert"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageAlertWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
