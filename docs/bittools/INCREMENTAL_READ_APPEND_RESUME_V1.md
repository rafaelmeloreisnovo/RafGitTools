# BitTools / RafGitTools — ler → gravar → retomar (v1)

**Produtor:** `rafaelmeloreisnovo/RafGitTools`. **Estado:** proposta isolada DRAFT/HOLD.
**Sem inventar um BitTools autônomo:** o inspetor `tools/zipraf_bit_layer_inspector_v1.py` já existia para vetores canônicos específicos. Este novo consumidor implementa a lacuna de varredura binária de *arquivos locais selecionados*, sem substituir o contrato ZIPRAF.

## Uso real em um diretório já disponibilizado por um operador autorizado

```sh
python3 -I -B tools/bittools_incremental_byte_ledger_v1.py \
  --root /workspace/entrada-autorizada \
  --ledger /workspace/receipts/eventos.jsonl \
  --chunk-bytes 1048576 --max-bytes 33554432 --max-events 512
```

Repita **o mesmo comando**, com o mesmo diretório e ledger. A cada chamada ele:
1. Reconstrói e valida toda a cadeia hash SHA-256 dos eventos persistidos; nunca aceita corrupção/truncamento silenciosamente.
2. Caminha em ordem determinística pelas pastas. Cada arquivo começa com `file_begin` e um vínculo de tamanho, inode, dispositivo e mtime_ns; *symlinks não são seguidos*.
3. Lê bytes em blocos de até 8 MiB, produz `chunk` com offset, comprimento, SHA-256 do bloco e um resumo de anomalias (ASCII dois espaços, NUL, LF e CRLF; as fronteiras entre blocos são consideradas).
4. Anexa cada registro ao JSONL, chama `fsync`, então avança. Se interromper, o último offset íntegro é reconstruído do histórico. Fonte alterada bloqueia retomada; não faz escrita no arquivo original.
5. Quando todos os blocos daquele arquivo foram registrados, anexa `file_done`. Se for ZIP, **somente os metadados do diretório central** são percorridos com `zip_member` (CRC declarado, tamanhos, criptografia, symlink e tentativas de path traversal). **Nenhum membro é descompactado** e nenhum CRC interno é reivindicado como verificado.
6. Registra `cycle` com tempo e bytes efetivamente lidos: `PARTIAL_CHECKPOINT` ou `DONE_SCOPED`. Nenhum evento afirma análise de todos os formatos ou contas.

**Precisão da integridade:** `chunk_chain_sha256` é uma *cadeia derivada de SHA-256 por bloco*, não é o SHA-256 dos bytes concatenados do arquivo. O campo `file_sha256` permanece `TOKEN_VAZIO_NOT_COMPUTED`. Se o usuário exigir hash integral padrão, é necessário um passo de verificação distinto (ou manter o hasher continuamente sem pausas). Uma cópia do JSONL isolada também não protege contra uma reescrita maliciosa completa: guardar o hash-âncora e executar comparação independente em custódia protegida.

## Teste remoto de CI isolado e segurança

```sh
python3 -I -B -m unittest discover -s tools -p 'test_bittools_incremental_byte_ledger_v1.py' -v
python3 -I -B tools/bittools_incremental_byte_ledger_v1.py \
  --root /workspace/entrada-autorizada \
  --ledger /workspace/receipts/eventos.jsonl --verify
```

A CI executa **apenas fixtures sintéticas pequenas**, não percorre o Google Drive e não recebe arquivos pessoais. Não existe neste código um serviço Google Drive OAuth, token, listener ou downloader — `SOURCE_DRIVE=TOKEN_VAZIO_NOT_CONNECTED`. Uma integração cloud sem telefone precisaria de autorização separada, streaming seguro, quotas, controle de checkpoint/provider e armazenamento persistente para o ledger. GitHub Actions é efêmero: receipts publicados precisam ser espelhados e conferidos no Drive antes de qualquer claim de persistência durável.

Este v1 não interpreta semanticamente imagens, instruções de máquina, ASTs, células de planilhas, vídeos ou conteúdo de membros ZIP. A leitura binária é uma camada de observação de bytes; as camadas semânticas dependem de parsers específicos, testes negativos e fontes. Byte não observado ≠ zero. Nome ou tamanho igual ≠ conteúdo igual.

**Proteções:** saída externa ao diretório de origem, orçamento de bytes e eventos por ciclo, máximo 200 mil entradas de ZIP, 2 milhões de eventos para reconstrução de um ledger individual, rejeição de symlinks e paths inseguros, nenhum comando arbitrário e nenhum arquivo original modificado. Restrição atual: 2 milhões de eventos por ledger exigem partição e ponte de hash para projetos muito grandes. O índice pode conter nomes privados de arquivos; não o publique em repositório público.

## Alcance honesto e condição de parada

- `DONE_SCOPED`: a árvore *local selecionada* foi percorrida e os bytes de arquivos regulares no recorte tiveram hashes por bloco. Não significa cobertura da conta Drive, que a fonte permaneça imutável após o scan, nem que todo conteúdo do ZIP tenha sido analisado.
- `PARTIAL_CHECKPOINT`: orçamento da rodada terminou, continuável com o mesmo comando.
- `BLOCKED`: fonte alterada, ledger inválido, cap excedido, arquivo corrompido ou permissão insuficiente. Corrigir por novo ledger/sucessor sob autorização; não reescrever registros antigos para forçar PASS.

**Rollback:** fechar PR sem merge. Para execuções de teste, descartar **somente o diretório sintético autorizado**. Não eliminar um ledger real automaticamente.

R3 = <fonte CLI+testes, contas Drive e arquivos ZIP internos não processados, CI sintética exata + integração por autoridade>.
