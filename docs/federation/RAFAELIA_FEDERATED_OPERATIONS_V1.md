# RAFAELIA Federated Operations v1

**Status:** `DRAFT_OPERATIONAL`  
**Control plane:** `rafaelmeloreisnovo/RafGitTools`  
**Scope:** orchestration across repositories without replacing each repository's local source of truth.

## 1. Sustaining invariant

The federation exists to preserve one operational invariant:

```text
claim -> source -> implementation -> test -> evidence -> decision -> rollback
```

A claim that cannot complete this path is not silently promoted. It receives one of the states below:

- `VERIFIED`: direct evidence exists and is addressable.
- `TESTED`: reproducible test or artifact exists, but external validation may still be pending.
- `PARTIAL`: implementation exists with known gaps.
- `DECLARED_BY_AUTHOR`: author statement preserved without independent repository proof.
- `TOKEN_VAZIO`: required evidence is absent, inaccessible or not yet executed.
- `CONTRADICTION`: evidence conflicts with the claim.
- `BLOCKED`: execution cannot proceed because a named prerequisite failed.

`TOKEN_VAZIO` is a valid result. It must never be converted into `PASS` by wording, inference, elapsed time or agent confidence.

## 2. Federation roles

| Repository | Federated role | Local source of truth remains |
|---|---|---|
| `RafGitTools` | control plane, repository map, operator-facing status | Android/JGit/GitHub application documents and code |
| `Vectras-VM-Android` | VM lifecycle, Android frontend, QEMU/proot boot chain | `PROJECT_STATE.md`, `BUILDING.md`, release ledgers |
| `termux-app-rafacodephi` | Android shell/runtime substrate | `docs/STATUS.md`, ABI policy and bootstrap evidence |
| `RafPolimata` | semantic contracts, evidence compiler and governance | canonical protocols/configs already maintained locally |
| `GAIA_phi` | deterministic indexing, data manifests and evidence storage | C/Python core and its tests |
| `ZIPRAF_OMEGA_FULL` | serialization, integrity and bounded prototype execution | local specifications, hashes and pytest suite |
| `llamaRafaelia` | local model/runtime integration and inference experiments | model/runtime code and reproducible benchmarks |
| `Rafaelia_Private` | private intellectual-property and restricted evidence vault | private repository policy and local access controls |
| `ChipQuantum` | low-level kernels, geometry, cryptography and experiments | source, papers, tests and architecture navigation |
| `relativity-living-light` | scientific claims, data workflows and falsification | claim-gated scientific documentation and real-data workflow |
| `Matem-tica-` | formal definitions, proofs and finite verifiers | proof/test classification defined in the repository |
| `Mapa` | human and machine navigation across the ecosystem | curated repository map and provenance links |

The control plane may report state, but may not overwrite the scientific, build, security or release truth owned by a repository.

## 3. Two-cycle execution

### Cycle A — observation and refusal

1. Read the repository's canonical status/index.
2. Resolve the exact commit or branch.
3. Collect evidence without modifying code.
4. Mark missing evidence as `TOKEN_VAZIO`.
5. Reject temporal inference: an old success is not proof of current success.

### Cycle B — bounded change and proof

1. Create a dedicated branch.
2. Change the smallest coherent surface.
3. Execute the repository's native tests.
4. Execute blind/order-independent tests where applicable.
5. Record artifact hashes and failure output.
6. Open a draft pull request.
7. Merge only after repository-local gates pass.

## 4. Fail-safe, failover and rollback

Every federated operation must declare:

- **health probe:** command or artifact that proves the component is responsive;
- **failure boundary:** what is isolated when the probe fails;
- **fail-safe state:** read-only, stopped, previous artifact or explicit `BLOCKED`;
- **failover target:** optional alternate component that can continue without changing the claim;
- **rollback anchor:** base commit, tag, signed artifact or immutable hash;
- **recovery proof:** the test that must pass after rollback/failover.

A failover may preserve availability, but it cannot promote scientific or implementation claims. Example: running a host-side simulator does not prove an Android device runtime.

## 5. Watchdog contract

The watchdog validates the federation manifest; it does not pretend to execute remote repositories. Its responsibilities are:

- schema and uniqueness checks;
- required-gate checks;
- dependency integrity;
- deterministic canonical digest;
- blind permutation test (same semantic digest regardless of repository order);
- simulated repository failure and failover selection;
- explicit non-zero exit when a critical repository has neither a safe state nor a failover path.

Run:

```bash
python3 scripts/federation/watchdog.py \
  --manifest configs/rafaelia-federation.json \
  --report artifacts/federation-watchdog.json

python3 scripts/federation/watchdog.py \
  --manifest configs/rafaelia-federation.json \
  --simulate-failure rafaelmeloreisnovo/Vectras-VM-Android
```

## 6. Blind-test policy

Blind tests must hide at least one non-essential execution detail while preserving the expected invariant. Supported classes:

- **order blindness:** permute repository records and require the same canonical digest;
- **fixture blindness:** choose a fixture by seed rather than filename;
- **implementation blindness:** compare outputs from two independent implementations;
- **failure injection:** mark one dependency unavailable and verify safe isolation;
- **temporal blindness:** evaluate using explicit timestamps/commits, never “latest” by assumption.

The seed, selected fixture and resulting digest must be recorded after the run so the experiment remains reproducible.

## 7. No abstraction over local truth

Federation vocabulary must point to concrete repository objects:

```text
repository + commit + path + command + output + artifact hash
```

Broad themes, metaphors and cross-domain interpretations are permitted as research context only. They cannot substitute a file path, command, test, dataset or proof obligation.

## 8. Operator output

Each operation ends with:

```text
F_ok: evidence that passed
F_gap: TOKEN_VAZIO, contradiction or blocked prerequisite
F_next: one bounded next action
rollback_anchor: commit/tag/hash
```

This structure is the minimum friendly interface for humans, agents and CI systems.
