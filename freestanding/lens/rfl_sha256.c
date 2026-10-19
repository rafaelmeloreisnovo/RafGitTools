#include "rfl_lens.h"
typedef unsigned W;
#define RR(x,n) (((x)>>(n))|((x)<<(32u-(n))))
static const W k[64]={
0x428a2f98u,0x71374491u,0xb5c0fbcfu,0xe9b5dba5u,0x3956c25bu,0x59f111f1u,0x923f82a4u,0xab1c5ed5u,
0xd807aa98u,0x12835b01u,0x243185beu,0x550c7dc3u,0x72be5d74u,0x80deb1feu,0x9bdc06a7u,0xc19bf174u,
0xe49b69c1u,0xefbe4786u,0x0fc19dc6u,0x240ca1ccu,0x2de92c6fu,0x4a7484aau,0x5cb0a9dcu,0x76f988dau,
0x983e5152u,0xa831c66du,0xb00327c8u,0xbf597fc7u,0xc6e00bf3u,0xd5a79147u,0x06ca6351u,0x14292967u,
0x27b70a85u,0x2e1b2138u,0x4d2c6dfcu,0x53380d13u,0x650a7354u,0x766a0abbu,0x81c2c92eu,0x92722c85u,
0xa2bfe8a1u,0xa81a664bu,0xc24b8b70u,0xc76c51a3u,0xd192e819u,0xd6990624u,0xf40e3585u,0x106aa070u,
0x19a4c116u,0x1e376c08u,0x2748774cu,0x34b0bcb5u,0x391c0cb3u,0x4ed8aa4au,0x5b9cca4fu,0x682e6ff3u,
0x748f82eeu,0x78a5636fu,0x84c87814u,0x8cc70208u,0x90befffau,0xa4506cebu,0xbef9a3f7u,0xc67178f2u};
static void process(struct rfl_sha256 *s) {
 W w[64],a,b,c,d,e,f,g,h,t1,t2;
 unsigned i;
 for(i=0;i<16u;i++){const unsigned char *p=s->block+(i<<2);
   w[i]=((W)p[0]<<24)|((W)p[1]<<16)|((W)p[2]<<8)|(W)p[3];}
 for(i=16;i<64u;i++) {W x=w[i-15u],y=w[i-2u];
   W q=RR(x,7)^RR(x,18)^(x>>3),r=RR(y,17)^RR(y,19)^(y>>10);
   w[i]=w[i-16u]+q+w[i-7u]+r;}
 a=s->h[0];b=s->h[1];c=s->h[2];d=s->h[3];e=s->h[4];f=s->h[5];g=s->h[6];h=s->h[7];
 for(i=0;i<64u;i++){
   t1=h+(RR(e,6)^RR(e,11)^RR(e,25))+((e&f)^((~e)&g))+k[i]+w[i];
   t2=(RR(a,2)^RR(a,13)^RR(a,22))+((a&b)^(a&c)^(b&c));
   h=g;g=f;f=e;e=d+t1;d=c;c=b;b=a;a=t1+t2;
 }
 s->h[0]+=a;s->h[1]+=b;s->h[2]+=c;s->h[3]+=d;
 s->h[4]+=e;s->h[5]+=f;s->h[6]+=g;s->h[7]+=h;
}
void rfl_sha_init(struct rfl_sha256 *s){
 static const W init[8]={0x6a09e667u,0xbb67ae85u,0x3c6ef372u,0xa54ff53au,
 0x510e527fu,0x9b05688cu,0x1f83d9abu,0x5be0cd19u};
 unsigned i; for(i=0;i<8u;i++)s->h[i]=init[i];
 s->bit_low=0;s->bit_high=0;s->used=0;
}
void rfl_sha_update(struct rfl_sha256 *s,const void *bytes,unsigned count){
 const unsigned char *p=(const unsigned char *)bytes;unsigned i=0,add=count<<3;
 W was=s->bit_low;s->bit_low+=add;s->bit_high+=(count>>29)+(s->bit_low<was);
 while(i<count){s->block[s->used++]=p[i++];if(s->used==64u){process(s);s->used=0;}}
}
void rfl_sha_final(struct rfl_sha256 *s,unsigned char out[32]){
 unsigned hi=s->bit_high,lo=s->bit_low,i;
 s->block[s->used++]=0x80u;
 if(s->used>56u){while(s->used<64u)s->block[s->used++]=0u;process(s);s->used=0;}
 while(s->used<56u)s->block[s->used++]=0u;
 for(i=0;i<4u;i++){s->block[56u+i]=(unsigned char)(hi>>(24u-8u*i));
 s->block[60u+i]=(unsigned char)(lo>>(24u-8u*i));}
 process(s);
 for(i=0;i<8u;i++){W x=s->h[i];unsigned j;for(j=0;j<4u;j++)out[i*4u+j]=(unsigned char)(x>>(24u-8u*j));}
}
void rfl_sha(const void *bytes,unsigned count,unsigned char out[32]){
 struct rfl_sha256 s;rfl_sha_init(&s);rfl_sha_update(&s,bytes,count);rfl_sha_final(&s,out);
}
