# Termux Health Contract — loopback read-only

## Estado

```yaml
client_probe: IMPLEMENTED
tool_router_wiring: IMPLEMENTED
unit_tests: IMPLEMENTED
kotlin_isolated_compile: PASS
local_client_server_loopback: PASS
workflow_execution: TOKEN_VAZIO_STARTUP
termux_server_endpoint: IMPLEMENTED_IN_PR_308
android_device_runtime: TOKEN_VAZIO
mutating_commands: false
```

O tool `termux.health` deixou de ser um handler inexistente e passou a possuir um transporte cliente real, limitado e somente leitura.

A distinção obrigatória continua:

```text
cliente capaz de sondar
!=
servidor disponível em device
!=
runtime saudável em produção
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

## Resposta do servidor Termux PR #308

```json
{
  "schema": "raf.termux-health.v1",
  "status": "ok",
  "runtime": "termux-rafcodephi",
  "abi": "armv7l",
  "pid": 1234,
  "uptime_ms": 42000,
  "capabilities": [
    "health.readonly"
  ],
  "commit": "git-sha-or-TOKEN_VAZIO"
}
```

Somente `health.readonly` é anunciado. Capacidade futura não pode aparecer antes do respectivo handler e teste.

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

A compilação Kotlin isolada encontrou inicialmente a warning `URL(String) is deprecated`. Ela foi corrigida com `URI.toURL()` e a recompilação terminou sem warning.

A ponte local foi executada:

```text
PASS termux-health-end-to-end-local code=200 state=PASS bytes=176
```

Isso é evidência host/local, não execução Android.

## Gates

```yaml
T0_KOTLIN_ISOLATED_COMPILE: PASS
T1_PYTHON_SERVER_SMOKE: PASS
T2_LOOPBACK_INTEGRATION: PASS
T3_GRADLE_JUNIT: TOKEN_VAZIO_STARTUP
T4_ANDROID_ARM32_DEVICE: TOKEN_VAZIO
T5_ANDROID_ARM64_DEVICE: TOKEN_VAZIO
T6_LATENCY_P50_P95_P99: TOKEN_VAZIO
```

## Retroalimentação

```text
F_ok   = cliente, servidor e loopback local executados com fronteira SSRF/read-only
F_gap  = Gradle/JUnit e integração nos devices
F_next = executar em ARM32/ARM64 e decidir lifecycle/autostart
```
