# Raf Index Ω — V1.1.0-rc1 · REVIEW / HOLD

The indexer code package is preserved in the official private Google Drive. The
GitHub connector blocked source-file writes on this branch; **no Android APK
or source integration is claimed**.

Drive artifact: https://drive.google.com/file/d/1mBixyEVHjNZsYd6FEHsShhkrfCnHCIY-/view
SHA256: 50c862a09e2994c7444080e3726a3c0056d76373affe3bb7343ca751f8e944b0

Ten local unit tests PASS for the Termux CLI. The application is still
1.0.0-alpha in main; 1.1.0-rc1 is a target, not a release. The CLI indexes
explicitly selected rclone Drive remotes (per-root, recursive) and GitHub
owner repositories (public or token-authorized), saves append-only snapshots
and SHA-bound pointers; external writes default OFF. Provider readback and
in-app integration remain unverified.

Desired routes:
- Drive account root /001_PONTEIRO/INDEX_<digest>.json plus LATEST_POINTER.json
- GitHub <owner>/<repo>/.rafaelia-index/INDEX.v1.json (PER-REPO REVIEW_REQUIRED)
- RafGitTools APK v1.1: user-initiated scans and installation upgrade gate
- No automatic scans of unrelated accounts, secrets, merge or deployment.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != PASS
F_next: authorize source write in producer; exact-HEAD CI; signed APK build,
lint, review, Android upgrade test on device; release HOLD.
