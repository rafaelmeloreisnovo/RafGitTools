# RAFAELIA Ω7 — Gate das sete direções (Draft/HOLD)

This bounded **offline**, stdlib-only gate extends the existing Federated Activity Governor PR #826. It never mutates a provider or authorizes a merge, release, migration, or pointer advance.

Directions: \`direta\` = exact-source predicate; \`inversa\` = rollback reconstruction; \`derivativa\` = typed finite-state diff; \`antiderivativa\` = successor reconstruction from parent+delta; \`recursiva\` = bounded digest-linked chain (<=42); \`ortogonal\` = four structurally distinct witness channels (not independence attestation); \`integrativa\` = fail-closed conjunction.

"Seven antiderivatives" is **methodological vocabulary, not a mathematical theorem**. Only the typed diff/reconstruction and transition/inverse pairs have partial inverse relations.

Run from this directory:
\`\`\`sh
python3 -m unittest -v test_omega7.py
python3 omega7_gate.py /path/to/authorized-evidence.json
\`\`\`

All evidence must be supplied by an authorized caller, <=64 KiB, and must not include private bodies or credentials. The gate has no network calls. Its positive result is \`READY_FOR_INDEPENDENT_PROVIDER_RECHECK\` **not PASS**. Boolean predicates and witness labels are *input assertions*, not independently verified provider facts. Every output keeps \`claim_allowed=false\` and \`provider_mutation_authorized=false\`. Unknown state, stale SHA, failed predicate, broken chain, invalid delta, or missing witness blocks.

Producer authority: each repository; federation routing: private Mapa; executor/gates: RafGitTools; historical Drive/Notion/Supabase only via their own authorized adapters. No automatic cross-provider writer is installed.

**Required for promotion:** exact-head CI, provider-negative bypass test, independent review, producer/consumer evidence, CAS/lock on Drive pointer updates, rollback receipts and readback. None established here.
