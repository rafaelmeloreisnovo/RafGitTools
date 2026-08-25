# Runbook de Excelência Operacional

## Fluxo read-only inicial

1. selecionar fonte;
2. apresentar identidade, tamanho, origem e política;
3. resolver capacidade;
4. executar health probe;
5. gerar `JobEnvelope` limitado;
6. solicitar aprovação quando necessária;
7. enviar ao runtime;
8. acompanhar eventos;
9. receber artefatos e evidência;
10. classificar resultado;
11. registrar `F_ok`, `F_gap` e `F_next`.

## Antes de executar

- confirmar que a capacidade não é apenas declarada;
- confirmar executor e versão;
- validar schemas;
- calcular impacto de privacidade;
- verificar espaço, bateria, rede e memória;
- declarar timeout e cancelamento;
- impedir operação destrutiva por padrão;
- registrar consentimento ou autorização.

## Durante

- mostrar progresso baseado em bytes/registros reais;
- persistir checkpoint;
- aplicar backpressure;
- não bloquear UI com trabalho pesado;
- redigir secrets e dados privados em logs;
- separar warning, error, status e estado epistêmico;
- permitir cancelamento seguro.

## Depois

- verificar saída contra critérios;
- vincular hashes e versão do executor;
- apresentar limitações;
- preservar resultados negativos;
- sincronizar somente artefatos autorizados;
- registrar rollback ou limpeza;
- nunca converter `TOKEN_VAZIO` em sucesso visual.

## Incidentes

### Executor indisponível

Estado: `BLOCKED_EXECUTOR`.

Ação: não redirecionar silenciosamente para executor diferente. Apresentar alternativa e suas diferenças.

### Política negada

Estado: `BLOCKED_POLICY`.

Ação: não reduzir a política automaticamente. Solicitar decisão explícita quando permitido.

### Evidência incompleta

Estado: `PASS_LIMITED` ou `TOKEN_VAZIO`.

Ação: conservar artefatos, listar lacunas e produzir próximo teste.

### Saída corrompida

Estado: `FAILED_INTEGRITY`.

Ação: quarentena, preservar logs redigidos, não publicar e oferecer reexecução a partir do último checkpoint válido.

## Métricas úteis

- tempo ponta a ponta;
- bytes lidos e escritos;
- pico de memória;
- taxa de retomada bem-sucedida;
- cache hit validado;
- jobs cancelados com estado seguro;
- proporção de claims com evidência;
- número de promoções bloqueadas corretamente;
- defeitos escapados após gate.

Throughput isolado não mede excelência operacional.
