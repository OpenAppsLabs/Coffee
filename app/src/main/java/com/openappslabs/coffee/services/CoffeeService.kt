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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import com.openappslabs.coffee.R
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.widgets.CoffeeWidget
import com.openappslabs.coffee.widgets.NothingCoffeeWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class CoffeeService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val dataStore by lazy { CoffeeDataStore(applicationContext) }
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
            val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            val notification = buildNotification("Remaining time: $timeString")
            notificationManager.notify(NOTIFICATION_ID, notification)

            if (totalSeconds % 30 == 0L) {
                serviceScope.launch { updateAllWidgets() }
            }

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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED)
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
                        val savedDuration = dataStore.observeDuration().first()
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
            val currentDuration = dataStore.observeDuration().first()
            val currentIndex = options.indexOf(currentDuration)
            val nextIndex = (currentIndex + 1) % options.size
            val nextDuration = options[nextIndex]

            dataStore.setSelectedDuration(nextDuration)
            startCoffee(nextDuration)
        }
    }

    private fun startCoffee(durationMinutes: Int) {
        val startTime = System.currentTimeMillis()
        val endTime = if (durationMinutes > 0) startTime + (durationMinutes * 60 * 1000L) else 0L

        serviceScope.launch {
            dataStore.setCoffeeStatus(active = true, startTime = startTime, endTime = endTime)
            updateTileAndWidgets()
        }

        val initialText = if (durationMinutes == 0) {
            "Active indefinitely"
        } else {
            String.format(Locale.getDefault(), "Remaining time: %d:00", durationMinutes)
        }

        val notification = buildNotification(initialText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        manageWakeLock(durationMinutes)

        handler.removeCallbacks(timerRunnable)
        if (durationMinutes > 0) {
            endTimeMillis = endTime
            handler.post(timerRunnable)
        }
    }

    private fun stopCoffee() {
        serviceScope.launch {
            dataStore.setCoffeeActive(false)
            updateTileAndWidgets()
            stopSelf()
        }
    }

    private fun manageWakeLock(durationMinutes: Int) {
        wakeLock?.let { lock ->
            try {
                if (lock.isHeld) lock.release()
            } catch (ignored: Exception) {}

            val timeout = if (durationMinutes > 0) {
                (durationMinutes * 60 * 1000L) + 2000L
            } else {
                12 * 60 * 60 * 1000L
            }
            lock.acquire(timeout)
        }
    }

    private fun initNotificationBuilder() {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val stopIntent = PendingIntent.getService(this, REQ_STOP,
            Intent(this, CoffeeService::class.java).apply { action = ACTION_STOP }, flags)

        val nextIntent = PendingIntent.getService(this, REQ_NEXT,
            Intent(this, CoffeeService::class.java).apply { action = ACTION_NEXT_TIMEOUT }, flags)

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Coffee is Active")
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(0, "Next timeout", nextIntent)
            .addAction(0, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
    }

    private fun buildNotification(contentText: String): Notification {
        return notificationBuilder.setContentText(contentText).build()
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)

        if (wakeLock?.isHeld == true) {
            try { wakeLock?.release() } catch (ignored: Exception) {}
        }

        serviceScope.launch(Dispatchers.IO) {
            dataStore.setCoffeeActive(false)
            updateAllWidgets()
        }

        try { unregisterReceiver(screenOffReceiver) } catch (e: Exception) {}

        handler.postDelayed({ serviceScope.cancel() }, 500)
        super.onDestroy()
    }

    private suspend fun updateAllWidgets() {
        withContext(Dispatchers.IO) {
            CoffeeWidget().updateAll(applicationContext)
            NothingCoffeeWidget().updateAll(applicationContext)
        }
    }

    private fun updateTileAndWidgets() {
        try {
            TileService.requestListeningState(applicationContext, ComponentName(this, CoffeeTileService::class.java))
        } catch (ignored: Exception) {}

        serviceScope.launch { updateAllWidgets() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Coffee Service", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Keeps the screen awake"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) stopCoffee()
        }
    }
}