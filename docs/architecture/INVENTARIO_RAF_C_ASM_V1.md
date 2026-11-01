# InventarioRAF — core de metadados C/ASM V1

**Estado:** `IMPLEMENTED_UNTESTED_ANDROID`; `claim_allowed=false`  
**Repositório produtor:** `rafaelmeloreisnovo/RafGitTools`  
**Base observada:** `main@b1efc42319d0eaa4ed22c995edc8265d4d934269`  
**Fronteira:** inventário de metadados da árvore SAF escolhida; sem leitura ou cópia de corpos de arquivos.

## Estado atual observado

O `NovoexportSafInventory` percorre uma árvore SAF fornecida pelo usuário e
enumera nomes, MIME, tamanhos reportados e relações pai/filho. A tela de
primeira execução limita o índice publicado a 5.000 documentos e exige escolha
explícita do destino. O índice não prova cobertura do Drive inteiro nem acesso
a outras contas.

Esta alteração adiciona um analisador freestanding para cada caminho e conecta
seu tokenizer ao scanner e ao índice `rafgittools.drive-saf-metadata-index.v2`.
O núcleo recebe spans de metadados enviados pelo adaptador Android; não fala
com Drive/SAF, não abre conteúdo, não usa heap, libc, syscalls ou rede.

## Estrutura e fronteiras

```text
Google Drive / SAF provider
        │ árvore selecionada pelo usuário
        ▼
Android Kotlin — enumera metadados, guarda recibo/estado de UI
        │ JNI — caminho, MIME, tamanho conhecido e presença de pai
        ▼
C freestanding — 5 passagens determinísticas e spans de tokens
        ├── ARMv7 ASM — conta componentes do caminho
        ├── AArch64 ASM — conta componentes do caminho
        └── fallback C — demais alvos do core
        │ tokens lexicais + classificação estrutural
        ▼
Índice JSON v2 → exportação explícita pelo seletor SAF
```

`ASM` e C precisam produzir a mesma contagem de componentes. A contagem é uma
propriedade estrutural; o assembly não implica ganho de velocidade. O custo da
travessia JNI por item e a ampliação do índice por tokens ainda não foram
medidos em aparelhos.

## Cinco passagens

| Passagem | Operação | Limite |
|---|---|---|
| 1 | validar caminho relativo, componentes vazios e `.`/`..` | não valida ACL nem autoria do arquivo |
| 2 | produzir spans de tokens ASCII delimitados sem copiar no core | lexical; sem stemming ou modelo semântico |
| 3 | classificar diretório, JSON, arquivo-texto, arquivo compactado ou desconhecido | MIME/extensão observados; não abre nem inspeciona bytes |
| 4 | preservar `size_known` separado do número | zero-byte não é tamanho desconhecido |
| 5 | registrar se há referência de pai | `CONTAINS` estrutural não é relação semântica |

`pass_mask=0x1f` significa que as cinco passagens estruturais terminaram. Não é
um gate de verdade científica, completude global ou validação humana.
Um caminho estruturalmente válido pode resultar em lista de tokens vazia; essa
ausência lexical é preservada como estado válido e não é substituída por token
inventado.

## Comportamento de falha

- caminho inválido, excesso de tokens ou análise incompleta: rejeitar a linha;
- biblioteca JNI indisponível: interromper a varredura, sem fallback silencioso
  para uma implementação diferente;
- tamanho ausente: `size_known=false`; tamanho reportado zero:
  `size_known=true, reported_bytes=0`;
- limite de itens ou índice excedido: rejeitar o snapshot, preservar a fonte e
  não publicar um índice parcial como completo;
- falha de gravação/readback: a fonte não é alterada; a exportação não recebe
  receipt de sucesso;
- rollback de código: reverter o commit da feature; o índice v1 e a árvore
  fonte continuam inalterados.

## Multi-ABI observado e adaptação

O app declara somente `armeabi-v7a` e `arm64-v8a`; CMake escolhe ASM por ABI e
mantém C de referência/fallback. Isso atende aos alvos ARM listados no produtor,
mas **não** autoriza dizer “qualquer aparelho”. x86/x86_64, RISC-V, dispositivos
sem Android/JNI, Android 10/14 físico e combinações incomuns de provedor SAF
continuam sem gate nesta alteração.

## Validação requerida

1. `make -C native/inventario_raf_v1 clean test` — self-test C local e objetos
   ARMv7/AArch64 no CI.
2. `testDevDebugUnitTest` — contrato do índice JSON v2.
3. `assembleDevDebugAndroidTest` — compila os testes JNI por ABI.
4. Executar instrumentation em ARMv7 e ARM64; comparar ASM com C para caminhos
   ASCII/UTF-8, diretórios vazios, nome unicode, tamanho `0`/desconhecido,
   travessia rejeitada e limite de tokens.
5. Teste físico no Android: selecionar pasta real do Drive, pausar/retomar,
   gerar/exportar somente metadados, ler de volta e verificar hash sem abrir os
   corpos dos arquivos.
6. Medir tempo, alocações, bytes de heap, contagem de transições de cache e
   tamanho do índice contra o baseline Kotlin no mesmo aparelho/ABI. Sem essa
   comparação, desempenho e cache hit/miss ficam `TOKEN_VAZIO`.

Os itens 4–6 não são executados nesta etapa. Pause/resume durável, watchdog,
exportação em chunks, failover de provedor e cobertura além do limite de 5.000
documentos seguem como lacunas explícitas. C compilado e self-test host PASS
não provam APK integrado, execução física ou acesso global ao Drive.

## Fato, artefato, execução, evidência e claim

- **SOURCE:** árvore SAF selecionada e os metadados retornados pelo provedor.
- **ARTIFACT:** código C/ASM/JNI e índice metadata-only v2.
- **EXECUTION:** host self-test C foi executado; Android build, instrumentation
  e provider físico ainda precisam de CI/aparelho.
- **EVIDENCE:** saída de self-test host sem erro; Gradle local bloqueado porque
  não conseguiu baixar Gradle 9.5.1 devido a rede indisponível.
- **CLAIM:** `claim_allowed=false` até gates do produtor e teste físico exato.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`  
`TOKEN_VAZIO != 0`  
`IMPLEMENTED_UNTESTED != PASS`
