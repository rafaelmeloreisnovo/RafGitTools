#include "rafkbd_panel.h"
/* Independently hand-drawn 5x7 glyph strokes. Every row is a five-bit mask.
 * No third-party font or imported code, OS access, heap, system include. */
static const unsigned char raf_glyph[128][7] = {
 ['0']={14,17,19,21,25,17,14},['1']={4,12,4,4,4,4,14},
 ['2']={14,17,1,2,4,8,31},['3']={30,1,1,14,1,1,30},
 ['4']={2,6,10,18,31,2,2},['5']={31,16,16,30,1,1,30},
 ['6']={14,16,16,30,17,17,14},['7']={31,1,2,4,8,8,8},
 ['8']={14,17,17,14,17,17,14},['9']={14,17,17,15,1,1,14},
 ['A']={14,17,17,31,17,17,17},['B']={30,17,17,30,17,17,30},
 ['C']={15,16,16,16,16,16,15},['D']={30,17,17,17,17,17,30},
 ['E']={31,16,16,30,16,16,31},['F']={31,16,16,30,16,16,16},
 ['G']={15,16,16,23,17,17,15},['H']={17,17,17,31,17,17,17},
 ['I']={14,4,4,4,4,4,14},['J']={7,2,2,2,18,18,12},
 ['K']={17,18,20,24,20,18,17},['L']={16,16,16,16,16,16,31},
 ['M']={17,27,21,21,17,17,17},['N']={17,25,21,19,17,17,17},
 ['O']={14,17,17,17,17,17,14},['P']={30,17,17,30,16,16,16},
 ['Q']={14,17,17,17,21,18,13},['R']={30,17,17,30,20,18,17},
 ['S']={15,16,16,14,1,1,30},['T']={31,4,4,4,4,4,4},
 ['U']={17,17,17,17,17,17,14},['V']={17,17,17,17,10,10,4},
 ['W']={17,17,17,21,21,21,10},['X']={17,17,10,4,10,17,17},
 ['Y']={17,17,10,4,4,4,4},['Z']={31,1,2,4,8,16,31},
 ['.']={0,0,0,0,0,12,12},[',']={0,0,0,0,6,6,4},
 ['/']={1,1,2,4,8,16,16},[';']={0,12,12,0,12,12,8},
 ['?']={14,17,1,2,4,0,4},['@']={14,17,23,21,23,16,14},
 ['#']={10,31,10,10,31,10,0},['_']={0,0,0,0,0,0,31},
 ['+']={0,4,4,31,4,4,0},['-']={0,0,0,31,0,0,0},
 ['<']={2,4,8,16,8,4,2},['>']={8,4,2,1,2,4,8}
};
/* A small upper-bounded unsigned quotient: no __aeabi_uidiv runtime calls. */
static raf_u32 raf_quot(raf_u32 n,raf_u32 d,raf_u32 *rem) {
 raf_u32 q=0u;
 if(d==0u){*rem=0u;return 0u;}
again:
 if(n<d){*rem=n;return q;}
 n-=d;
 ++q;
 goto again;
}
void rafkbd_l1_edit(unsigned char *buf,raf_u32 cap,raf_u32 *length,raf_u32 key) {
 raf_u32 n;
 if(buf==(void *)0 || length==(void *)0 || cap==0u)return;
 n=*length;
 if(n>cap || key==0xffffffffu)return;
 if(key==8u){if(n!=0u)*length=n-1u;return;}
 if(key==9u)key=32u;
 if(key!=10u && (key<32u || key>126u))return;
 if(n==cap)return;
 buf[n]=(unsigned char)key;
 *length=n+1u;
}
void rafkbd_l1_paint(raf_u32 *dst,raf_u32 w,raf_u32 h,raf_u32 stride,const unsigned char *txt,raf_u32 n) {
 raf_u32 x=0u,y=0u,c,r,px,py,g,fg,bg,code,col,row,xx,yy,rem;
 if(dst==(void *)0 || txt==(void *)0 || w<320u || h<320u || w>65535u || h>65535u || stride<w)return;
 if(n>256u)n=256u;
each_row:
 if(y>=h)return;
 x=0u;
each_pixel:
 if(x>=w){++y;goto each_row;}
 bg=0xff0d1420u; fg=0xffe3e7efu;
 if(y>=h/2u){
   col=raf_quot(x*10u,w,&px);
   row=raf_quot((y-h/2u)*5u,h-h/2u,&py);
   bg=((col+row*10u)&1u)?0xff203446u:0xff293b4bu;
   if(px<w/85u || py<(h/2u)/40u)bg=0xff63839bu;
   code=rafkbd_l0_probe(col*32u+16u,row*32u+16u);
   if(code==8u)code='<';
   if(code==9u)code='>';
   if(code==10u)code='+';
   if(code==32u)code='_';
   if(code>='a' && code<='z')code-=32u;
   xx=raf_quot(px*12u,w,&rem);
   yy=raf_quot(py*14u,h/2u,&rem);
   if(xx>=3u && xx<=7u && yy>=3u && yy<=9u && code<128u){
     g=raf_glyph[code][yy-3u];
     if(((g>>(7u-xx))&1u)!=0u)bg=fg;
   }
 } else {
   if(y<14u)bg=0xff29495fu;
   if(y>=24u && x>=16u){
     c=(x-16u)/12u;r=(y-24u)/16u;
     xx=((x-16u)%12u)/2u;yy=((y-24u)%16u)/2u;
     code=r*16u+c;
     if(c<16u && code<n && xx<5u && yy<7u){
       g=(raf_u32)txt[code];
       if(g>='a' && g<='z')g-=32u;
       if(g<128u && ((raf_glyph[g][yy]>>(4u-xx))&1u)!=0u)bg=fg;
     }
   }
 }
 dst[y*stride+x]=bg;
 ++x;goto each_pixel;
}
