#!/usr/bin/env python3
"""Host-only deterministic L1 edit, paint and canary tests; not Android execution."""
import ctypes
import pathlib
import re
import subprocess
import tempfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
raw=(ROOT/'src/layout.inc').read_text(encoding='utf-8')
rows=re.findall(r'^\.ascii "([^"]*)"',raw,re.M)
last=re.search(r'^\.byte (.+)$',raw,re.M)
assert len(rows)==4 and all(len(z)==10 for z in rows) and last
codes=''.join(rows).encode('ascii')+bytes(int(x,0) for x in last.group(1).split(','))
assert len(codes)==50 and codes[40:]==bytes([8,9,32,32,32,32,10,64,35,63])
with tempfile.TemporaryDirectory() as folder:
    tmp=pathlib.Path(folder)
    (tmp/'host_probe.c').write_text(
      '#include "rafkbd_panel.h"\n'
      'static const unsigned char keys[50]={' + ','.join(str(n) for n in codes) + '};\n'
      'raf_u32 rafkbd_l0_probe(raf_u32 x,raf_u32 y){'
      'if(x>=320u||y>=160u)return 0xffffffffu;'
      'return keys[(y>>5)*10u+(x>>5)];}\n')
    so=tmp/'l1.so'
    subprocess.run(['clang','-shared','-fPIC','-O2','-std=c11','-Wall','-Wextra','-Werror',
       '-ffreestanding','-fno-builtin','-I'+str(ROOT/'src'),
       str(ROOT/'src/rafkbd_panel.c'),str(tmp/'host_probe.c'),'-o',str(so)],check=True)
    lib=ctypes.CDLL(str(so))
    u32=ctypes.c_uint32
    data=(ctypes.c_ubyte*256)()
    n=u32(0)
    lib.rafkbd_l1_edit.argtypes=[ctypes.POINTER(ctypes.c_ubyte),u32,ctypes.POINTER(u32),u32]
    lib.rafkbd_l1_paint.argtypes=[ctypes.POINTER(u32),u32,u32,u32,ctypes.POINTER(ctypes.c_ubyte),u32]
    checks=0
    for key in (ord('a'),ord('b'),8,ord('c'),9,10,0xffffffff):
        lib.rafkbd_l1_edit(data,256,ctypes.byref(n),key)
        checks+=1
    assert bytes(data[:n.value])==b'ac \n';checks+=1
    original=n.value
    lib.rafkbd_l1_edit(data,256,ctypes.byref(n),0)
    assert n.value==original;checks+=1
    n.value=256
    lib.rafkbd_l1_edit(data,256,ctypes.byref(n),ord('x'))
    assert n.value==256;checks+=1
    n.value=257
    lib.rafkbd_l1_edit(data,256,ctypes.byref(n),8)
    assert n.value==257;checks+=1
    n.value=4
    picture=(u32*(640*640))()
    lib.rafkbd_l1_paint(picture,640,640,640,data,n.value)
    assert picture[0]!=picture[640*639];checks+=1
    assert picture[640*480]!=picture[640*100];checks+=1
    assert any(p==0xffe3e7ef for p in picture);checks+=1
    guarded=(u32*(640*640+2))()
    guarded[0]=0xabcdef01;guarded[-1]=0x12345678
    lib.rafkbd_l1_paint(ctypes.cast(ctypes.byref(guarded,4),ctypes.POINTER(u32)),
                        640,640,640,data,4)
    assert guarded[0]==0xabcdef01 and guarded[-1]==0x12345678;checks+=1
    print(f'PASS: {checks} host L1 edit/raster/bounds checks; ARM CPU/Android NOT_RUN')
