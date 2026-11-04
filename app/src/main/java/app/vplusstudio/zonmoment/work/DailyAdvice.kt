package app.vplusstudio.zonmoment.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import app.vplusstudio.zonmoment.MainActivity
import app.vplusstudio.zonmoment.R
import app.vplusstudio.zonmoment.data.AMSTERDAM
import app.vplusstudio.zonmoment.data.ForecastRepository
import app.vplusstudio.zonmoment.data.NetworkException
import app.vplusstudio.zonmoment.data.SettingsStore
import app.vplusstudio.zonmoment.domain.Planner
import app.vplusstudio.zonmoment.ui.hhmm
import app.vplusstudio.zonmoment.ui.label
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Sends the morning notification with today's best moments. Runs on-device; no server needed. */
object DailyAdvice {
    private const val WORK_NAME = "daily-advice"
    private const val CHANNEL_ID = "daily-advice"
    private const val NOTIFICATION_ID = 1
    val SEND_AT: LocalTime = LocalTime.of(7, 30)

    fun schedule(context: Context) {
        val now = ZonedDateTime.now(AMSTERDAM)
        var next = now.with(SEND_AT)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<Worker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    class Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val ctx = applicationContext
            val settings = SettingsStore(ctx).load() ?: return Result.success()
            val forecast = try {
                ForecastRepository.forecast(settings)
            } catch (e: NetworkException) {
                return Result.retry()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                return Result.success() // bad data today; try again tomorrow
            }
            val now = ZonedDateTime.now(AMSTERDAM)
            val today = LocalDate.now(AMSTERDAM)
            val lines = settings.appliances.sortedBy { it.ordinal }.mapNotNull { a ->
                Planner.bestWindow(forecast.hours, a, today, now, settings.dynamicContract)?.let { r ->
                    ctx.getString(
                        R.string.notification_line,
                        ctx.getString(a.label),
                        r.start.hhmm(),
                        r.end.hhmm(),
                        (r.solarShare * 100).roundToInt(),
                    )
                }
            }
            notify(ctx, lines.ifEmpty { listOf(ctx.getString(R.string.notification_none)) })
            return Result.success()
        }
    }

    private fun notify(context: Context, lines: List<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
