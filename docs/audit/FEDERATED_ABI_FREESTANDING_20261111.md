# RAFAELIA Ω — ABI freestanding federado: contrato e riscos (2026-11-11)

**Estado:** IMPLEMENTED_TESTED_LOCAL_SCOPED / CROSS_REPO_UNVERIFIED / REVIEW_ONLY. **claim_allowed=false**.

## Autoridade e escopo
- START HERE Ω V2.2 (Drive) é roteador; Mapa governa rotas federadas; RafGitTools governa apenas este validador L0; cada produtor governa seus próprios builds e ABI.
- Contrato: ../.. / contracts/federated-freestanding-review-gate.v1.json (relação: IMPLEMENTS_CONTRACT).
- Origem nova: native/raf_gate_l0_v1.c, com teste tests/raf_gate_l0_v1_host_test.c e comando sh scripts/verify_freestanding_gate_v1.sh.
- Este gate **não** tem ligação automática com Termux, Vectra, Firebird ou Zabbix; integração é trabalho separado de produtores e consumidores.

## O que foi realizado (N0)
- Compilação C11 freestanding sem cabeçalhos, libc, alocação, chamadas a sistema ou APIs Android **no código-fonte do módulo**.
- Dois testes host, GCC 14.2.0 e Clang 17.0.0: sucesso para estados sintéticos positivos e negativos; zero não se converte em PASS.
- Objetos ARMv7 ELF32, AArch64 ELF64 e x86_64 ELF64 gerados como ET_REL com zero símbolos indefinidos por nm -u; nenhuma linkagem/execução ARM foi feita.
- Os Git blob SHA-1 do código, teste e script, relidos da branch, coincidem com os hashes computados localmente sobre os mesmos bytes testados.
- SHA-256 local do C (sucessor 1.0.1): b12dca84587a5b3c7eb7ae7df03dcda4e8c21defebc1ab01adb30bcd34c76452.
- Hash de blob Git do C (sucessor 1.0.1): ebf0716f8e941f59dc41df187a3a92c4b99428f7.
- CI exact-head: NOT_RUN; binário linkado: NOT_RUN; Android/VM/SQL em dispositivo: NOT_RUN. A ausência de símbolos externos em ET_REL não prova ausência de toda instrução privilegiada ou independência do sistema em tempo de execução.

## Semântica de bloqueio (contrato de cinco palavras unsigned int de 32 bits)
Cada campo (source, artifact, execution, evidence, authority) aceita: 0=UNSET, 1=VERIFIED_IN_OWN_SCOPE, 2=FAIL, 3=NOT_RUN, 4=TOKEN_VAZIO. UNSET=0 é código de status, não o dado numérico zero; o dado pertence a tipo/campo separado. Qualquer valor diferente de 1 bloqueia sua dimensão; valor arbitrário 999 também bloqueia. Bits: 1,2,4,8,16. Ponteiro nulo retorna 0x80000000. Valor retornado 0 apenas habilita **revisão independente**, não prova que o campo foi atestado nem que o produto funciona. Os testes usam entradas simuladas, não observações reais autenticadas.

## Roteamento dos produtores observados
| Urgência | Produtor | Fronteira | Lacuna principal |
|---|---|---|---|
| P0 | termux-app-rafacodephi | Android/provider; bootstrap/APT + pacote/ABI | execução real pkg/apt e identidade do APK TOKEN_VAZIO; sem alterar master |
| P0 | RafGitTools/Mapa | enforcement CI/branch | prova negativa de bloqueio do merge: NOT_VERIFIED |
| P1 | Vectras-VM-Android | consumidor de VM/QEMU do provider Termux | dispatch ≠ execução QEMU ≠ boot do guest |
| P1 | Rafcode-FireBird | núcleo autoral e adaptador futuro, separado do engine | teste engine e banco físico NOT_RUN |
| P1 | firebird_APK_sever_admin | protótipo L0/L1 independente | não é servidor Firebird nem APK; licença formal exige revisão |
| P1 | Zabbix | monitoramento mencionado | repositório produtor e proveniência TOKEN_VAZIO |
| P2 | actions/ndk/jdk | ferramentas de compilação | versões, pinning e compatibilidade por produtor ainda não auditados aqui |

