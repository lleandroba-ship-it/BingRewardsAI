package com.example.rewardsbot

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private const val TAG = "RewardsBot"
    private const val MAX_BYTES = 1_000_000L
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun file(ctx: Context): File = File(ctx.applicationContext.filesDir, "bot.log")

    @Synchronized
    fun log(ctx: Context, msg: String) {
        Log.i(TAG, msg)
        try {
            val f = file(ctx)
            if (f.exists() && f.length() > MAX_BYTES) f.writeText("")
            f.appendText("${fmt.format(Date())}  $msg\n")
        } catch (e: Exception) {
            Log.e(TAG, "falha ao gravar log", e)
        }
    }
}
