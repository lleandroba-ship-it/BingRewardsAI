package com.example.rewardsbot

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast

object BotController {
    enum class State { IDLE, RUNNING, PAUSED }

    @Volatile
    var state = State.IDLE
        private set

    private val main = Handler(Looper.getMainLooper())
    private var worker: Thread? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private fun toast(ctx: Context, msg: String) {
        main.post { Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
    }

    private fun isLocked(ctx: Context): Boolean =
        (ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked

    @Synchronized
    fun start(context: Context) {
        val ctx = context.applicationContext
        when (state) {
            State.RUNNING -> return
            State.PAUSED -> {
                state = State.RUNNING
                Logger.log(ctx, "Retomado")
                return
            }
            State.IDLE -> {}
        }

        val launch = ctx.packageManager.getLaunchIntentForPackage(Selectors.BING_PKG)
        if (launch == null) {
            Logger.log(ctx, "ERRO: ${Selectors.BING_PKG} não está instalado")
            toast(ctx, "App do Bing não encontrado")
            return
        }
        if (BotAccessibilityService.instance == null) {
            Logger.log(ctx, "ERRO: serviço de acessibilidade desativado")
            toast(ctx, "Ative o serviço de acessibilidade do Rewards Bot")
            return
        }
        if (isLocked(ctx)) {
            Logger.log(ctx, "ERRO: tela bloqueada, não iniciei")
            toast(ctx, "Desbloqueie a tela")
            return
        }

        state = State.RUNNING
        acquireWakeLock(ctx)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        worker = Thread { runSession(ctx, launch) }.apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun pause(context: Context) {
        if (state == State.RUNNING) {
            state = State.PAUSED
            Logger.log(context.applicationContext, "Pausado")
        }
    }

    @Synchronized
    fun stop(context: Context) {
        if (state != State.IDLE) {
            Logger.log(context.applicationContext, "Parado pelo usuário")
            state = State.IDLE
            worker?.interrupt()
        }
    }

    private fun runSession(ctx: Context, launch: Intent) {
        try {
            Logger.log(ctx, "Abrindo o app do Bing")
            ctx.startActivity(launch)

            val deadline = System.currentTimeMillis() + Selectors.BING_LAUNCH_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline &&
                BotAccessibilityService.currentPackage != Selectors.BING_PKG
            ) {
                Thread.sleep(300)
            }
            if (BotAccessibilityService.currentPackage != Selectors.BING_PKG) {
                Logger.log(
                    ctx,
                    "ERRO: Bing não ficou em primeiro plano (atual: ${BotAccessibilityService.currentPackage})"
                )
                toast(ctx, "Bing não abriu. Veja o log.")
                return
            }
            Logger.log(ctx, "Bing em primeiro plano (etapa 1: apenas abrir e monitorar)")
            toast(ctx, "Bing aberto")

            var lostSince = 0L
            while (state != State.IDLE) {
                Thread.sleep(500)
                if (state == State.PAUSED) {
                    lostSince = 0L
                    continue
                }
                if (isLocked(ctx)) {
                    Logger.log(ctx, "Tela bloqueada: pausando")
                    state = State.PAUSED
                    continue
                }
                val now = System.currentTimeMillis()
                if (BotAccessibilityService.currentPackage == Selectors.BING_PKG) {
                    lostSince = 0L
                } else if (lostSince == 0L) {
                    lostSince = now
                } else if (now - lostSince > Selectors.FOREGROUND_LOST_TOLERANCE_MS) {
                    Logger.log(
                        ctx,
                        "Bing saiu de primeiro plano (agora: ${BotAccessibilityService.currentPackage}). Abortando."
                    )
                    toast(ctx, "Bing saiu de primeiro plano; parado")
                    break
                }
            }
        } catch (e: InterruptedException) {
            // parada solicitada
        } catch (e: Exception) {
            Logger.log(ctx, "ERRO inesperado: $e")
        } finally {
            finish(ctx)
        }
    }

    @Synchronized
    private fun finish(ctx: Context) {
        state = State.IDLE
        releaseWakeLock()
        Logger.log(ctx, "Sessão encerrada")
    }

    private fun acquireWakeLock(ctx: Context) {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rewardsbot:session").apply {
            setReferenceCounted(false)
            acquire(4 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }
}
