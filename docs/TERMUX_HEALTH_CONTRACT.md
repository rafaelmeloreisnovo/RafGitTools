# Termux Health Contract — loopback read-only

## Estado

```yaml
client_probe: IMPLEMENTED
tool_router_wiring: IMPLEMENTED
unit_tests: IMPLEMENTED
workflow_execution: TOKEN_VAZIO
termux_server_endpoint: TOKEN_VAZIO
android_device_runtime: TOKEN_VAZIO
mutating_commands: false
```

O tool `termux.health` deixou de ser um handler inexistente e passou a possuir um transporte cliente real, limitado e somente leitura.

Isso não significa que o servidor Termux já expõe `/health`. A distinção obrigatória é:

```text
cliente capaz de sondar
!=
servidor disponível
!=
runtime saudável
```

## Fluxo

```text
kernel/tool call
→ GovernanceGate
→ ToolRouter
→ TermuxHealthProbe
→ HTTP GET loopback
→ PASS | FAIL | TOKEN_VAZIO | ERROR
```

O registro `app/src/main/assets/kernel/protocol/tool_registry.json` já autoriza `termux.health` sem autenticação porque a operação é local, read-only e não retorna segredos por desenho.

## Endpoint padrão

```text
http://127.0.0.1:8765/health
```

Também são aceitos:

- `http://localhost:<porta>/health`;
- `http://[::1]:<porta>/health`;
- caminho `/v1/health`.

## Fronteiras de segurança

O probe bloqueia:

- HTTPS ou outros esquemas;
- host não loopback;
- portas abaixo de 1024;
- usuário/senha na URI;
- query string;
- fragmento;
- caminhos diferentes de `/health` e `/v1/health`;
- redirects;
- upload/body de requisição;
- resposta acima de 4096 bytes;
- timeout acima de 1000 ms.

Consequência:

```text
endpoint fornecido pelo modelo
→ validação local estrita
→ nenhuma SSRF para rede externa
```

## Estados

| Estado | Significado |
|---|---|
| `PASS` | servidor respondeu HTTP 2xx dentro do contrato |
| `FAIL` | servidor respondeu, mas com status não 2xx |
| `TOKEN_VAZIO` | conexão/transport não produziu evidência; Termux não é declarado quebrado |
| `ERROR` | endpoint inválido ou transporte negado pela plataforma |

Um `connection refused` vira:

```json
{
  "tool": "termux.health",
  "status": "TOKEN_VAZIO",
  "reason": "runtime_unreachable:ConnectException"
}
```

Isso preserva a regra:

```text
ausência de resposta != falha comprovada do runtime
```

## Resposta esperada do servidor futuro

O cliente aceita qualquer corpo limitado, mas o servidor recomendado deve produzir:

```json
{
  "schema": "raf.termux-health.v1",
  "status": "ok",
  "runtime": "termux-rafcodephi",
  "abi": "armv7l",
  "pid": 1234,
  "uptime_ms": 42000,
  "capabilities": [
    "job.submit.readonly",
    "artifact.inspect",
    "rafpolimata.status"
  ],
  "commit": "git-sha-or-TOKEN_VAZIO"
}
```

Nenhum token, caminho privado, variável de ambiente ou credencial deve atravessar esse endpoint.

## Testes implementados

`TermuxHealthProbeTest` cobre:

1. loopback IPv4, hostname e IPv6;
2. bloqueio de host externo, HTTPS, query, userinfo, porta privilegiada e path arbitrário;
3. HTTP 200 → `PASS`;
4. HTTP 503 → `FAIL`;
5. IOException → `TOKEN_VAZIO`;
6. bloqueio antes de abrir transporte;
7. limite de corpo em 4096 bytes;
8. GET, sem redirect e sem output.

## Próximo lado da ponte

O próximo PR deve ficar no `termux-app-rafacodephi` e implementar somente:

```text
GET /health
→ snapshot sanitizado
→ JSON determinístico
→ nenhum comando mutável
```

Gates:

```yaml
T0_CLIENT_UNIT_TESTS: TOKEN_VAZIO_UNTIL_RUN
T1_SERVER_CONTRACT: TOKEN_VAZIO
T2_LOOPBACK_INTEGRATION: TOKEN_VAZIO
T3_ANDROID_ARM32_DEVICE: TOKEN_VAZIO
T4_ANDROID_ARM64_DEVICE: TOKEN_VAZIO
T5_LATENCY_P50_P95_P99: TOKEN_VAZIO
```

## Retroalimentação

```text
F_ok   = cliente real, governança, SSRF boundary e testes implementados
F_gap  = servidor Termux e integração em device
F_next = endpoint /health read-only no Termux, depois teste ponta a ponta
```
