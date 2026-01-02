package com.openappslabs.coffee.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import com.openappslabs.coffee.R
import com.openappslabs.coffee.data.CoffeeManager

class CoffeeService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    private var endTimeMillis: Long = 0

    private val timerRunnable = object : Runnable {
        override fun run() {
            val remainingMillis = endTimeMillis - System.currentTimeMillis()

            if (remainingMillis <= 0) {
                stopSelf()
                return
            }

            val totalSeconds = remainingMillis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60

            val timeString = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
            updateNotification("Remaining time: $timeString")

            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val CHANNEL_ID = "coffee_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val REQ_STOP = 1
        private const val REQ_NEXT = 2

        const val ACTION_STOP = "com.openappslabs.coffee.ACTION_STOP"
        const val ACTION_NEXT_TIMEOUT = "com.openappslabs.coffee.ACTION_NEXT_TIMEOUT"
        const val EXTRA_DURATION_MINUTES = "DURATION_MINUTES"

        @Volatile
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        initNotificationBuilder()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Coffee::ScreenAwakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_NEXT_TIMEOUT -> handleNextTimeout()
            else -> {
                val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, -1)
                    ?.takeIf { it != -1 } ?: CoffeeManager.getSelectedDuration(this)
                startCoffee(durationMinutes)
            }
        }
        return START_STICKY
    }

    private fun handleNextTimeout() {
        val options = CoffeeManager.TIME_OPTIONS
        val currentDuration = CoffeeManager.getSelectedDuration(this)
        val currentIndex = options.indexOf(currentDuration)
        val nextIndex = (currentIndex + 1) % options.size
        val nextDuration = options[nextIndex]

        CoffeeManager.setSelectedDuration(this, nextDuration)
        startCoffee(nextDuration)
    }

    private fun startCoffee(durationMinutes: Int) {
        CoffeeManager.setCoffeeActive(this, true)
        updateTile()

        val initialText = if (durationMinutes == 0) "Active indefinitely" else "Remaining time: $durationMinutes:00"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, buildNotification(initialText), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(initialText))
        }

        manageWakeLock(durationMinutes)

        handler.removeCallbacks(timerRunnable)
        if (durationMinutes > 0) {
            endTimeMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
            handler.post(timerRunnable)
        }
    }

    private fun updateNotification(contentText: String) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun manageWakeLock(durationMinutes: Int) {
        wakeLock?.let { lock ->
            try {
                if (lock.isHeld) lock.release()
            } catch (e: Exception) {
                android.util.Log.e("CoffeeService", "WakeLock release failed", e)
            }

            if (durationMinutes > 0) {
                lock.acquire(durationMinutes * 60 * 1000L + 1000L)
            } else {
                val twentyFourHours = 24 * 60 * 60 * 1000L
                lock.acquire(twentyFourHours)
            }
        }
    }

    private fun initNotificationBuilder() {
        val stopPendingIntent = PendingIntent.getService(
            this, REQ_STOP,
            Intent(this, CoffeeService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this, REQ_NEXT,
            Intent(this, CoffeeService::class.java).apply { action = ACTION_NEXT_TIMEOUT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Coffee Active")
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(0, "Next timeout", nextPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    private fun buildNotification(contentText: String): Notification {
        return notificationBuilder.setContentText(contentText).build()
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        wakeLock?.let { if (it.isHeld) it.release() }

        CoffeeManager.setCoffeeActive(this, false)
        updateTile()
        super.onDestroy()
    }

    private fun updateTile() {
        try {
            TileService.requestListeningState(this, ComponentName(this, CoffeeTileService::class.java))
        } catch (ignored: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Coffee Service", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Keeps the screen awake"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }
}
