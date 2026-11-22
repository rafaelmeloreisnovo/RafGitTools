# NOVOexport — genealogia por interação e token lexical V1

Status: SOURCE_IMPLEMENTED / CI_EXACT_HEAD_PENDING / FULL_CORPUS_NOT_RUN / MODEL_TOKENIZER_TOKEN_VAZIO.
Privacy: PRIVATE_DEFAULT_DENY. claim_allowed=false. Training/weight update not performed.

## Proveniência

Predecessores: RAFAELIA Navigator V1.2; full-recount-000-050.safe.v1.json; MESSAGES/NODES/CONVERSATIONS; Wave2→Wave4; Sementeira 5×7.
Vínculo técnico de feedback: frida-desktop PR #84. Frida não é repositório do corpus.

O recibo histórico registra 5054 conversas, 307043 nós, 301991 nós com mensagem, 296794 IDs efetivos únicos e 5197 observações excedentes de IDs repetidos ENTRE conversas. Logo message_id sozinho não identifica a ocorrência. O índice Navigator anterior não é modificado.

## Interação (distinta de conversa)

Interação = nó de papel user mais descendentes de cada ramificação até o próximo nó user. É reconstruída pela árvore de parent, não por inferência de uma sequência linear ou supostas respostas únicas. Respostas alternativas da IA permanecem distintas. Nós sem mensagem, system/developer/tool, ciclos, parent ausente e contexto anterior ao primeiro user não desaparecem.

A chave de ocorrência é SHA256 do objeto canônico (shard, posição da conversa no shard, conversation_id, node_id). A chave da interação substitui node_id pela âncora do prompt user. Cada linha contém source_pointer; campos privados de mensagem continuam privados, não são publicados.

## Operação local-first

Executar com cópias locais autorizadas e íntegras dos arquivos BRUTOS conversations-000.json a conversations-050.json. Os arquivos publicados MESSAGES-*.jsonl.txt não substituem esses brutos quando é necessário verificar a árvore completa.

Exemplo — um shard, emitindo os tokens lexicais em cada mensagem:

    python3 tools/rafaelia_navigator/interaction_token_ledger_v1.py \
      "$HOME/storage/shared/NOVOexport" "$HOME/rafaelia-interactions-private-a" \
      --first 0 --last 0 --emit-lexical-tokens

Modo econômico (somente contagens e compromissos lexicais, três shards):

    python3 tools/rafaelia_navigator/interaction_token_ledger_v1.py \
      "$HOME/storage/shared/NOVOexport" "$HOME/rafaelia-interactions-private-b" \
      --first 0 --last 50 --max-shards 3

Os caminhos de saída devem ser NOVOS, fora da origem. Pasta 0700 e arquivos 0600, criação exclusiva sem sobrescrever. Para cada shard:
- SHARD_NNN/INTERACTIONS_PRIVATE.jsonl — uma linha por nó, textos originais de usuário/IA e lexemas opcionais; não subir em GitHub público.
- SHARD_NNN/INTERACTION_INDEX_PRIVATE.jsonl — uma linha por prompt/interação, com ponteiros para o prompt e todas as respostas alternativas da IA, contagem por papel e tokens lexicais por papel; não duplica os corpos privados.
- SHARD_NNN/RECEIPT.json — SHA-256 origem antes/depois, dois hashes de output, contagens, lacunas e status.

## Tokens: réguas distintas

Token lexical: regex Python Unicode WORD_OR_PUNCT, expressão \w+|[^\w\s], digest SHA256 de sequência UTF-8 com prefixo de comprimento. Token lexical NÃO é token interno GPT ou BPE. Sem vocabulário/tokenizer exato e versão verificada, model_token_count e model_token_ids são TOKEN_VAZIO_MODEL_TOKENIZER_UNPINNED.

Partes textuais preservam caracteres/acentos da fonte sem normalização silenciosa. Partes multimodais não textuais recebem referência/hash e não viram texto fabricado. Não se acessa pensamento oculto, pesos, ativações ou mensagens ausentes do export.

## Gates e falsificadores

- SOURCE_IMPLEMENTED: script e testes sintéticos privados-segurança no PR draft.
- CI_EXACT_HEAD: depende de execução verificável pelo provider.
- FULL_SCAN_000_050: TOKEN_VAZIO_FULL_SCAN_NOT_RUN.
- MODEL_EXACT_TOKENIZATION: TOKEN_VAZIO_MODEL_TOKENIZER_UNPINNED.
- HUMAN_REVIEW: REQUIRED.
- TRAINING_EXECUTED: false.
- claim_allowed=false.

Testar ramificações concorrentes, segundo prompt na árvore, ID repetido em outra conversa, nós vazios, parent inexistente, ciclo, Unicode, multimodal e bloqueio de overwrite. Não afirmar cobertura plena com amostra ou hash substituto.

Fato: o export bruto expõe árvore e papéis. Lacuna: bytes de todos os shards e tokenizer exato. Invariante: SOURCE ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM e TOKEN_VAZIO ≠ zero. Variante: micro-lotes e nível opcional de tokens. Prova: SHA fonte antes/depois + ponteiro, parent e recibo. Parábola: duas folhas com o mesmo número ainda pertencem a árvores distintas. Retroalimentação: nova mensagem recebe predecessor e novas hipóteses de rapport exigem fonte.

R3 — F_ok: estrutura, privacidade, compilação e testes devem ser validados. F_gap: cobertura global e tokens do modelo. F_next: verificar CI no head exato e executar primeiro shard bruto autorizado, reconciliando progressivamente com full-recount-000-050.safe.v1.json antes de qualquer claim.
