# Knowledge Archives — navegação operacional

Este diretório organiza os materiais conceituais, matemáticos e low-level associados a **RAFAELIA**, **Toro7D**, **Vectras-VM-Android** e arquivos soltos do repositório.

A regra principal é simples: metáfora pode ensinar, mas somente evidência local, teste, build ou prova formal pode promover uma ideia para implementação.

## Mapa de navegação

| Documento | Função | Quando usar |
| --- | --- | --- |
| [`VECTRAS_VM_ANDROID_ARCHIVE.md`](VECTRAS_VM_ANDROID_ARCHIVE.md) | Catálogo expandido das sementes E20, E13 e S11; contrato de invariantes; matriz de arquivos soltos; protocolo de sessão e resposta. | Use para transformar prompts longos, fórmulas e parábolas em backlog técnico auditável. |
| [`../RAFAELIA_INDEX.md`](../RAFAELIA_INDEX.md) | Índice geral do material RAFAELIA já classificado no repositório. | Use para saber o que é produção, parcial, experimental, stub ou arquivo histórico. |
| [`../maths/TORO7D_KNOWLEDGE_CARRIER.md`](../maths/TORO7D_KNOWLEDGE_CARRIER.md) | Tradução técnica de Toro7D para estado, hash, métrica, teste e histórico Git. | Use quando a pergunta central for “o que carrega o conhecimento entendido?”. |
| [`../INCOMING_PROMOTION.md`](../INCOMING_PROMOTION.md) | Critérios para promover materiais de `_incoming/` para docs, scripts, app ou protótipos. | Use antes de mover, copiar ou declarar maturidade de qualquer arquivo experimental. |
| [`../TECHNICAL_CLAIMS_POLICY.md`](../TECHNICAL_CLAIMS_POLICY.md) | Política de alegações técnicas. | Use para evitar chamar stubs, metáforas ou hipóteses de produção. |

## Camadas de organização

1. **Arquivo bruto**: texto, fórmula, pacote, código experimental ou parábola preservada sem alteração de sentido.
2. **Catálogo**: identificação, origem, status, risco, evidência e comando de validação quando existir.
3. **Expansão controlada**: tradução do conceito para requisitos, dados, limites, testes e rollback.
4. **Promoção**: mudança para docs, scripts, app ou módulo nativo somente depois de evidência mínima.
5. **Navegação**: links internos, índice por tema, matriz de decisão e próximos passos.

## Vocabulário de maturidade

| Rótulo | Significado |
| --- | --- |
| `metáfora didática` | Parábola, analogia ou imagem mental útil para explicar uma ideia. Não é prova. |
| `hipótese` | Ideia que pode virar teste, benchmark ou especificação, mas ainda não foi validada. |
| `contrato` | Regra operacional que pode ser auditada, como determinismo, ausência de heap ou preservação de ABI. |
| `protótipo` | Código ou script executável com escopo limitado e evidência local. |
| `produção` | Parte da trilha principal do app Android com build/teste/documentação alinhados. |

## Regra anti-alucinação para sessões longas

Quando o contexto estiver amplo, ambíguo ou sem tema único, a resposta correta deve priorizar:

```text
não inventar → separar camadas → registrar incerteza → pedir/derivar escopo → propor navegação auditável
```

“Token vazio” é útil nesse sentido: é melhor marcar ausência de fato do que preencher lacunas com afirmações sem evidência.
