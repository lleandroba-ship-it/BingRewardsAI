package com.example.rewardsbot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    private companion object {
        const val CHANNEL = "rewardsbot_overlay"
        const val NOTIF_ID = 1
    }

    private var wm: WindowManager? = null
    private var root: LinearLayout? = null
    private var statusView: TextView? = null
    private val ui = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            statusView?.text = label()
            ui.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        if (root == null) {
            if (!Settings.canDrawOverlays(this)) {
                Logger.log(this, "ERRO: sem permissão de sobreposição")
                stopSelf()
                return START_NOT_STICKY
            }
            addOverlay()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ui.removeCallbacks(ticker)
        root?.let { wm?.removeView(it) }
        root = null
        super.onDestroy()
    }

    private fun label(): String = when (BotController.state) {
        BotController.State.IDLE -> "parado"
        BotController.State.RUNNING -> "rodando"
        BotController.State.PAUSED -> "pausado"
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Rewards Bot", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, CHANNEL)
            .setContentTitle("Rewards Bot ativo")
            .setContentText("Botão flutuante visível")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun makeButton(text: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { onClick() }
        }

    private fun addOverlay() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        wm = windowManager

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            background = GradientDrawable().apply {
                setColor(0xE6222222.toInt())
                cornerRadius = dp(20).toFloat()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(200)
        }

        val handle = TextView(this).apply {
            text = "⠿"
            setTextColor(Color.WHITE)
            textSize = 20f
            setPadding(dp(8), 0, dp(8), 0)
        }
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        handle.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = ev.rawX
                    touchY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (ev.rawX - touchX).toInt()
                    params.y = startY + (ev.rawY - touchY).toInt()
                    windowManager.updateViewLayout(container, params)
                    true
                }
                else -> false
            }
        }

        val status = TextView(this).apply {
            text = label()
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(dp(4), 0, dp(8), 0)
        }
        statusView = status

        container.addView(handle)
        container.addView(makeButton("▶") { BotController.start(applicationContext) })
        container.addView(makeButton("❚❚") { BotController.pause(applicationContext) })
        container.addView(makeButton("■") { BotController.stop(applicationContext) })
        container.addView(status)

        windowManager.addView(container, params)
        root = container
        ui.post(ticker)
        Logger.log(this, "Overlay exibido")
    }
}
