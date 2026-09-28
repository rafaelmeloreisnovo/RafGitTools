# TEMPLO_VIVO_ARC — Privacy, Provenance and Cadence V1

This is a bounded model for the intent “retain the visual object/provenance without exposing the original text, and progressively reduce polling frequency.”

## 1. Visual/pixel rule

There is no reliable confidentiality guarantee in making text “human-readable but AI-unreadable.” If a human can read visible text, the system assumes a capable OCR/vision model may also read it.

Therefore V1 separates two concerns:

```text
VISIBLE/SHARED SIDE
opaque image hash + dimensions + digest fingerprint + provenance pointer

PRIVATE SIDE
original body or encrypted payload + authorization + retention + key/deobfuscator IDs
```

`visual-anchor` reads image bytes only to compute deterministic identity metadata. It performs no OCR and creates no semantic embedding. The digest vector is an opaque fingerprint, not a meaning vector and not encryption.

## 2. Original content and deobfuscator

RafGitTools is public, so it stores **no deobfuscation key material and no reversible plaintext dictionary**. Only `key_id` and `deobfuscator_id` may cross the public boundary. Secret values stay in GitHub Secrets or another approved private store. The private authority is:

`rafaelmeloreisnovo/Rafaelia_Private/inputs/templo_vivo_arc/`

Raw content, passwords, PATs, private keys, seed phrases and authorization headers are prohibited fields in the public intake contract.

## 3. TOKEN_VAZIO subclasses

The typed dictionary in `token_vazio.v1.json` distinguishes missing source, authority, execution target, evidence, key identity, deobfuscator, privacy class, retention, visual anchor, schedule rule and receipt. None of them means 0, false or PASS.

## 4. Adaptive cadence

Two sequence phases are fully specified by the request:

- `BOOT_1_5`: 1, 2, 3, 4, 5 minutes;
- `RAMP_1_29`: 1 through 29 minutes.

Only a `PASS` advances a sequence. `FAIL|BLOCKED|PENDING|AUDIT|TOKEN_VAZIO|IMPLEMENTED_UNTESTED` holds the current phase.

Then the model has:
- `HOURLY`: 60 minutes;
- `TWELVE_HOURLY`: 720 minutes;
- `DAILY`: 1440 minutes.

The transition counts from hourly→12-hour and 12-hour→daily were not specified, so V1 intentionally stores those promotion rules as `TOKEN_VAZIO` instead of inventing them.

## 5. Backend truth

`LOCAL_ACTIVE_LOOP` allows 1-minute intent **only while a process/service is actually running**.

`GITHUB_ACTIONS_SCHEDULE` is best-effort and has a 5-minute minimum expression; therefore 1–4 minutes is `BLOCKED` for that backend. Cron metadata is scheduling intent, not execution evidence.

No second active workflow is added: RafGitTools currently has a single-root `.github/workflows/START.yml` invariant.

## 6. Commands

```bash
python3 orchestration/templo_vivo_arc/engine.py \
  validate-intake \
  --config orchestration/templo_vivo_arc/config.v1.json \
  --input pointer.json

python3 orchestration/templo_vivo_arc/engine.py \
  schedule-next \
  --config orchestration/templo_vivo_arc/config.v1.json \
  --state state.json \
  --outcome PASS \
  --backend LOCAL_ACTIVE_LOOP

python3 orchestration/templo_vivo_arc/engine.py \
  visual-anchor \
  --image screenshot.png
```

## 7. Evidence boundary

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`

This branch can establish implementation and local deterministic test evidence. It cannot claim that a private runner, GitHub Secret, Android service or hourly process is active until that exact runtime is observed.

## R3

- F_ok: model, privacy boundary, typed gaps, deterministic scheduler and no-OCR visual anchor are materialized.
- F_gap: live private-reader credential/runner, exact schedule transition counts after hourly, CI and device/runtime receipts.
- F_next: run exact-head tests and one synthetic pointer exchange; only then consider wiring the single START root or a local Termux service.
