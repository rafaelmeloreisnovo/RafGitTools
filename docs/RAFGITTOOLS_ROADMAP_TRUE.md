# RAFGITTOOLS_ROADMAP_TRUE

- Status: ATIVO
- Última atualização: 2026-05-27

## Entregue
- PAT login funcional.
- Device Code integrado ao ViewModel/UI com estado pending/polling.
- Importação via gh CLI integrada.
- Modo offline habilitado no Auth e respeitado na Home.

## Parcial / Roadmap
- SSH auth local: **REAL_ATIVO_LOCAL** (requer chave SSH existente; ativa modo offline).
- OAUTH_WEB: **REAL_ATIVO** (inicia device flow com UX de abrir navegador e persistência de método).
- UX completa para operações git destrutivas com confirmação e trilha.

## Próximos incrementos
1. Testes unitários dedicados para offline na Home.
2. Hardening de terminal (SAFE/DANGEROUS/BLOCKED).
3. Cobertura de integração para auth + repository cache.
