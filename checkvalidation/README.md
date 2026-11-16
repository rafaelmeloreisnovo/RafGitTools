# CheckValidation v1

CheckValidation separates rapid contribution feedback from expensive publication evidence.

## Contract

- `feature/*` and `fix/*` target `develop`.
- `develop` is the integration branch and must receive the bounded fast gate.
- `main` is the publication branch.
- Expensive Gradle/JDK/NDK/JNI/R8/Rust/C/ASM validation runs asynchronously against an immutable commit.
- Development may continue while heavy validation runs.
- Publication requires evidence bound to the exact candidate SHA and explicit human authorization.
- No title such as DRAFT/HOLD is an enforcement mechanism.
- `PASS_SCOPED` from the fast gate is not build, device, security, scientific, or release proof.

## Local fast gate

    python3 checkvalidation/check.py --profile checkvalidation/profiles/rafgittools.v1.json

The gate uses only Python's standard library, writes `build/checkvalidation/receipt.json`, rejects mutable action references and active Node.js 20 workflow baselines, and preserves `claim_allowed=false`.

## Heavy lane

The `full` lane is asynchronous and non-blocking for development. A producer repository remains responsible for real build and runtime evidence. CheckValidation records orchestration state; it does not manufacture a PASS.

## Rollback

Close the draft PR or revert the isolated commits. This implementation does not merge, publish, sign, delete history, or update `main`.
