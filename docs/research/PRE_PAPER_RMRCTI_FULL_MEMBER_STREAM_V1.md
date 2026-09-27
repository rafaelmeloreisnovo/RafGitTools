# PRE-PAPER — RMRCTI Full Member Stream V1

State: SOURCE_IMPLEMENTED / CI_PENDING / PHYSICAL_NOVOEXPORT_NOT_RUN
claim_allowed=false

## Question

How can a bounded RMRCTI sample that receives higher rigor be promoted to a
full-source descriptor on Android without pretending that the sample hash is the
member hash and without materializing the complete member in RAM?

## Producer authority

Observed producer:
- rafaelmeloreisnovo/llamaRafaelia
- rmrCti/rmrcti_dataset_mobile.py
- blob f8e6687ef1994f64db8bcb5ce765f8cd21c842f1

Observed ZIP reader:
- rmrCti/rmrcti_zipio.py
- blob 945238b9a4911a715a19a0bfd7217d9870274239

The producer's stream path uses iter_member_bytes(). The ZIP reader verifies
full emitted size and CRC32 only after the complete stream is consumed.
iter_member_prefix_bytes() explicitly does not claim full CRC verification.

## Invariants

BOUNDED_SAMPLE != FULL_SOURCE

H(sample) != H(full member)

AUTO_BIAS_MAX = MULTIMODAL

EVIDENCE != automatic promotion

FULL_SOURCE is assigned only after:
1. stream reaches EOF;
2. emitted byte count equals central-directory uncompressed size;
3. CRC32 equals the member CRC32;
4. SHA-256 has been computed over the complete uncompressed stream;
5. replay used for text descriptors sees the same SHA-256.

## Bounded-memory pipeline

sample
-> rigor bias
-> full-member stream
-> chunked SHA-256 / CRC32 / byte descriptors
-> optional bounded text replay
-> FULL_SOURCE source ref
-> LibraryLocalJobExecutor with precomputed stream descriptors
-> catalog bundle
-> receipt

The executor no longer requires a whole ByteArray when a descriptor was
computed from a verified full stream.

## Security boundary

The optional Python adapter uses ProcessBuilder argv directly:

python3 rmrcti_dataset_mobile.py stream ZIP --member MEMBER --out - --max-output-mib N

No shell command string is built.

Private filesystem paths are runtime-only and are not placed in the public
receipt. Public provenance uses the opaque archive locator hash plus producer
blob/path identifiers.

## Falsifiers

- truncated stream -> FAIL
- size mismatch -> FAIL
- CRC mismatch -> FAIL
- second-pass SHA mismatch -> FAIL
- output budget exceeded -> FAIL
- bounded sample treated as full source -> FAIL
- automatic EVIDENCE target -> FAIL
- full hash differs from LibrarySourceRef.contentSha256 -> CHECKPOINTED
- generated catalog fails LibraryCatalogGate -> FAIL

## Known limitation

Text vector V1 keeps at most 8192 unique token hashes in the bounded streaming
set. If saturated, the descriptor remains usable but records
TEXT_UNIQUE_ESTIMATE_SATURATED_AT_8192.

## F_next

Run the first physical NOVOexport member through the exact producer stream
route on the phone and persist the resulting full-member receipt + catalog
artifact. Then compare the physical result against an independent replay.


## Promotion provenance gate

Every full-member read is now typed as one of:

- RIGOR_BIAS
- EXPLICIT_HUMAN_REQUEST

RIGOR_BIAS requires the full RigorBiasDecisionEnvelopeV1. The plan stores the
bias input SHA-256 and a canonical SHA-256 of the decision envelope.

EXPLICIT_HUMAN_REQUEST requires a non-empty request identifier.

Therefore the custody edge is explicit:

sample job -> promotion trigger -> bias/request provenance -> full stream job.

The full-member executor still refuses EVIDENCE. Explicit evidence processing
belongs to the separate evidence-gated workflow.
