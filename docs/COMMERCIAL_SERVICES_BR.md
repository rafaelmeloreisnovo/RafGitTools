# Serviços Comerciais — RafGitTools / RAFAELIA

**Estado:** `COMMERCIAL_CANDIDATE / HUMAN_CONTRACT_REQUIRED`  
**Moeda:** BRL (R$)  
**Base técnica observada:** RafGitTools público, GPL-3.0-or-later, com governança source-first e gates de evidência.  
**Regra:** software livre ≠ serviço gratuito. A GPL continua aplicável ao código coberto; os valores abaixo remuneram trabalho humano de auditoria, integração, hardening, suporte e entrega contratada.

> SOURCE ≠ ARTEFATO ≠ EXECUÇÃO ≠ EVIDÊNCIA ≠ CLAIM  
> Nenhum pacote promete certificação, conformidade legal, ausência de vulnerabilidades, aprovação em loja ou resultado financeiro.

## Oferta de entrada — Auditoria Técnica Expressa

**Preço-piloto:** **R$ 1.490**

Escopo padrão:
- 1 repositório;
- leitura do estado atual + revisão de README/roadmap/gates;
- inventário de build/CI/segurança/proveniência observável;
- separação `SOURCE_OBSERVED / TEST_PROVEN / BUILD_PROVEN / RUNTIME_PROVEN`;
- Gap Atlas priorizado;
- relatório curto com riscos, bloqueadores e 3–5 ações de maior valor;
- receipt final com refs/SHAs observados.

Não inclui:
- pentest intrusivo;
- credenciais de produção;
- parecer jurídico;
- execução em dispositivo físico não fornecido;
- correção ilimitada de código.

## Sprint de Hardening e Evidência

**Preço-piloto:** **R$ 4.900**

Escopo padrão:
- tudo da Auditoria Técnica Expressa;
- branch de trabalho reversível;
- correção de até 3 gaps técnicos de maior valor;
- CI/gates fail-closed onde aplicável;
- cadeia de custódia de artefatos e hashes;
- documentação de rollback/failback;
- PR final com evidência do que foi implementado e do que permanece `TOKEN_VAZIO`.

Condição de aceite:
- o cliente aprova previamente o repositório, escopo e autoridade de escrita;
- `IMPLEMENTED_UNTESTED != PASS`;
- gaps externos não são convertidos artificialmente em sucesso.

## Implantação Assistida — Git/Android/Governança

**Preço-piloto:** **a partir de R$ 9.900**

Indicado para equipes que precisam integrar:
- fluxo Git/GitHub com governança e receipts;
- Android/Gradle/JDK e pipeline de build;
- política de branches/PRs;
- proveniência de artefatos;
- documentação operacional;
- matriz de gates para release.

O valor final depende de número de repositórios, providers, variantes Android, ambiente de CI e necessidade de execução física.

## Suporte especializado

**Hora técnica avulsa:** **R$ 350/h**  
**Bloco de 10 horas:** **R$ 3.000**

Uso típico:
- diagnóstico de build;
- revisão de PR;
- arquitetura de receipts/cadeia de custódia;
- hardening de workflows;
- troubleshooting Git/GitHub/Android;
- documentação técnica source-first.

## Como contratar

1. Abra uma Issue neste repositório com o prefixo **`[COMERCIAL]`** e descreva apenas informações não sensíveis.
2. Informe: objetivo, repositório/projeto, ambiente, prazo desejado e evidência já disponível.
3. Dados privados, segredos, tokens, chaves e credenciais **não devem ser publicados em Issue**.
4. O escopo comercial só começa após concordância humana sobre entrega, preço, autoridade e canal privado adequado.

## Gate de recebimento

`PAYMENT_RAIL = TOKEN_VAZIO_VERIFIED_CHANNEL_REQUIRED`

Nenhum PIX, conta bancária, carteira ou processador de pagamento é inventado neste documento. O canal de cobrança deve ser vinculado pelo titular e confirmado fora do código antes de qualquer pagamento.

## Evidência comercial

Até existir contrato/ordem de serviço/pagamento confirmado:

`LEAD != CONTRACT != PAYMENT != REVENUE`

Portanto:
- preços acima = oferta-piloto;
- receita realizada = `TOKEN_VAZIO`;
- nenhum número de faturamento é declarado sem receipt financeiro verificável.

## Licença e propriedade intelectual

Este documento não altera a licença do RafGitTools nem concede direitos adicionais sobre componentes de terceiros. Serviços podem ser cobrados independentemente da gratuidade do código, respeitando GPL, atribuições, licenças de dependências e contratos específicos.

---

**R3**  
- **F_ok:** oferta comercial concreta em BRL, escopo delimitado e compatível com software GPL.  
- **F_gap:** canal de pagamento verificado + primeiro lead/contrato.  
- **F_next:** publicar a oferta via PR e vincular um canal de recebimento autorizado pelo titular.
