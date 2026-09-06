package com.example.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps a focus session alive while the app is
 * backgrounded and shows an ongoing notification with a live elapsed count
 * plus Pause/Resume and Stop actions. State lives in [FocusController]; this
 * service only renders it and reacts to the notification action buttons.
 */
class FocusTimerService : Service() {

  private val scope = CoroutineScope(Dispatchers.Main)
  private var tickJob: Job? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_PAUSE -> FocusController.pause()
      ACTION_RESUME -> FocusController.resume()
      ACTION_STOP -> {
        FocusController.pause() // freeze elapsed; VM.stopFocus does the real teardown
        stopSelf()
        return START_NOT_STICKY
      }
    }

    val session = FocusController.session.value
    if (session == null) {
      stopSelf()
      return START_NOT_STICKY
    }

    startForegroundCompat(buildNotification(session))
    startTicking()
    return START_STICKY
  }

  private fun startTicking() {
    if (tickJob?.isActive == true) return
    tickJob = scope.launch {
      while (isActive) {
        val s = FocusController.session.value ?: run { stopSelf(); return@launch }
        notificationManager().notify(NOTIF_ID, buildNotification(s))
        delay(1000)
      }
    }
  }

  private fun buildNotification(s: FocusSession): Notification {
    val open = PendingIntent.getActivity(
      this, 0,
      Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val toggleAction = if (s.isPaused) {
      NotificationCompat.Action(0, "Resume", action(ACTION_RESUME))
    } else {
      NotificationCompat.Action(0, "Pause", action(ACTION_PAUSE))
    }
    val elapsed = formatFocusElapsed(s.elapsedMs())
    val subtitle = s.projectName?.let { "$it · $elapsed" } ?: elapsed

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(if (s.isPaused) "Focus paused — ${s.taskName}" else s.taskName)
      .setContentText(subtitle)
      .setOngoing(!s.isPaused)
      .setOnlyAlertOnce(true)
      .setContentIntent(open)
      .addAction(toggleAction)
      .addAction(NotificationCompat.Action(0, "Stop", action(ACTION_STOP)))
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .build()
  }

  private fun action(name: String): PendingIntent = PendingIntent.getService(
    this, name.hashCode(),
    Intent(this, FocusTimerService::class.java).setAction(name),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
  )

  private fun startForegroundCompat(n: Notification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIF_ID, n)
    }
  }

  private fun notificationManager() =
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  override fun onDestroy() {
    tickJob?.cancel()
    scope.coroutineContext[Job]?.cancel()
    super.onDestroy()
  }

  companion object {
    private const val CHANNEL_ID = "focus_timer"
    private const val NOTIF_ID = 42
    const val ACTION_PAUSE = "com.example.focus.PAUSE"
    const val ACTION_RESUME = "com.example.focus.RESUME"
    const val ACTION_STOP = "com.example.focus.STOP"

    fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
      val mgr = context.getSystemService(NotificationManager::class.java)
      if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
        mgr.createNotificationChannel(
          NotificationChannel(CHANNEL_ID, "Focus timer", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Ongoing notification while a focus session is running"
            setShowBadge(false)
          }
        )
      }
    }

    fun start(context: Context) {
      ensureChannel(context)
      val i = Intent(context, FocusTimerService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
      else context.startService(i)
    }

    fun send(context: Context, action: String) {
      context.startService(Intent(context, FocusTimerService::class.java).setAction(action))
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, FocusTimerService::class.java))
    }
  }
}
