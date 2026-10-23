"""BrowserRaf HTTP phase regression: hosted fixture, freestanding production.

No network, credentials, TLS, downloads or simulated browser claims.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "BrowserRaf" / "internal" / "br_main.c"
FIXTURE = r'''
#include "BrowserRaf/internal/br_types.h"
int main(void) {
    u8 f=FL_IDLE;
    if(!BR_PHASE_NEXT(&f,0u,FL_DNS,0u))return 1;
    if(!BR_PHASE_NEXT(&f,FL_DNS,FL_CONNECT,FL_DNS))return 2;
    if(!BR_PHASE_NEXT(&f,FL_CONNECT,FL_HTTP_TX,FL_CONNECT))return 3;
    if(!BR_PHASE_NEXT(&f,FL_HTTP_TX,FL_HTTP_RX,FL_HTTP_TX))return 4;
    if(!BR_PHASE_NEXT(&f,FL_HTTP_RX,FL_HTML_RND,FL_HTTP_RX))return 5;
    if(!BR_PHASE_NEXT(&f,FL_HTML_RND,FL_DONE,FL_HTML_RND))return 6;
    if((f&FL_DONE)==0u || (f&FL_ERROR)!=0u)return 7;
    f=FL_CONNECT;
    if(BR_PHASE_NEXT(&f,FL_HTTP_TX,FL_HTTP_RX,FL_HTTP_TX))return 8;
    if((f&FL_ERROR)==0u)return 9;
    return 0;
}
'''


class PhaseTransitionTests(unittest.TestCase):
    def test_browser_http_tx_is_entered_before_send_and_rx(self):
        source = SOURCE.read_text(encoding="utf-8")
        start = source.index("static s32 DO_FETCH")
        entered = source.index(
            "BR_PHASE_NEXT(&ctx->flags,FL_CONNECT,FL_HTTP_TX,FL_CONNECT)", start
        )
        sent = source.index("SEND_ALL(ctx->fd", entered)
        received = source.index(
            "BR_PHASE_NEXT(&ctx->flags,FL_HTTP_TX,FL_HTTP_RX,FL_HTTP_TX)", sent
        )
        self.assertLess(start, entered)
        self.assertLess(entered, sent)
        self.assertLess(sent, received)

    def test_real_c_flag_chain_and_invalid_transition(self):
        cc = shutil.which("cc")
        if not cc:
            self.skipTest("host C compiler absent, status=NOT_RUN")
        with tempfile.TemporaryDirectory(prefix="browser_phase_") as folder:
            source = Path(folder) / "phase.c"
            binary = Path(folder) / "phase"
            source.write_text(FIXTURE, encoding="utf-8")
            built = subprocess.run(
                [cc, "-std=c11", "-O0", "-fno-builtin", "-I", str(ROOT),
                 str(source), "-o", str(binary)],
                capture_output=True, text=True, timeout=30, check=False,
            )
            self.assertEqual(built.returncode, 0, built.stdout + built.stderr)
            result = subprocess.run(
                [str(binary)], capture_output=True, text=True,
                timeout=10, check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == "__main__":
    unittest.main()
