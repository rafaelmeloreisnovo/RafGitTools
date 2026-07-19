from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BROWSER_MAIN = ROOT / "BrowserRaf" / "internal" / "br_main.c"
TLS_HEADER = ROOT / "BrowserRaf" / "internal" / "br_tls.h"


class BrowserCapabilityClaimTests(unittest.TestCase):
    def test_banner_does_not_claim_operational_tls(self) -> None:
        source = BROWSER_MAIN.read_text(encoding="utf-8")
        self.assertIn("HTTPS FAIL-CLOSED", source)
        self.assertNotIn("BROWSER · TLS1.3 · HTTP/1.1", source)

    def test_https_path_fails_before_plain_http_request(self) -> None:
        source = BROWSER_MAIN.read_text(encoding="utf-8")
        tls_gate = source.index("if(ctx->use_tls)")
        unsupported = source.index("HTTPS não suportado neste build", tls_gate)
        request_builder = source.index("HTTP_BUILD_REQ", unsupported)
        self.assertLess(tls_gate, unsupported)
        self.assertLess(unsupported, request_builder)

    def test_tls_header_exposes_unfinished_upgrade_steps(self) -> None:
        source = TLS_HEADER.read_text(encoding="utf-8")
        for symbol in (
            "TLS_UP_X25519",
            "TLS_UP_HKDF",
            "TLS_UP_TRANSCRIPT",
            "TLS_UP_AEAD",
            "TLS_UP_FINISHED",
            "TLS_UP_RECORD_CRYPT",
        ):
            self.assertIn(symbol, source)


if __name__ == "__main__":
    unittest.main()
