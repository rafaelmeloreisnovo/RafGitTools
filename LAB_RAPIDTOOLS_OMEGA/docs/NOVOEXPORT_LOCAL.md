# NOVOexport / shader / leader / conversão.jsum — bounded offline inventory

Do not upload private conversation corpora or live provider secrets into this public repository.

To run on a directory explicitly chosen on your device:
    python3 tools/nov_export_scan.py /sdcard/NOVOexport

Initial depth=1, maximum=2. Default SHA256 file cap=16 MiB. Large conversation files are marked TOKEN_VAZIO_HASH_SIZE_GATED and not read beyond metadata. Names and relative paths are redacted as SHA-256 path identifiers, and no source file content is emitted. Unrecognized JSUM is opaque; no fabricated parser.

To persist local-only metadata:
    python3 tools/nov_export_scan.py /sdcard/NOVOexport --write-local

Output build/private_novoexport_inventory.json is created once using exclusive creation, local file mode 0600, ignored by Git. The command does not grant remote access. The user may need termux-setup-storage to authorize Android storage access separately.

Use --max-depth 2 only for a second targeted pass. No symlinks, remote queries or raw corpus copies to Drive/GitHub. Separately review metadata receipts before authorizing any provider synchronization.
