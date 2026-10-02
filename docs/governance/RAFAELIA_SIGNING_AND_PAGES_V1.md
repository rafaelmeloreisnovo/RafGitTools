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


## Local public-page gate

The canonical `scripts/validate_rafaelia_workflow.sh` invokes
`scripts/validate_rafaelia_signing_contract.sh`. The gate parses the JSON
receipt, enforces the V1 field set and schema, requires the current
`AWAITING_REAL_SIGNING_RECEIPT` state with typed `TOKEN_VAZIO` values, checks
text/JSON parity, validates public-variable and secret-placeholder metadata,
and scans the Pages folder for private-key material.

A future signed, rejected, or otherwise promoted receipt requires a versioned
contract successor and tests. V1 does not validate GitHub secret configuration,
perform APK signing, prove a Pages deployment, or establish physical-device
acceptance. These remain separate gates.
