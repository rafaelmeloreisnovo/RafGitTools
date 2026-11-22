# Laboratório federado de campo RAFAELIA Ω — etapa V1 (não promover)

## Escopo constatado
Esta etapa implementa em **RafGitTools** um plano de composição com apontadores imutáveis, testes negativos, CI exact-head e recibo sanitizado. **Não é** um agente com permissão para modificar todos os repos, executar código remoto, expor o corpus privado ou controlar segredos. O roteamento e a autoridade epistemológica federada permanecem no Mapa; o RafGitTools implementa o controle; o RafPolimata produz artefatos de compilação sob seu próprio contrato.

| Papel | Superfície | Fronteira |
| --- | --- | --- |
| Referência/estado | Mapa | indexação, provas e autorizações, nunca herdar PASS |
| Coordenação | RafGitTools | controle, recibos, gates e rollback |
| Compilador/laboratório | RafPolimata | freestanding L0/ELF e ApkC candidato |
| Dispositivo | Termux e Vectras | execução Android física ainda pendente |
| Áudio | EstudioAudio | laboratório independente, sem copy/transposição |
| Privado | registro `configs/private-ci/execution-registry.v1.json` | somente ponte existente sob disparo controlado; nenhum segredo neste contrato |

### Engenharia de limites
- **L0 freestanding**: núcleo C/ASM separado, sem libc, heap ou APIs do Android quando provado por auditoria de objeto; objeto ARMv7 não é executável ARM64 e vice-versa.
- **L1 toolchain**: GitHub Actions, Python, Clang, linker, ApkC, QEMU e ZIP/APK são ferramentas hospedeiras, não componentes mágicos sem dependências.
- **L2 APK**: o formato APK/manifesto/assinatura/instalação é contrato do Android. Compilar um APK dual-ABI em CI não prova instalação, interface de usuário ou segurança de login.
- **L3 dispositivo**: comprovantes reais por mesmo SHA, assinatura, instalação, launch/dlopen, acessibilidade, privacidade e observação independente ainda são `TOKEN_VAZIO`.

### Segurança de secrets (capabilities, não valores)
O runner disparado por PR é **secretless**, `permissions.contents=read`, `persist-credentials=false` e não executa outros repos. As entradas `PAT_ACTIONS`, `PAT_AGENTS`, `PAT_ENV` e `PAT_ENVIRONMENTS` são identidades de capacidade existentes, não tokens copiados.

- `PAT_ACTIONS` **não** passa para a nova etapa: a ponte preexistente `START.yml` + `scripts/private_ci_bridge.py` define uma separação de checkout/execução de código privado, com SHA exato, allowlist e recibo hash-only. Requer autorização e ambiente confiável próprios.
- `PAT_AGENTS` não é promoção implícita de poderes de administrador, gestão de chaves ou instalação de aplicativos: permanece `REGISTERED_NOT_WIRED` até readback e decisão humana.
- `PAT_ENV` é distinguido de `PAT_ENVIRONMENTS`. Mutações do provedor, branch protection, regras e segredos exigem preflight, ambiente protegido, revisão humana e readback. **Criar segredos automaticamente ou copiá-los entre repos não é permitido por este pacote**.
- Repositórios privados não são inferidos como públicos; o contrato do laboratório não contém seu SHA, URL privada, nome de arquivo, fonte, stdout/stderr ou payload. Nada desse conteúdo deve ser enviado a artefatos públicos.

### Governança e normas como referências
- **DMAIC/Six Sigma**, sem alegação de certificação: Define (papéis e finalidade) → Measure (SHA de entrada, dados mínimos e estado dos checks) → Analyze (falsificadores independentes e contradições) → Improve (hotfix no escopo mínimo) → Control (CI, recibo, revisão e rollback).
- **ISO 8000, ISO 9001, ISO/IEC 27001/27002 e NIST**: mapas de referência para qualidade dos dados, melhoria contínua, privacidade, gestão de segurança, desenvolvimento seguro e registro de riscos; nenhum selo de conformidade é alegado.
- **Proteção infantil/dignidade humana**: nenhuma coleta identificável de crianças, inferência de identidade ou corpus sensível na etapa pública; risco desconhecido ou ausência de consentimento interrompe publicação e transferência. Dignidade, privacidade, licenças e segurança são fatores não compensatórios.

### Falsificadores
O teste deve falhar com promoção/merge/release/dispatch automático, troca ou fallback entre PATs, SHA dinâmico, execução remota, referência privada legível, exfiltração, supressão de fonte/licença, desligamento de proteção infantil ou dignidade, ID duplicado e mudança de autoridade.

**Receipts:** `artifacts/federated-field-lab/receipt.json` é gerado no runner e enviado como artefato sanitizado. Ele não contém dados privados, segredos, código nem logs de repositórios externos. `PASS_SCOPED_CONTRACT_ONLY` não equivale a autorização de uma rota federada nem de APK.

**Rollback:** um revert do branch/PR Draft; default branch não alterado. Mudança em cada produtor requer PR próprio, revisão e recebimento por SHA exato.

**R3:** `F_ok` = fonte do contrato, teste e CI propostos; `F_gap` = execução CI exact-head até ela ocorrer, poderes do provedor, private CI físico, dispositivo/assinatura e verificação independente; `F_next` = vincular recibo explícito do ApkC no RafPolimata, preservar o controle fail-closed e solicitar revisão antes de qualquer promoção.

`claim_allowed=false · no_merge=true · no_release=true`.
