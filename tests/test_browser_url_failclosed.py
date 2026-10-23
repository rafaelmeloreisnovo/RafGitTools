"""Host-only BrowserRaf URL parser falsifiers; production remains freestanding."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
FIXTURE = r'''
#include "BrowserRaf/internal/br_types.h"
#include "BrowserRaf/internal/br_dns.h"

int main(void){
    BCtx ctx;
    if(URL_PARSE("http://example.com/a",&ctx)!=0)return 1;
    if(ctx.port!=80u||ctx.use_tls||ctx.host[0]!='e'||ctx.path[1]!='a')return 2;
    if(URL_PARSE("https://example.com/path",&ctx)!=0)return 3;
    if(!ctx.use_tls||ctx.port!=443u)return 4; /* parsed, not connected */
    if(URL_PARSE("http://127.0.0.1:65535/path",&ctx)!=0)return 5;
    if(ctx.port!=65535u)return 6;
    if(URL_PARSE("http://127.0.0.1:65536/path",&ctx)==0)return 7;
    if(URL_PARSE("http://example.com:0/path",&ctx)==0)return 8;
    if(URL_PARSE("http://example.com:/path",&ctx)==0)return 9;
    if(URL_PARSE("http://example.com:xyz/path",&ctx)==0)return 10;
    if(URL_PARSE("http://bad\r\nHeader:attack/", &ctx)==0)return 11;
    if(URL_PARSE("http://example.com/path\nHeader:attack", &ctx)==0)return 12;
    if(URL_PARSE("http://user@host/path", &ctx)==0)return 13;
    if(URL_PARSE("", &ctx)==0)return 14;
    if(URL_PARSE((const char*)0, &ctx)==0)return 15;
    if(URL_PARSE("http://example.com/", (BCtx*)0)==0)return 16;
    static char large_host[310];
    for(unsigned i=0u;i<sizeof(large_host);i++)large_host[i]='a';
    large_host[0]='h';large_host[1]='t';large_host[2]='t';
    large_host[3]='p';large_host[4]=':';large_host[5]='/';large_host[6]='/';
    large_host[309]=0;
    if(URL_PARSE(large_host,&ctx)==0)return 17;
    static char large_path[610];
    for(unsigned i=0u;i<sizeof(large_path);i++)large_path[i]='a';
    large_path[0]='h';large_path[1]='t';large_path[2]='t';
    large_path[3]='p';large_path[4]=':';large_path[5]='/';large_path[6]='/';
    large_path[7]='x';large_path[8]='/';large_path[609]=0;
    if(URL_PARSE(large_path,&ctx)==0)return 18;
    return 0;
}
'''

class UrlBoundaries(unittest.TestCase):
    def test_host_c_parser_hard_bounds(self):
        cc = shutil.which("cc")
        if cc is None:
            self.skipTest("no host C compiler; NOT_RUN")
        with tempfile.TemporaryDirectory(prefix="browser_url_") as d:
            src, exe = Path(d)/"url.c", Path(d)/"url"
            src.write_text(FIXTURE, encoding="utf-8")
            built = subprocess.run(
                [cc, "-std=c11", "-O0", "-fno-builtin", "-I", str(ROOT),
                 str(src), "-o", str(exe)],
                capture_output=True, text=True, check=False, timeout=35,
            )
            self.assertEqual(built.returncode, 0, built.stderr)
            ran = subprocess.run(
                [str(exe)], capture_output=True, text=True, check=False,
                timeout=10,
            )
            self.assertEqual(ran.returncode, 0, f"rc={ran.returncode} {ran.stderr}")


if __name__ == "__main__":
    unittest.main()
