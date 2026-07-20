from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BROWSER_MAIN = ROOT / "BrowserRaf" / "internal" / "br_main.c"
TLS_HEADER = ROOT / "BrowserRaf" / "internal" / "br_tls.h"
ENTROPY_HEADER = ROOT / "BrowserRaf" / "internal" / "br_entropy.h"
CSPRNG_CONFIG = ROOT / "configs" / "browserraf-csprng.phase2.json"


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

    def test_tls_random_uses_kernel_entropy_without_deterministic_fallback(self) -> None:
        tls_source = TLS_HEADER.read_text(encoding="utf-8")
        entropy_source = ENTROPY_HEADER.read_text(encoding="utf-8")

        self.assertIn("BR_RANDOM_FILL(t->random", tls_source)
        self.assertIn("static s32 TLS_INIT", tls_source)
        self.assertIn("TLS_ALERT_INTERNAL_ERROR", tls_source)
        self.assertNotIn("0xDEADBEEF", tls_source)
        self.assertNotIn("AI u32 PRNG", tls_source)
        self.assertNotIn("LFSR + PHI64", tls_source)

        for syscall_number in ("384u", "278u", "318u"):
            self.assertIn(f"BR_NR_GETRANDOM {syscall_number}", entropy_source)
        self.assertIn("if(got==-(s32)BR_EINTR)continue", entropy_source)
        self.assertGreaterEqual(entropy_source.count("MC0(p,n)"), 2)
        self.assertIn("return-1", entropy_source)

    def test_csprng_manifest_preserves_fail_closed_claim_boundary(self) -> None:
        manifest = json.loads(CSPRNG_CONFIG.read_text(encoding="utf-8"))

        self.assertFalse(manifest["policy"]["claim_allowed"])
        self.assertFalse(manifest["policy"]["https_enabled"])
        self.assertFalse(manifest["policy"]["tls_certified"])
        self.assertFalse(manifest["policy"]["deterministic_fallback_allowed"])
        self.assertEqual(manifest["implementation"]["entropy_source"], "linux_getrandom")
        self.assertEqual(manifest["implementation"]["fallback"], "none")
        self.assertEqual(manifest["status"]["https"], "FAIL_CLOSED")
        self.assertEqual(manifest["status"]["cross_abi_compile"], "PASS_LIMITED")
        self.assertEqual(
            manifest["status"]["canonical_runner_execution"], "TOKEN_VAZIO"
        )
        self.assertEqual(manifest["status"]["device_runtime"], "TOKEN_VAZIO")

        evidence = {item["kind"]: item for item in manifest["evidence"]}
        self.assertEqual(evidence["CROSS_ABI_VERIFIER"]["status"], "PRESENT")
        self.assertEqual(
            evidence["INDEPENDENT_CROSS_ABI_COMPILE"]["status"], "PASS_LIMITED"
        )
        self.assertEqual(
            evidence["CANONICAL_RUNNER_EXECUTION"]["status"], "TOKEN_VAZIO"
        )


if __name__ == "__main__":
    unittest.main()
