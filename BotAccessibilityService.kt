package com.example.rewardsbot

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class BotAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: BotAccessibilityService? = null
        @Volatile var currentPackage: String? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Logger.log(this, "Serviço de acessibilidade conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in Selectors.IGNORED_PACKAGES) return
        currentPackage = pkg
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        currentPackage = null
        Logger.log(this, "Serviço de acessibilidade desconectado")
        return super.onUnbind(intent)
    }
}