## Licenças — nunca se apropriar do upstream
- **Vectras-VM-Android:** LICENSE apresenta GPL-2.0 e mantém atribuição ao criador xoureldeen; modificações do fork não convertem o upstream em obra integralmente autoral.
- **termux-app-rafacodephi:** COPYING apresenta GPL-3.0; termos upstream permanecem aplicáveis aos derivados.
- **RafGitTools:** LICENSE estabelece GPL-3.0-ou-posterior; o novo L0 e o verificador declaram SPDX GPL-3.0-or-later.
- **Rafcode-FireBird:** LICENSES/SCOPES.md atribui MIT para NUCLEOS e auditorias originais, CC BY 4.0 para documentação indicada e preserva separadamente o Firebird upstream IPL/IDPL.
- **Firebird oficial:** IPL/IDPL permanecem nas partes correspondentes; consultar https://firebirdsql.org/en/licensing/; uma arquitetura com duas licenças por componente não implica permissão para relicenciar o motor.
- **firebird_APK_sever_admin:** README contém aviso de restrição comercial; a licença formal para novos componentes é TOKEN_VAZIO e não é presumida.
- **Zabbix:** licença do repositório específico não verificada; não importar código.

## Compilação, comentários e YAML
- Freestanding é propriedade definida **do programa C e seu ambiente contratado**, não do documento YAML. Actions, shell, compilador, assembler e linker são ferramentas externas de build, mesmo que o objeto L0 seja independente de libc.
- Flags, ABI, ISA, ordem das unidades, pragmas e atributos podem mudar o objeto e precisam de medição com mesmo compilador/versão/alvo. Comentários e formatação comuns não são instruções de otimização; podem afetar tokenização, diretivas ou macros em casos específicos. Nenhum ganho por alteração de formatação foi medido.
- Não usar ASM inline, hex ou assembly de máquina por hipótese de velocidade: exigir prova de equivalência, estabilidade de ABI, segurança, medição e fallback para ARM32/ARM64. Void é um tipo C, não uma instrução de otimização.
- Nenhum YML do usuário foi alterado neste delta: adicionar jobs sem CI/ruleset comprovados poderia aumentar filas, custo e risco; o script de verificação é opt-in e não cria execução agendada.

## 6Σ / falsificação / rollback
- Define: bloquear promoção de cinco fronteiras ausentes.
- Measure: repositórios/ref e ferramenta, inputs sintéticos, três ELF e hashes.
- Analyze: zero, ausente, falha e NOT_RUN precisam permanecer distinguíveis; evidência autenticada é fronteira externa.
- Improve: um módulo C L0 sem dependências de runtime e um verificador hospedado de uso manual.
- Control: entradas nulas, incompletas, falhas, não executadas e inválidas; compilação Clang/GCC e ELF ARM32/ARM64; CI/provider/device permanecem bloqueados.
- **Falsificador externo:** adulterar qualquer fronteira para 0/2/3/4/999; a implementação deve retornar máscara não zero, e nunca publicar CLAIM_ALLOWED.
- **Rollback:** não mesclar a branch; correção por commit sucessor, nunca force-push, apagar histórico ou alterar rulesets.

R3=<F_ok:compilação e testes locais com blob readback, F_gap:links/CI/execução real/licenças pendentes, F_next:exact-head CI e confirmação independente de produtor+consumidor>.

## Correção sucessora append-only — contrato 1.0.1
O predecessor 1.0.0 utilizou 0=UNKNOWN no texto do contrato. Isto confundia o estado desconhecido com um valor numérico zero, incompatível com o princípio TOKEN_VAZIO != 0. A correção separa UNSET=0 (código de campo de status) de TOKEN_VAZIO=4 (ausência tipada), ambos bloqueantes, sem converter um dado zero em ausência. Os commits predecessores seguem no histórico. C e teste atualizados e reexecutados no host com os três objetos ABI compilados, com blob SHA confirmado. Nenhum CI/provider/device novo foi testado.

## Sucessor 1.0.2 — risco ISA e bloqueio de dependência (G0 local)
Após a versão 1.0.1, a desassemblagem da compilação ARMv7 padrão mostrou instruções NEON. Como NEON pode ser opcional em implementações ARMv7-A, o adaptador de build **para objeto portátil** agora fixa `-mfloat-abi=soft -mfpu=none`. Medição local com Clang 17.0.0 e `-Oz`: seção `.text` ARMv7 passou de 88 para 68 bytes. Não há conclusão sobre velocidade, consumo ou APK. AArch64 mediu 72 bytes de `.text`; x86_64 mediu 70 bytes, nos mesmos alvos testados localmente. O teste sem Clang no PATH retornou 127 com `gate=BLOCKED missing_host_tool=clang`. Verificador hospedado: SHA-256 32f0d79aaf75a4df6a21c418679abb41bcc103fcfca0d6d7430b2bcd6226a48a, Git blob SHA-1 64c8e3a9d225a01a2e1e0fc043812e54f57234f1. O histórico anterior não foi apagado. Gate de CI exact-head e execução ARM física continuam NOT_RUN.
