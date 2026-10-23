"""BrowserRaf bounded wire serialization tests; host compiler is test-only.

NO network I/O, credentials, TLS encryption, or HTTPS capability is exercised.
Production BrowserRaf remains freestanding with its fail-closed HTTPS gate.
"""
from __future__ import annotations

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
C_SOURCE = r"""
#include "BrowserRaf/internal/br_http.h"

static u8 b[1024];
static TLSCtx tls;

static void sentinel(void) {
    for(u32 i=0u;i<(u32)sizeof(b);i++)b[i]=0xA5u;
}
static int unchanged(void) {
    for(u32 i=0u;i<(u32)sizeof(b);i++)if(b[i]!=0xA5u)return 0;
    return 1;
}
int main(void) {
    u32 n;
    sentinel();
    n=HTTP_BUILD_REQ("example.com","/",b,(u32)sizeof(b));
    if(n!=206u||b[n-4u]!='\r'||b[n-3u]!='\n'||b[n-2u]!='\r'||b[n-1u]!='\n')return 1;

    for(u32 cap=0u;cap<206u;cap++) {
        sentinel();
        if(HTTP_BUILD_REQ("example.com","/",b,cap)!=0u||!unchanged())return 2;
    }
    sentinel();
    if(HTTP_BUILD_REQ("bad\r\nInjected: yes","/",b,sizeof(b))!=0u||!unchanged())return 3;
    sentinel();
    if(HTTP_BUILD_REQ("example.com","/bad\nrequest",b,sizeof(b))!=0u||!unchanged())return 4;
    sentinel();
    if(HTTP_BUILD_REQ("example.com","bad",b,sizeof(b))!=0u||!unchanged())return 5;

    MC0(&tls,sizeof(tls)); /* Deterministic fixture: serialization ONLY. */
    sentinel();
    n=TLS_BUILD_CLIENT_HELLO(&tls,"example.com",b,(u32)sizeof(b));
    if(n!=103u||b[0]!=0x16u||b[3]!=0u||b[4]!=98u)return 6;
    for(u32 cap=0u;cap<103u;cap++) {
        sentinel();
        if(TLS_BUILD_CLIENT_HELLO(&tls,"example.com",b,cap)!=0u||!unchanged())return 7;
    }
    sentinel();
    if(TLS_BUILD_CLIENT_HELLO(&tls,"bad\r\nexample",b,sizeof(b))!=0u||!unchanged())return 8;
    return 0;
}
"""

class BrowserWireBounds(unittest.TestCase):
    def test_wire_c_fixture(self) -> None:
        cc = shutil.which(os.environ.get("CC", "cc"))
        if cc is None:
            self.skipTest("Host C compiler unavailable; runtime evidence remains NOT_RUN")
        with tempfile.TemporaryDirectory(prefix="br_wire_") as folder:
            root = Path(folder)
            src, exe = root / "wire.c", root / "wire"
            src.write_text(C_SOURCE, encoding="utf-8")
            build = subprocess.run(
                [cc, "-std=c11", "-O0", "-Wall", "-Wextra", "-I", str(ROOT),
                 str(src), "-o", str(exe)],
                capture_output=True, text=True, check=False, timeout=35,
            )
            self.assertEqual(build.returncode, 0, build.stdout + build.stderr)
            ran = subprocess.run(
                [str(exe)], capture_output=True, text=True, check=False, timeout=10,
            )
            self.assertEqual(ran.returncode, 0, ran.stdout + ran.stderr)

    def test_https_still_fails_closed(self) -> None:
        source = (ROOT / "BrowserRaf/internal/br_main.c").read_text(encoding="utf-8")
        tls_gate = source.index("if(ctx->use_tls)")
        deny = source.index("HTTPS não suportado neste build", tls_gate)
        sender = source.index("SEND_ALL(ctx->fd", deny)
        self.assertLess(tls_gate, deny)
        self.assertLess(deny, sender)

if __name__ == "__main__":
    unittest.main()
