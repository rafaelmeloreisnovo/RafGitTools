# RafGitTools PR #680 — Hotfix de consistência de hashes / TOCTOU

**Data:** 2026-10-15 · **Prioridade:** P0 custódia, integridade e proveniência · **Política:** DRAFT/NO_MERGE/claim_allowed=false.

## Falsificador reproduzível (source-side)

O adaptador `scripts/drive_custody_rmr_pair_v1.py` calcula SHA-256 do objeto de origem e readback, chama o binário externo `pai` para KAT e BLAKE3, e produz recibo com digests. Antes deste hotfix, uma modificação nos bytes *entre* o primeiro SHA-256 e a execução BLAKE3 poderia criar um recibo inconsistente com o estado observado. Isso era uma lacuna temporal (TOCTOU), não uma colisão criptográfica.

## Patch mínimo

- Refazer SHA-256 e contagem de bytes dos **dois** objetos depois de BLAKE3, antes de construir o recibo.
- Em qualquer mudança, `CustodyError('BYTES_CHANGED_DURING_HASH_VERIFICATION')` e nenhum recibo é emitido.
- Dois falsificadores adicionais: mutação da origem durante o subprocesso simulado e mutação do readback durante a segunda chamada simulada.
- Nenhum token, ID privado, conteúdo real do Drive, novo workflow ou mudança em main. `pai` permanece explicitamente hospedado.

## Limite honesto

A segunda leitura não substitui um snapshot atômico de filesystem (imunidade completa a corrida) nem valida assinatura, ACL ou autenticidade do ID do provedor. O subprocesso dos testes de mutação é *mock*, não execução real de BLAKE3. O teste em CI com `pai` pinado e o readback do ZIP real ainda precisam de recibos separados. Uma alteração e restauração entre duas leituras continua teoricamente possível; usar arquivos imutáveis/snapshot e obter observação física/provedor para fechamento robusto.

## Critérios de promoção não satisfeitos por este patch

1. Source unit tests e Android/CodeQL exatos no novo HEAD = **aguardando CI**.
2. KAT com binário RMR/pai pinado = **verificar logs/artefato executados**, sem depender de testes mock.
3. Raw readback real do mesmo arquivo com BLAKE3 e arquivo congelado = **TOKEN_VAZIO**.
4. Provider protection + revisão independente + rollback = **não verificados**.

## Rollback

Este hotfix só altera o branch do PR #680; para reverter, um commit sucessor que restaure os blobs anteriores `90358f6ed62bf11e6552284ac9a50de815a6ff57` (script) e `7bcf8f9c6eabc6e9660fa72a7601bc25fc294e0d` (testes), após avaliação de impacto. **Não apagar** recibos anteriores. Nenhuma ação automática de merge.

## R3

F_ok: risco de inconsistência detectado e guarda explícita inserida; 2 testes negativos source-side propostos. F_gap: resultados de CI em novo HEAD, atomicidade/snapshot e medição real BLAKE3. F_next: verificar HEAD/checks, binário pinado, dois digest KAT em arquivo real, independent review, só então avaliar promoção.
