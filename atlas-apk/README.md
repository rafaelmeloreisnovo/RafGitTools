# RAFAELIA Atlas - segundo APK independente
Package org.rafaelia.atlas; source candidate; GPL-3.0-or-later.
A aplicação RafGitTools atual, START.yml, secrets e operações de provider permanecem intactos.
L0 C freestanding classifica apenas nome/extension (sem malloc/libc/JNI/syscall); L1 JNI Android; L2 SAF; L3 CI debug APK arm32/arm64.
Seleção explícita de pasta, inventário até 5000 itens/16 níveis. Lê metadados apenas e produz JSONL local: caminho, SAF id, MIME, size opcional, prefixo, extensão e recibo. O SHA-256 do recibo cobre registros, não bytes originais.
Exportação apenas mediante ACTION_CREATE_DOCUMENT e destino escolhido. Caminhos/IDs podem ser sensíveis. Nunca exportar índice privado para CI ou GitHub público.
Teste: sh atlas-apk/tests/test_core.sh
Build: ./gradlew --no-daemon -p atlas-apk :app:assembleDebug :app:lintDebug
DEVICE/DRIVE/BYTES/SEMANTIC/RELEASE=TOKEN_VAZIO_NOT_RUN; claim_allowed=false. Sem treino nem sincronização automática.
