# RAFAELIA · Atlas CI/Bootstrap Ω — 2026-10-07

**Escopo da observação:** leitura estática de 112 arquivos YAML no `main` de três repositórios: termux-app-rafacodephi (72), termux-packages (39) e RafGitTools (1). Inventário de eventos, condições, cancelamento, referências de Actions, permissões declaradas e **nomes** de segredos. Não foi lido nenhum valor de Secret nem confirmados os escopos da credencial PAT. Fonte é distinta de resultado CI.

## Topologia mínima com autoridade

```text
RafGitTools START.yml [PLANEJAR / AUTORIDADE / P0]
    │ valida eventos, proveniência e direitos; sem acionar segredos em PR não confiável
    ├── termux-packages/rafcodephi-auto-handoff.yml [PRODUTOR]
    │       μ contrato → BUILD ARM+ARM64 → SHA/ZIPRAF
    │       → verificar batches independentes → fan-in → dispatch com artifact-id/digest
    └── termux-app-rafacodephi/rafcodephi-v1-termux-packages.yml [CONSUMIDOR]
            μ verificar SHA/ref/receipt → baixar artifact exato
            → conferir bootstrap ARM+ARM64 → APK → receipt
            → execução Android física = TOKEN_VAZIO até aferida
                └── sign-release.yml [ASSINAR ≠ PUBLICAR]
                        publication = DRAFT manual após gates
```

## Correções source-side desta rodada, aguardando CI exato

| Rota | PR | Alteração | Estado |
| --- | --- | --- | --- |
| termux-packages | [#139](https://github.com/rafaelmeloreisnovo/termux-packages/pull/139) | Produtor só em mudanças relevantes; não cancelar builds `main` rodando; preservar fan-in | DRAFT / CI PENDING |
| termux-app-rafacodephi | [#497](https://github.com/rafaelmeloreisnovo/termux-app-rafacodephi/pull/497) | Concurrency por evento/producer-run, manual isolado; legados ARM32 duplicados só manuais | DRAFT / CI PENDING |
| termux-app-rafacodephi | [#498](https://github.com/rafaelmeloreisnovo/termux-app-rafacodephi/pull/498) | Segregar `contents: read` do build e `contents: write` da criação manual de *draft*; tag push não publica | DRAFT / CI PENDING |
| RafGitTools | [#655](https://github.com/rafaelmeloreisnovo/RafGitTools/pull/655) | `PAT_ENV` somente evento permitido, ator dono, `main` e ambiente governado | DRAFT / CI PENDING |

As PRs são independentes; não exige cherry-pick cruzado para testes estáticos. **Nenhum PR deste conjunto está comprovado como instalado em APK físico ou release.**

## Invariantes e testes falsificadores

1. `SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM`.
2. `TOKEN_VAZIO ≠ 0`; teste CI `PASS` não equivale a prova Android física.
3. Producer só despacha após `contract→produce→(verify_arm ∥ verify_aarch64)→join`; sem fan-in não existe handoff.
4. `repository_dispatch` pertence ao `producer_run_id`; o consumidor confere manifest, sha256 e id/digest do artifact antes do APK.
5. `workflow_dispatch` manual recebe grupo distinto para não cancelar por engano o job `push` do mesmo ref.
6. Workflows marcados `ci_track: deprecated` não fazem build em `push` automaticamente; preservar para replay manual.
7. Secret de leitura de repositório não deve servir automaticamente como segredo de publicação/deploy. `PAT_ACTIONS`, `PAT_ENV`, `GITPAT/PATGITHUB/GIT`, APT signing e release keystore são **capacidades diferentes**.
8. `publish`/GitHub Release não segue de `signed-apk`: exige aprovação P0, licença, cadeia de custódia, assinatura e receipt de dispositivo; draft não é release pública.
9. Em concurrency com `cancel-in-progress:false`, execuções **pending** ainda podem ser substituídas pelo GitHub; preservar a prova de cada execução que iniciou e não confundir cancelamento com falha de source.
10. `pull_request` executa contratos baratos sem segredos privilegiados; jobs com PAT bloqueados pelo guard de ator/ref/evento e provider workflow policy.

## Riscos independentes não corrigidos por esta rodada

- Permissões *reais*, validade, expiração e abrangência de qualquer PAT: **TOKEN_VAZIO**, pois API de Secrets não expõe valores/escopos aqui.
- Cotas de Actions, artifact upload e Copilot Code Scanning: falhas de provider `≠` falhas de source.
- Workflow APT signing e release legados, `Dockerfile` e referências de Actions em versões heterogêneas: saneamento futuro em PR dedicado, com teste de compatibilidade e zero alteração indiscriminada.
- ZIP bootstrap alternativo com duplo consentimento, PR [#495](https://github.com/rafaelmeloreisnovo/termux-app-rafacodephi/pull/495), mantém blocker [#496](https://github.com/rafaelmeloreisnovo/termux-app-rafacodephi/issues/496) de preflight antes de qualquer mutação do prefixo.
- Publicação do repo APT próprio, `pkg update` do prefixo real, ARMv7+AArch64 físico, assinatura por dispositivo, proteção branch/ruleset e autorização de redistribuição ainda exigem receipts próprios.

## Ordem de fechamento sem regressão

`CP00 direitos → CP01 workflow lint → CP02 unit contract → CP03 produtor ZIP+SHA → CP04 fan-in ARM/ARM64 → CP05 consume artifact_id+digest → CP06 APK signed_candidate → CP07 device receipt → CP08 release owner authorization`.

**Rollback:** todas as mudanças propostas são ramos/PRs isolados; reverter commit da PR antes do merge ou restaurar exatamente o YAML anterior. Nunca apagar releases, segredos, artifacts existentes ou trocar hashes sem receipt.

`R3 = ⟨F_ok: atlas + patches source-side gravados, F_gap: CI/quotas/PAT scopes/proteção/device, F_next: execução CI exata e provas por ABI sem promoção antecipada⟩`.
