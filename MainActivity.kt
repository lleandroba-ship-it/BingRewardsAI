package com.example.rewardsbot

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        status = TextView(this).apply { textSize = 15f }
        col.addView(status)

        fun add(label: String, action: () -> Unit) {
            col.addView(Button(this).apply {
                text = label
                setOnClickListener { action() }
            })
        }

        add("1. Permitir sobreposição") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        add("2. Info do app (liberar configurações restritas)") {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        }
        add("3. Abrir configurações de acessibilidade") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        add("4. Mostrar botão flutuante") { showOverlay() }
        add("Esconder botão flutuante") { stopService(Intent(this, OverlayService::class.java)) }
        add("Exportar log para Downloads") { exportLog() }

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        val overlay = if (Settings.canDrawOverlays(this)) "OK" else "FALTA"
        val a11y = if (BotAccessibilityService.instance != null) "ativa" else "inativa"
        val bing = if (packageManager.getLaunchIntentForPackage(Selectors.BING_PKG) != null)
            "instalado" else "NÃO encontrado"
        status.text = "Sobreposição: $overlay\nAcessibilidade: $a11y\n" +
            "Bing: $bing\nBot: ${BotController.state}"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Conceda a sobreposição primeiro")
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        startForegroundService(Intent(this, OverlayService::class.java))
    }

    private fun exportLog() {
        val src = Logger.file(this)
        if (!src.exists()) {
            toast("Log vazio")
            return
        }
        try {
            val name = "rewardsbot-log-${System.currentTimeMillis()}.txt"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore recusou")
            contentResolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { it.copyTo(out) }
            }
            toast("Salvo em Downloads/$name")
        } catch (e: Exception) {
            toast("Falha ao exportar: ${e.message}")
        }
    }
}
