# Raf Kiwi Local Bridge

Status: implementation v0.1

## Purpose

Create a low-friction local path:

```text
Kiwi Browser extension
        -> 127.0.0.1:8765
RafGitTools Java bridge
        -> 127.0.0.1:8080/v1/chat/completions
llamaRafaelia / llama.cpp
```

The bridge is conversational. It is not an autonomous executor.

## Backend decision

The default backend is **llamaRafaelia served by a llama.cpp-compatible HTTP server**.

Reason:

- it already fits instruction/chat behavior;
- it can expose an OpenAI-compatible local endpoint;
- the APK needs only one direct HTTP call;
- the model remains local;
- no cloud API key is required.

NanoGPT remains valuable as a training and architecture laboratory, but it is not the default runtime for this bridge. It may be used later only if wrapped behind the same local chat endpoint.

## Direct Java implementation

```text
app/src/main/java/com/rafgittools/bridge/
├── RafBridgeActivity.java
├── RafBridgeContract.java
├── RafBridgePrefs.java
├── RafBridgeService.java
└── RafModelClient.java
```

There is no DI layer, repository pattern, reflection, plugin registry, or generic command bus in this path.

## Protocol

### Health

```http
GET http://127.0.0.1:8765/health
```

### Conversation

```http
POST http://127.0.0.1:8765/v1/chat
X-Raf-Token: <local pairing token>
Content-Type: application/json
```

```json
{
  "request_id": "uuid",
  "action": "chat",
  "intent": "conversa natural",
  "consent": true,
  "data_class": "private",
  "source": "kiwi-extension",
  "message": "texto autorizado pelo usuário"
}
```

Response:

```json
{
  "ok": true,
  "request_id": "uuid",
  "reply": "resposta do modelo local",
  "executed_external_action": false,
  "retained_message": false
}
```

## Moral contract

The bridge enforces these invariants:

1. loopback only;
2. explicit pairing token;
3. explicit consent for every message;
4. declared intent;
5. declared data class;
6. sensitive content disabled by default;
7. known credential patterns rejected;
8. maximum message size;
9. no conversation persistence;
10. no shell, git write, file write, purchase, send, or hidden automation route.

```text
conversation != command execution
processing != retention
local endpoint != permission to exfiltrate
consent once != consent forever
```

## APK usage

1. Open the **Raf Bridge** launcher entry.
2. Keep the default endpoint or enter another loopback endpoint.
3. Set the local model name.
4. Leave sensitive content disabled unless a conscious session requires it.
5. Tap **Salvar e iniciar ponte**.
6. Copy the generated token into the Kiwi extension.

The activity is also reachable through:

```text
rafgittools://bridge
```

## Kiwi extension usage

The unpacked extension is in:

```text
kiwi-extension/
```

It can:

- store the pairing token in extension-local storage;
- test bridge health;
- send a natural-language message;
- optionally copy only the selected text from the active page;
- require consent before each send;
- display the local model response.

It cannot execute repository operations.

## Local model example

A llama.cpp-compatible server must listen only on loopback and expose:

```text
http://127.0.0.1:8080/v1/chat/completions
```

Conceptual launch shape:

```bash
llama-server \
  --host 127.0.0.1 \
  --port 8080 \
  --alias llama-rafaelia \
  -m /caminho/modelo.gguf
```

Exact flags must match the installed llama.cpp build.

## Threat boundaries

The implementation reduces risk but does not claim absolute security.

- A rooted or compromised Android device can expose private application state.
- A malicious extension with broad permissions can read browser content.
- A local model can still produce unsafe or incorrect text.
- Cleartext is accepted only on loopback; remote HTTP remains blocked.
- The token should be rotated after device sharing, debugging, or suspected exposure.

## Next engineering gate

Before calling the bridge production-ready:

1. compile all Android variants;
2. install on the target Android device;
3. run a local llama.cpp server;
4. load the extension in Kiwi;
5. test health, valid chat, invalid token, missing consent, sensitive block, credential block, oversized body, model timeout and service restart;
6. record the APK hash and test log.
