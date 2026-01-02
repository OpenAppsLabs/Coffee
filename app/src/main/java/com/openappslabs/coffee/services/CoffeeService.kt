package com.openappslabs.coffee.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import com.openappslabs.coffee.R
import com.openappslabs.coffee.data.CoffeeDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CoffeeService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    private var endTimeMillis: Long = 0

    private val timerRunnable = object : Runnable {
        override fun run() {
            val remainingMillis = endTimeMillis - System.currentTimeMillis()

            if (remainingMillis <= 0) {
                stopCoffee()
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
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Coffee::ScreenAwakeLock"
        ).apply { setReferenceCounted(false) }
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopCoffee()
            ACTION_NEXT_TIMEOUT -> handleNextTimeout()
            else -> {
                val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, -1)
                    ?.takeIf { it != -1 }

                if (durationMinutes != null) {
                    startCoffee(durationMinutes)
                } else {
                    serviceScope.launch {
                        val savedDuration = CoffeeDataStore.observeDuration(this@CoffeeService).first()
                        startCoffee(savedDuration)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun handleNextTimeout() {
        serviceScope.launch {
            val options = CoffeeDataStore.TIME_OPTIONS
            val currentDuration = CoffeeDataStore.observeDuration(this@CoffeeService).first()
            val currentIndex = options.indexOf(currentDuration)
            val nextIndex = (currentIndex + 1) % options.size
            val nextDuration = options[nextIndex]

            CoffeeDataStore.setSelectedDuration(this@CoffeeService, nextDuration)
            startCoffee(nextDuration)
        }
    }

    private fun startCoffee(durationMinutes: Int) {
        serviceScope.launch {
            CoffeeDataStore.setCoffeeActive(applicationContext, true)
            updateTile()
        }

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

    private fun stopCoffee() {
        serviceScope.launch {
            CoffeeDataStore.setCoffeeActive(applicationContext, false)
            updateTile()
            stopSelf()
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
            .setContentTitle("Coffee is Active")
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

        val context = applicationContext
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            CoffeeDataStore.setCoffeeActive(context, false)
            try {
                TileService.requestListeningState(context, ComponentName(context, CoffeeTileService::class.java))
            } catch (ignored: Exception) {}
        }

        try { unregisterReceiver(screenOffReceiver) } catch (e: Exception) {}

        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateTile() {
        try {
            TileService.requestListeningState(applicationContext, ComponentName(applicationContext, CoffeeTileService::class.java))
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

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                stopCoffee()
            }
        }
    }
}