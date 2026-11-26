# KSP 2.3.14 — exact upstream resolution candidate

Predecessor 3e7c649472a3531ee8aa9e6d0eaf9b7884ec4f3d, START run 45860341787, Android job 140049869579: coherence and CodeQL passed; Gradle failed resolving plugin marker before compilation. Maven Central lists the exact marker POM delegating to `com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.14` (Apache-2.0): https://central.sonatype.com/artifact/com.google.devtools.ksp/com.google.devtools.ksp.gradle.plugin/2.3.14 . Upstream https://github.com/google/ksp/releases/tag/2.3.14 . Actual downloaded bytes and runner reachability remain TOKEN_VAZIO.

Use exact module mapping for this ID/version only; no added repository, fallback version, credential, or disabled gate. Hypothesis: marker-resolution issue rather than missing implementation. If module resolution still fails, preserve failure and investigate network/artifact evidence. Retain KSP as hosted build-time symbol processor for Hilt/Room; independent replacement requires inventory of generated bindings, equivalent code generation, negative tests and dual-ABI APK parity; no copied upstream code. No new runtime dependency. License notices and resolved artifact hashes remain unverified.

Full exact-head APK/build/test/lint/CodeQL and review required. Rollback: revert this successor; candidate stays HOLD, claim_allowed=false.
