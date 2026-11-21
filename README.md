# Rewards Bot (Etapa 1: projeto + overlay + abrir o Bing)

## Build pelo GitHub (sem PC)
1. Suba todo o conteúdo desta pasta na raiz de um repositório (inclusive `.github/`).
2. Aba **Actions** > workflow **Build APK** (roda a cada push).
3. Ao terminar, abra a execução e baixe o artifact **rewards-bot-debug-apk**.
4. Descompacte e instale o `.apk` (permita "instalar apps desconhecidos" para o navegador/gerenciador).

Obs.: a chave debug é gerada a cada build, então para instalar uma versão nova
é preciso desinstalar a anterior antes.

## Permissões
1. Abra o Rewards Bot > **1. Permitir sobreposição** > ligue.
2. Android 13+ (app instalado fora da loja): **2. Info do app** > menu ⋮ >
   **Permitir configurações restritas**. Sem isso a acessibilidade fica cinza.
3. **3. Acessibilidade** > Rewards Bot > ligue e confirme.
4. **4. Mostrar botão flutuante** (aceite a notificação).
5. Bateria: Configurações > Apps > Rewards Bot > Bateria > Sem restrições.

## Teste da etapa 1
- Desbloqueie a tela, toque ▶ no botão flutuante: o Bing deve abrir.
- ❚❚ pausa, ■ para. Se você sair do Bing por mais de 5 s, o bot aborta e registra no log.
- **Exportar log para Downloads** gera um .txt com o histórico.
