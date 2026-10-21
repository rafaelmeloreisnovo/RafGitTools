"""BrowserRaf HTTP/1.1 subset: native-C host falsifiers, no network/TLS.

This gate checks finite protocol framing only. It cannot certify an HTTPS,
cookie or modern web-session implementation. No new runtime dependencies.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "BrowserRaf/internal/br_main.c"
FIXTURE = r'''
#include "BrowserRaf/internal/br_http.h"

#define LEN(s) ((u32)sizeof(s)-1u)
int main(void){
    const u8 ok[] = "HTTP/1.1 200 OK\r\nContent-Length: 4\r\n\r\nWiki";
    const u8 missing[] = "HTTP/1.1 200 OK\r\nContent-Length: 4\r\n";
    const u8 upper[] = "HTTP/1.1 200 OK\r\ncontent-length: 4294967295\r\n\r\n";
    const u8 overflow[] = "HTTP/1.1 200 OK\r\nContent-Length: 4294967296\r\n\r\n";
    const u8 corrupt[] = "HTTP/1.1 200 OK\r\nContent-Length: 3junk\r\n\r\n";
    const u8 empty[] = "HTTP/1.1 204 No Content\r\n\r\n";
    const u8 ch[] = "4\r\nWiki\r\n0\r\n\r\n";
    const u8 badsize[] = "ffffffff\r\nx\r\n";
    const u8 truncated[] = "4\r\nWiki\r\n0\r\n";
    const u8 trailer[] = "4\r\nWiki\r\n0\r\nTrailer: no\r\n\r\n";
    u32 length=0u;
    if(HTTP_HEADERS_END(ok,LEN(ok))!=LEN(ok)-4u)return 1;
    if(HTTP_HEADERS_END(missing,LEN(missing))!=0u)return 2;
    if(HTTP_HEADERS_END(empty,LEN(empty))!=LEN(empty))return 3;
    if(HTTP_CONTENT_LENGTH(ok,LEN(ok),&length)!=1||length!=4u)return 4;
    if(HTTP_CONTENT_LENGTH(missing,LEN(missing),&length)!=-1)return 5;
    if(HTTP_CONTENT_LENGTH(upper,LEN(upper),&length)!=1||
       length!=4294967295u)return 6;
    if(HTTP_CONTENT_LENGTH(overflow,LEN(overflow),&length)!=-1)return 7;
    if(HTTP_CONTENT_LENGTH(corrupt,LEN(corrupt),&length)!=-1)return 8;
    if(HTTP_CONTENT_LENGTH(empty,LEN(empty),&length)!=0)return 9;
    u8 wire[sizeof(ch)];
    for(u32 i=0;i<LEN(ch);i++)wire[i]=ch[i];
    if(HTTP_DECHUNK(wire,LEN(ch))!=4u)return 10;
    if(wire[0]!='W'||wire[1]!='i'||wire[2]!='k'||wire[3]!='i')return 11;
    for(u32 i=0;i<LEN(badsize);i++)wire[i]=0u; /* reset fixture */
    if(HTTP_DECHUNK((u8*)badsize,LEN(badsize))!=0xFFFFFFFFu)return 12;
    if(HTTP_DECHUNK((u8*)truncated,LEN(truncated))!=0xFFFFFFFFu)return 13;
    if(HTTP_DECHUNK((u8*)trailer,LEN(trailer))!=0xFFFFFFFFu)return 14;
    return 0;
}
'''


class ResponseFramingTests(unittest.TestCase):
    def test_c_framing_and_overflow_rejection(self):
        cc = shutil.which("cc")
        if cc is None:
            self.skipTest("host C compiler missing: NOT_RUN")
        with tempfile.TemporaryDirectory(prefix="br_response_") as directory:
            root = Path(directory)
            src = root / "test.c"
            binary = root / "test"
            src.write_text(FIXTURE, encoding="utf-8")
            compiled = subprocess.run(
                [cc, "-std=c11", "-O0", "-fno-builtin", "-I", str(ROOT),
                 str(src), "-o", str(binary)],
                capture_output=True, text=True, check=False, timeout=25,
            )
            self.assertEqual(compiled.returncode, 0, compiled.stderr)
            ran = subprocess.run(
                [str(binary)], capture_output=True, text=True,
                check=False, timeout=10,
            )
            self.assertEqual(ran.returncode, 0, f"rc={ran.returncode}: {ran.stderr}")

    def test_receiver_rejects_partial_frame(self):
        text = MAIN.read_text(encoding="utf-8")
        self.assertIn("if(io_error||!eof||total==0u)", text)
        self.assertIn("if(!body_off)", text)
        self.assertIn("if(is_chunked&&cl_state==1)", text)
        self.assertIn("else if(cl_state==1&&body_len!=ctx->content_len)", text)
        self.assertIn("HTTP_CONTENT_LENGTH(_NB,total,&ctx->content_len)", text)


if __name__ == "__main__":
    unittest.main()
