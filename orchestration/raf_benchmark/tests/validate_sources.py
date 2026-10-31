#!/usr/bin/env python3
"""Scoped source-only gate; never confuses passing structure with Android runtime."""
import json
import pathlib
import re
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve()
ROOT = HERE.parents[3]
APP = ROOT / "app/src/main/kotlin/com/rafgittools"
BENCH = APP / "benchmark"
CONTRACT = HERE.parents[1] / "00_CONTRACTS/WORKFLOW_AND_CREDITS_V2.json"


def main():
    routes = (BENCH / "RafApplicationsCatalog.kt").read_text(encoding="utf-8")
    core = (BENCH / "RafBenchmarkCore.kt").read_text(encoding="utf-8")
    exchange = (BENCH / "RafBenchmarkExchange.kt").read_text(encoding="utf-8")
    receipt = (BENCH / "RafBenchmarkReceiptStore.kt").read_text(encoding="utf-8")
    ui = (APP / "RafBenchmarkActivity.kt").read_text(encoding="utf-8")
    credits = (BENCH / "ContextualCredits.kt").read_text(encoding="utf-8")
    ref = json.loads(CONTRACT.read_text(encoding="utf-8"))
    markers = re.findall(r'add\(Route\("(K\d{3})"', routes)
    assert markers == [f"K{x:03}" for x in range(1, 91)], "K001..K090 nonconsecutive"
    assert routes.count('Group("') == 8, "A..H count"
    assert "CATALOG_ONLY" in routes and "CLAIM_ALLOWED" not in routes
    assert "val allowedWorkers = listOf(1, 2, 4, 8)" in core
    assert "val allowedBufferKiB = listOf(256, 2048, 16384, 65536, 153600)" in core
    assert "val v = when (workload)" in core
    assert "xor (worker * 257)" not in core, "worker-dependent workload"
    assert "CountDownLatch" in core and "futures.map" in core
    assert "BLOCKED_MODULE" in exchange and "REQUIRES_HUMAN_CLICK" in exchange
    assert "authorityVerified: Boolean = false" in exchange
    assert "proposalSha256" in receipt and "previous_hash" in receipt
    assert "TOKEN_VAZIO_EXACT_CODE_SHA" in receipt
    assert "RafApplicationsCatalogView()" in ui
    assert "ContextualCreditsInfo" in ui and "EXTERNAL_API" in credits
    tree = ET.parse(ROOT / "app/src/main/AndroidManifest.xml")
    ns = "{http://schemas.android.com/apk/res/android}"
    activities = tree.findall(".//activity")
    benchmark = [a for a in activities if a.attrib.get(ns + "name") == ".RafBenchmarkActivity"]
    assert len(benchmark) == 1, "exactly one Raf Benchmark launcher"
    categories = [c.attrib.get(ns + "name") for c in benchmark[0].iter("category")]
    assert "android.intent.category.LAUNCHER" in categories
    for old in (".RepositoryPrivacyActivity", ".SetupWizardActivity", ".ActionsControlActivity"):
        assert any(a.attrib.get(ns + "name") == old for a in activities), old
    assert ref["release"]["allowed"] is False
    assert ref["knowledge"]["count"] == 90
    assert ref["privacy"]["no_cross_repo_secret_alias"] is True
    print("PASS_SCOPED_SOURCE: catalog90, 8 groups, launcher, bounds, credits, "
          "proposal deny, local receipts, release hold")
    print("ANDROID_BUILD=NOT_RUN DEVICE=NOT_RUN CLAIM_ALLOWED=false")


if __name__ == "__main__":
    main()
