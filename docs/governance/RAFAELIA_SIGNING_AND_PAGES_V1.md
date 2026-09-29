# Rafaelia Signing & Pages Contract V1

## Purpose

RafGitTools acts as a public verification surface for Rafaelia signed artifacts.

It is **not** a private-key store.

## Public variables

```text
RAFAELIA_SIGNER_ID
RAFAELIA_EXPECTED_CERT_SHA256
RAFAELIA_ANDROID_KEY_ALIAS
RAFAELIA_PAGES_REPO
```

## Secrets

```text
RAFAELIA_ANDROID_KEYSTORE_B64
RAFAELIA_ANDROID_STORE_PASSWORD
RAFAELIA_ANDROID_KEY_PASSWORD
RAFAELIA_PAGES_PAT
```

Secret values are intentionally `TOKEN_VAZIO` in repository source.

The connected GitHub tool available to this execution does not expose a mutation API for repository Secrets/Variables, therefore no secret or account-level variable value is fabricated here.

## Publication route

```text
Est-dio-de-udio signed-release workflow
-> build unsigned release
-> apksigner with private keystore
-> verify observed certificate SHA-256
-> compare with expected public fingerprint
-> signed APK + receipt release
-> governed PR updating:
   docs/site/rafaelia-signing/latest.txt
   docs/site/rafaelia-signing/latest.json
-> GitHub Pages
```

The Pages branch update is proposed by PR rather than silently overwriting main.

## Verification

Public consumers need only:

- signed APK SHA-256;
- signing certificate SHA-256;
- source SHA;
- signer identifier;
- certificate_match=PASS.

## Boundary

```text
certificate_match=PASS != scientific validation
hash_match=PASS != legal ownership proof by itself
public receipt != private signing material
```
