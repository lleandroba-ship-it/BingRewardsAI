package com.example.rewardsbot

/** Todos os seletores e constantes de ambiente ficam aqui. */
object Selectors {
    const val BING_PKG = "com.microsoft.bing"

    /** Pacotes cujos eventos de janela não contam como "trocar de app". */
    val IGNORED_PACKAGES = setOf(
        "com.android.systemui",
        "android",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.touchtype.swiftkey"
    )

    // Cascata para achar o campo de busca (usada a partir da etapa 2):
    // 1) classe, 2) contentDescription, 3) texto/dica.
    val SEARCH_FIELD_CLASSES = listOf("android.widget.EditText")
    val SEARCH_FIELD_DESC_HINTS = listOf("pesquis", "search")
    val SEARCH_FIELD_TEXT_HINTS = listOf("pesquis", "search", "pergunt", "ask")

    const val BING_LAUNCH_TIMEOUT_MS = 15_000L
    const val FOREGROUND_LOST_TOLERANCE_MS = 5_000L
}
