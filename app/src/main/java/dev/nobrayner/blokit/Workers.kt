package dev.nobrayner.blokit

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import android.content.BroadcastReceiver
import androidx.work.WorkManager
import java.util.UUID
import android.app.PendingIntent
import kotlin.time.Duration.Companion.minutes

class CountdownWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_DURATION = "duration"
        const val KEY_PROGRESS = "progress"
        const val NOTIFICATION_CHANNEL_ID = "blocks_channel"
        const val NOTIFICATION_ID = 1001
        const val WORK_ID = "WORK_ID"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        createNotificationChannel()
        setForeground(createForegroundInfo())

        val duration = inputData.getLong(KEY_DURATION, 25.minutes.inWholeSeconds) // seconds

        for (i in duration downTo 1) {
            setProgress(workDataOf(KEY_PROGRESS to i))
            delay(1000)
        }

        val database = AppDatabase.getInstance(applicationContext)
        val end = Instant.now()
        val start = end.minus(Duration.of(duration, ChronoUnit.SECONDS))
        database.blockDao().insert(Block(
            startedAt = start,
            finishedAt = end,
        ))

        showCompletedNotification()

        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val cancelIntent = Intent(applicationContext, StopCountdownReceiver::class.java).apply {
            putExtra("WORK_ID", id.toString())
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Blokit")
            .setContentText("Making a block")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSilent(true)
            .setOngoing(true)
            .addAction(0, "Stop Block", cancelPendingIntent)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showCompletedNotification() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE // Use FLAG_UPDATE_CURRENT if you want to update extras
        )

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Block made!")
            .setContentText("Block complete")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()


        NotificationManagerCompat
            .from(applicationContext)
            .notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Blocks",
            NotificationManager.IMPORTANCE_HIGH
        )

        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(channel)
    }
}

class StopCountdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val workId = intent.getStringExtra(CountdownWorker.WORK_ID)
        if (workId != null) {
            WorkManager.getInstance(context).cancelWorkById(UUID.fromString(workId))
        }
    }
}