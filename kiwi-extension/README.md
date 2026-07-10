# Raf Bridge for Kiwi Browser

This folder is a Manifest V3 unpacked extension for the local RafGitTools bridge.

## Install

1. Build and install RafGitTools.
2. Open the separate **Raf Bridge** launcher entry.
3. Start the bridge and copy its local token.
4. Open Kiwi Browser extension management.
5. Enable developer mode.
6. Load this `kiwi-extension` directory as an unpacked extension.
7. Open the Raf Bridge popup and paste the token.
8. Tap **Testar ponte**.

## Contract

The popup sends only:

```text
request_id
+ action=chat
+ declared intent
+ consent=true
+ data class
+ source=kiwi-extension
+ message
```

Every message requires a new consent checkbox action.

The extension has no route for shell, git push, commits, filesystem writes or hidden automation.

## Optional selected text

`Usar seleção da página` reads only the text currently selected by the user in the active tab. It does not send the text until the user checks consent and taps **Conversar**.

## Local endpoint

```text
http://127.0.0.1:8765
```

The extension cannot connect to LAN or internet model endpoints through its declared host permissions.
