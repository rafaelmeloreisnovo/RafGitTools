# Política de Alegações Técnicas

Esta política define como o repositório RafGitTools deve descrever recursos, documentação, experimentos e entregas técnicas sem exagerar o estado real do código.

## Objetivo

Evitar alegações técnicas sem evidência verificável. Toda afirmação de maturidade deve apontar para:

1. um caminho de código, documentação ou configuração que implemente o comportamento descrito; e
2. um teste, validação manual documentada ou execução de CI correspondente.

Se qualquer um desses dois elementos estiver ausente, a alegação deve usar um label de maturidade mais conservador.

## Termos controlados

Não usar os termos abaixo como afirmação de estado real sem caminho de código e teste correspondente:

- `funcional`
- `enterprise`
- `produção`
- `seguro`
- `otimizado`
- `validado`

Quando esses termos forem necessários, a frase deve indicar a evidência mínima:

- caminho de código: arquivo, módulo, script, fluxo Gradle ou workflow relacionado;
- teste correspondente: unit test, lint, build local, verificação manual registrada, validação de APK ou CI;
- escopo: variante, ABI, ambiente, feature flag, limitação conhecida e data da validação quando relevante.

### Exemplos de redação permitida

- Permitido: `validado localmente em ./scripts/gradlew_with_java17.sh testDevDebugUnitTest para o módulo X`.
- Permitido: `protótipo de fluxo enterprise; sem validação de produção`.
- Permitido: `stub de GPG; interface presente, sem assinatura criptográfica integrada`.
- Evitar: `GPG funcional em produção` sem código e teste que provem esse estado.
- Evitar: `segurança enterprise validada` sem ameaça, controle, teste e evidência de auditoria.

## Labels padronizados

Use exatamente estes labels para marcar maturidade técnica:

| Label | Uso obrigatório |
|---|---|
| `conceito` | Ideia, modelo mental, proposta ou arquitetura sem implementação executável. |
| `parábola` | Metáfora, analogia, reflexão conceitual ou explicação narrativa que não deve ser lida como prova técnica. |
| `hipótese` | Suposição técnica ainda não demonstrada por código e teste. |
| `protótipo` | Implementação exploratória executável, com limites conhecidos e sem contrato estável. |
| `stub` | Casca, placeholder, interface, tela, rota ou documentação sem implementação completa do comportamento prometido. |
| `parcial` | Implementação real com cobertura limitada, lacunas conhecidas ou suporte incompleto. |
| `validado localmente` | Implementação executada e verificada em ambiente local com comando, data ou evidência documentada. |
| `validado em CI` | Implementação verificada por workflow de CI reproduzível e rastreável. |

## Escopo de aplicação

Esta política se aplica a qualquer texto que descreva capacidade técnica do RafGitTools, incluindo:

- `README.md` e demais READMEs do repositório;
- roadmaps e matrizes de status em `docs/`;
- documentos RAFAELIA;
- `_incoming/`;
- `Livro/`;
- comentários de release, changelogs, notas de versão e descrições de PR;
- issues de planejamento, checklist de entrega e relatórios de auditoria.

## Regras operacionais

1. Antes de promover um item para `funcional`, `produção`, `seguro`, `otimizado` ou `validado`, registrar o caminho de código e o teste correspondente.
2. Quando a evidência existir apenas fora de CI, usar `validado localmente`, não `validado em CI`.
3. Quando existir código parcial, mas sem cobertura suficiente, usar `parcial` e listar lacunas.
4. Quando existir apenas interface, documentação ou placeholder, usar `stub`.
5. Quando houver linguagem metafórica, multidisciplinar ou especulativa, marcar como `conceito`, `parábola` ou `hipótese` conforme o caso.
6. Não transformar documentação aspiracional em promessa de entrega sem issue, caminho de implementação e critério de aceite.
7. Não declarar suporte a ambiente, ABI, SDK, release signing ou distribuição sem build ou validação correspondente.
8. Comentários de release devem separar claramente: entregue, parcial, stub, experimental, risco conhecido e validação executada.

## Exemplos obrigatórios do repositório

### GPG, LFS, worktree e webhook

GPG, LFS, worktree e webhook devem ser descritos como `stub` enquanto não houver implementação completa, integração real e teste correspondente. É proibido descrevê-los como `funcional`, `produção`, `seguro`, `enterprise` ou `validado` sem evidência rastreável.

Exemplo recomendado:

```text
GPG/LFS/worktree/webhook: stub. Há estrutura planejada ou placeholder, mas a funcionalidade completa ainda não está validada por código e teste correspondentes.
```

### `_incoming/`

O diretório `_incoming/` deve ser tratado como `experimental` no texto comum e classificado com labels técnicos conservadores como `conceito`, `hipótese`, `protótipo` ou `stub`, conforme o conteúdo. O material em `_incoming/` não deve ser apresentado como contrato estável do aplicativo sem revisão, integração e validação.

Exemplo recomendado:

```text
_incoming/: área experimental para triagem. Conteúdo não promovido para documentação oficial sem revisão técnica e label de maturidade.
```

### ARM32/Termux

ARM32/Termux deve ser descrito como runtime/toolchain validation, não como build host canônico de APK. Não afirmar suporte completo de build Android dentro de Termux ARM32 sem prova explícita e reproduzível.

Exemplo recomendado:

```text
ARM32/Termux: validado apenas como ambiente de runtime/toolchain validation quando o script correspondente passa. O build host canônico de APK permanece desktop/CI com JDK 17 e Android SDK compatível.
```

## Checklist para novas alegações

Antes de adicionar ou alterar uma alegação técnica, confirmar:

- [ ] Qual label padronizado descreve o estado real?
- [ ] Existe caminho de código ou configuração?
- [ ] Existe teste local, validação manual registrada ou CI?
- [ ] A frase separa fato, hipótese, metáfora e plano?
- [ ] O texto evita prometer produção, enterprise, segurança, otimização ou validação sem prova?
- [ ] O escopo informa ambiente, variante, ABI ou limitação relevante?

## Modelo de anotação recomendado

```text
[parcial] Recurso X: implementado em path/to/file.kt, coberto por path/to/fileTest.kt.
Validação: ./scripts/gradlew_with_java17.sh testDevDebugUnitTest.
Limite: sem validação em CI para release production.
```

```text
[parábola] Texto conceitual RAFAELIA usado como metáfora de arquitetura. Não é prova matemática, criptográfica, médica, financeira ou de segurança.
```

## Política de correção

Quando uma alegação antiga violar esta política:

1. rebaixar o label para o estado comprovado;
2. adicionar lacunas conhecidas;
3. apontar o teste ausente ou o caminho de implementação pendente;
4. abrir ou referenciar issue/roadmap se a funcionalidade continuar planejada.

A correção de linguagem deve preservar funcionalidades existentes e não deve apagar histórico técnico relevante; deve apenas tornar o estado real verificável.
