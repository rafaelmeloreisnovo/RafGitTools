# RAFAELIA · Session Queue L0 freestanding — revisão cirúrgica V1

**Estado:** IMPLEMENTED_SOURCE + LOCAL_NATIVE_AND_CROSS_COMPILE_SCOPED.
**Gate:** `claim_allowed=false`, `dispatch_enabled=false`, PR #707 DRAFT/HOLD.
**Repositório autoridade:** RafGitTools. **Roteamento:** Mapa, somente por pin exato.
**Escopo de mudança:** dois arquivos C freestanding + um cabeçalho; nenhum original substituído.

## Fronteira obrigatória

`scripts/freestanding/session_queue_core_v1.h` — ABI de buffers tipados,
emprestados ao chamador. `session_queue_core_v1.c` — núcleo de decisão
determinístico. `test_session_queue_core_v1.c` — testes freestanding
com retorno numérico, sem I/O, libc, alocação, syscalls nem `main`.

A aplicação/Google Drive/GitHub/API/JSON parser permanecem explicitamente
**fora do núcleo**. Não há parsing JSON em L0 e nenhum adaptador novo foi
conectado. Campo `boolean_types_verified` **não autentica a entrada**;
só pode ser informado por um adaptador que comprovou tipo booleano exato,
chaves não duplicadas, quinze campos, tamanho máximo e sem payload privado.
Até existir e ser testado, a passagem L1->L0 fica bloqueada.

Não ativar `execução`, `workflow_dispatch`, CI privada, nem promoção.
Não converter `SHA-256` em segredo: checksum de idempotência não é MAC,
assinatura, autorização, criptografia, confidencialidade ou antifraude.

## Preservação do contrato anterior

O Python legado em `scripts/validate_session_queue_preflight_v1.py`
permanece **inalterado**, apenas como referência para comparação e
rollback. Nenhuma autorização de novos caminhos de execução. Kernel L0
reproduz o SHA-256 do esquema V1 para os 8 campos de identidade imutável:
`schema,envelope_id,session_id,target_id,packet_id,mapa_commit,mapa_blob,operation`.
Status e classificação não alteram a chave. Pin do Mapa fixo
`ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09`,
blob `c1d1d8f1692c74957f8b70968fa863f855d93cb0`.

O código usa rotinas de SHA-256 conforme a especificação pública FIPS 180-4.
Essa implementação própria deve continuar recebendo vetores e auditoria
independente; sua presença não aprova nenhum uso criptográfico para
confidencialidade, autenticação ou assinatura.

## Evidência e comandos reproduzíveis

Compilar **somente** os objetos L0, sem link com libc:

```sh
clang -target armv7a-none-eabi -std=c11 -O1 -Wall -Wextra -Werror \
  -ffreestanding -fno-builtin -fno-stack-protector -c \
  scripts/freestanding/session_queue_core_v1.c -o /tmp/rafq_armv7.o
clang -target aarch64-none-elf -std=c11 -O1 -Wall -Wextra -Werror \
  -ffreestanding -fno-builtin -fno-stack-protector -c \
  scripts/freestanding/session_queue_core_v1.c -o /tmp/rafq_aarch64.o
nm -u /tmp/rafq_armv7.o
nm -u /tmp/rafq_aarch64.o
```

Testar a rotina freestanding por meio de *um chamador hosted descartável*
que invoca `raf_queue_selftest_v1()` e observa zero falhas. Esse chamador
não integra a biblioteca, APK, Action ou contrato do L0. A suíte do repositório
compila sem cabeçalhos, dependências ou chamadas hosted.

**Observações locais exatas desta alteração:**

- x86-64: 22 verificações internas / 0 falhas.
- AddressSanitizer + UndefinedBehaviorSanitizer: 22/22 sem relatos.
- 512 identidades sintéticas válidas com SHA-256 canônico do Python de referência: 512/512.
- 7 mutações por identidade: 3.584/3.584 bloqueadas.
- Build objetos ELF x86-64 / ARMv7 EABI5 / AArch64: sucesso.
- `nm -u` dos três núcleos: nenhum símbolo externo.
- **Não houve runtime físico em ARM32 ou ARM64**, nem job Python da CI dedicado ao núcleo.
- **Não houve integração L1->L0 ou ativação de agendamento privado**.
- sha256 header `c0404146a47151bb2c04c8afbb722133ee793c72dcf8e9d88622a82085729ac4`
- sha256 core `3f7ea942f8ed7780d7cc640bcd4e4b3f4c650ce5a600e83f7f30a262f0254bdf`
- sha256 freestanding selftest `62a057c06f40356c23607485522aead71e7d49062bed797136cca33695912de7`

Hashes servem à integridade de conteúdo, não comprovam comportamento ou
revisão de segurança por terceiros. Os testes diferenciais externos foram
feitos como instrumentação local, não como código freestanding do produto.

## Modos de falha, rollback, F_ok/F_gap/F_next

`PARSE_TYPE_UNVERIFIED` -> HOLD; `SCHEMA/PIN/OPAQUE_ID/PRIVACY`
incompatíveis -> HOLD; `EXECUTION/CLAIM` habilitados -> HOLD;
`IDEMPOTENCY` inválido -> HOLD; estados PROCESSING/PROCESSED -> HOLD;
ponte privada ou chave/credencial desconhecida -> HOLD.

Rollback: fechar o PR Draft sem merge ou reverter exclusivamente os três
arquivos novos e este documento em PR revisado; não apagar originais,
dados privados, evidências nem autoria. Nenhuma alteração em
`START.yml`, Actions, `main`, PATs ou proteções.

**F_ok:** objetos freestanding e suíte freestanding implementados, testados
no host e compilados cruzadamente; checksum diferencial local.
**F_gap:** parser/adaptador tipado e sua segurança; canário no runner remoto,
execução física ARM; validação independente; provas de ABI e de
não regressão integradas à CI; permission readback; crypto de transporte.
**F_next:** executar essa mesma suíte em gate CI dedicado ao HEAD exato,
realizar readback do SHA/ABI no runner; implementar parser/adapter
freestanding separado com teste de tipos/duplicatas; canário privado só
depois de autorização, isolamento, zero-trust e auditoria independentes.
