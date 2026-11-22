# RafGitTools Ω HyperAtlas — ZIP64 source-reader contract V1

**Producer:** `rafaelmeloreisnovo/RafGitTools`  
**Authority:** existing Android Home → Drive → HyperAtlas (user-selected SAF tree/file); Mapa continues to govern federation.  
**Existing Drive ZIPs:** never move, replace, extract, upload into GitHub, or download onto the phone as an incidental scan.

## Implemented in this scoped PR

- `app/src/main/kotlin/com/rafgittools/navigator/ZipCentralDirectoryRangeV1.kt`: independent ZIP/ZIP64 parser over `Reader.readAt(offset, byteCount)`, with 64-bit offsets and strict seek/range preconditions. It only parses EOCD, optional ZIP64 locator/EOCD, and a capped central-directory window; no member content is decompressed.
- `app/src/main/kotlin/com/rafgittools/navigator/SafZipRangeGateV1.kt`: existing Android SAF authorization path. Before `openFileDescriptor`, the gate reads file size via `OpenableColumns.SIZE` and **refuses unknown size and anything over 16 MiB**. It requires a known descriptor length and positional `FileChannel.read`; no sequential whole-file fallback.
- `ui/screens/setupwizard/DriveFirstRunIndexCard.kt`: explicit button to select and inspect **a small ZIP**; shows up to eight internal paths, known sizes, declared/observed member counts and typed gaps. Separate existing folder scan inventories large ZIPs by metadata only.
- `app/src/test/kotlin/com/rafgittools/navigator/ZipCentralDirectoryRangeV1Test.kt`: classic ZIP, synthetic sparse >4 GiB ZIP64, truncated/unsafe paths, budgets and short reads.

**Maximum source-read budget:** EOCD tail <= 65,557 bytes + ZIP64 record/locator <= 76 bytes + central directory <= 2 MiB. The default central-directory budget is 1 MiB, capped at 4,096 entries. If limits are hit, the output records a typed partial result instead of claiming completeness. External `Reader` MUST guarantee exact positioned reads. Neither this parser nor the Android bridge establishes that an external provider would not internally cache or stage a small selected file.

## Important 4–20 GB distinction

The pure parser **does support ZIP64 offsets larger than 4 GB** (synthetic virtual sparse test without materializing 4 GB). It does **not** provide a validated authenticated Google Drive HTTP `Range` transport. A SAF provider may download large files when opening a descriptor, so the Android gate deliberately blocks those files before opening them. No installed ChatGPT connector authorization is inherited by the APK. ZIPRAF may be a distinct format; a `.zipraf` filename is not proof of ZIP compatibility. Split archives are not accepted as standalone valid ZIPs.

To inspect large files remotely, next implement an OAuth-scoped Drive `Range` reader that checks `206 Partial Content`, exact `Content-Range`, immutable revision or ETag across reads, ACL, quota, bounded network bytes, and fail-closed handling of `200 OK` full-file responses. Reuse this parser as the consumer, without changing its pure metadata contract.

## Verification and rollback

- Local host smoke: classic ZIP two entries and synthetic ZIP64 archive length >4 GiB with central-directory offset >4 GiB; approximately 65.7 KB of positioned reads for ZIP64, no 4 GiB allocation. This is **not** a real provider test.
- Android compilation, unit tests and Motorola physical access must be assessed against the exact final commit. No runtime PASS from source existence.
- Preserve DRAFT/HOLD until CI and user-selected Drive source/destination tested. START workflow topological failures are a separate gate and must not be silently conflated with Android compilation.
- Rollback: keep branch unmerged; no original ZIPs, Drive files or organization repository contents are touched.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`; `TOKEN_VAZIO != 0`; `claim_allowed=false`.
