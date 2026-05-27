# RAFGITTOOLS_CURRENT_STATE

- Status: ATIVO (auditoria técnica em andamento)
- Última atualização: 2026-05-27
- Escopo: estado executável real do app Android + gaps de exposição em UI.
- Fonte de verdade: `app/src/main/kotlin`, `scripts/`, `docs/`.

## Resumo executivo

1. **Auth**: PAT está funcional e exposto; Device Code existe e agora tem ligação no `AuthViewModel`; SSH é parcial/placeholder; modo offline existe de forma mínima.
2. **Git local (JGit)**: camada operacional ampla existe no domínio/repositório/serviço, mas nem toda operação está exposta em tela.
3. **GitHub API**: serviço e repositório possuem cobertura extensa de endpoints (repos/issues/PR/releases/notificações/stars), com lacunas de UX para acesso completo.
4. **Terminal interno**: é controlado por allowlist (não é PTY completo/Termux real).
5. **Segurança**: interceptor oficial é `data.auth.AuthInterceptor`; interceptor deprecated de `data.network` foi removido nesta iteração.

## Classificação por dimensão

| Dimensão | Situação atual |
|---|---|
| CODE_REAL | Alto: app com base robusta em Kotlin/Compose/Hilt/JGit/Retrofit. |
| UI_EXPOSTA | Médio: várias capacidades reais estão ocultas/parciais na UI. |
| DOC_DECLARA | Médio/alto: havia sobreposição entre visão e realidade; docs novas separam isso. |
| TESTADO_COM_PROVA | Médio/baixo no ambiente local atual: execução de testes Android bloqueada por ausência de SDK local. |

## Falhas conhecidas (ambiente desta execução)

- `./scripts/gradlew_with_java17.sh testDevDebugUnitTest` falha sem `local.properties`/`ANDROID_HOME`.

## Próximos passos imediatos

1. Completar matrizes com evidências linha-a-linha.
2. Cobrir testes de autenticação/offline/interceptor.
3. Endurecer terminal com classificação de risco em runtime (SAFE/DANGEROUS/BLOCKED).
