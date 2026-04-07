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
import androidx.glance.appwidget.updateAll
import com.openappslabs.coffee.R
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.widgets.CoffeeWidget
import com.openappslabs.coffee.widgets.NothingCoffeeWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

            val notification = buildNotification("Time remaining $timeString")
            notificationManager.notify(NOTIFICATION_ID, notification)

            if (totalSeconds % 60 == 0L) {
                serviceScope.launch { updateAllWidgets() }
            }

            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val CHANNEL_ID = "coffee_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val REQ_STOP = 1
        private const val REQ_EXTEND = 2

        const val ACTION_STOP = "com.openappslabs.coffee.ACTION_STOP"
        const val ACTION_EXTEND = "com.openappslabs.coffee.ACTION_EXTEND"
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

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopCoffee()
            ACTION_EXTEND -> handleExtend()
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

    private fun handleExtend() {
        serviceScope.launch {
            if (endTimeMillis == 0L) return@launch

            val extendMillis = dataStore.observeDuration().first() * 60_000L
            endTimeMillis = endTimeMillis.coerceAtLeast(System.currentTimeMillis()) + extendMillis

            dataStore.setCoffeeStatus(active = true, endTime = endTimeMillis)

            val remainingMinutes = ((endTimeMillis - System.currentTimeMillis()) / 60_000L).toInt()
            manageWakeLock(remainingMinutes)

            handler.removeCallbacks(timerRunnable)
            handler.post(timerRunnable)

            updateTileAndWidgets()
        }
    }

    private fun startCoffee(durationMinutes: Int) {
        val startTime = System.currentTimeMillis()
        val endTime = if (durationMinutes > 0) startTime + (durationMinutes * 60 * 1000L) else 0L

        endTimeMillis = endTime

        serviceScope.launch {
            dataStore.setCoffeeStatus(active = true, endTime = endTime)
            updateTileAndWidgets()
        }

        val initialText = if (durationMinutes == 0) {
            "Active indefinitely"
        } else {
            String.format(Locale.getDefault(), "Time remaining %02d:00", durationMinutes)
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
            handler.post(timerRunnable)
        }
    }

    private fun stopCoffee() {
        serviceScope.launch {
            dataStore.setCoffeeStatus(false)
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
                val remainingMillis = endTimeMillis - System.currentTimeMillis()
                remainingMillis.coerceAtLeast(0L) + 2000L
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

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Coffee is Active")
            .setSmallIcon(R.drawable.app_icon)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(0, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
    }

    private fun buildNotification(contentText: String): Notification {
        notificationBuilder.clearActions()

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val stopIntent = PendingIntent.getService(this, REQ_STOP,
            Intent(this, CoffeeService::class.java).apply { action = ACTION_STOP }, flags)
        notificationBuilder.addAction(0, "Stop", stopIntent)
        if (endTimeMillis > 0) {
            val extendIntent = PendingIntent.getService(this, REQ_EXTEND,
                Intent(this, CoffeeService::class.java).apply { action = ACTION_EXTEND }, flags)
            notificationBuilder.addAction(0, "Extend Time", extendIntent)
        }

        return notificationBuilder
            .setContentText(contentText)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        handler.removeCallbacksAndMessages(null)

        wakeLock?.let {
            if (it.isHeld) {
                try { it.release() } catch (ignored: Exception) {}
            }
        }

        serviceScope.launch(Dispatchers.IO) {
            dataStore.setCoffeeStatus(false)
            updateAllWidgets()
        }

        try { unregisterReceiver(screenStateReceiver) } catch (ignored: Exception) {}

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
            TileService.requestListeningState(
                applicationContext,
                ComponentName(this, CoffeeTileService::class.java)
            )
        } catch (ignored: Exception) {}

        serviceScope.launch { updateAllWidgets() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Coffee Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the screen awake"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> stopCoffee()
            }
        }
    }
}