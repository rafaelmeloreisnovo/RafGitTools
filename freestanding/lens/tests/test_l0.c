#include "../rfl_lens.h"
static int equal(const unsigned char *a,const unsigned char *b,unsigned n){unsigned i;for(i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
static const unsigned char empty_sha[32]={0xe3,0xb0,0xc4,0x42,0x98,0xfc,0x1c,0x14,0x9a,0xfb,0xf4,0xc8,0x99,0x6f,0xb9,0x24,0x27,0xae,0x41,0xe4,0x64,0x9b,0x93,0x4c,0xa4,0x95,0x99,0x1b,0x78,0x52,0xb8,0x55};
static const unsigned char abc_sha[32]={0xba,0x78,0x16,0xbf,0x8f,0x01,0xcf,0xea,0x41,0x41,0x40,0xde,0x5d,0xae,0x22,0x23,0xb0,0x03,0x61,0xa3,0x96,0x17,0x7a,0x9c,0xb4,0x10,0xff,0x61,0xf2,0x00,0x15,0xad};
#define SPAN(s) {(const unsigned char *)(s),sizeof(s)-1u}
int main(void){unsigned char hash[32],seen[3];unsigned n,i;struct rfl_vector a,b;
 static const struct rfl_span x[]={SPAN("custodia"),SPAN("mapa"),SPAN("etica")};
 static const struct rfl_span y[]={SPAN("etica"),SPAN("custodia"),SPAN("mapa")};
 static const struct rfl_span z[]={SPAN("mapa"),SPAN("paper")};
 static const struct rfl_node nodes[]={ {SPAN("NODE_001"),RFL_DRIVE_PRIVATE,RFL_TOKEN_VAZIO,x,3u},
   {SPAN("NODE_002"),RFL_DRIVE_PRIVATE,RFL_TOKEN_VAZIO,y,3u},
   {SPAN("NODE_003"),RFL_MAPA_PUBLIC,RFL_UNVERIFIED,z,2u} };
 struct rfl_edge edges[64];
 static const unsigned char expected_node_sha[32]={0xa2,0xef,0x31,0x9e,0xc7,0xd2,0x46,0x10,0xe7,0x74,0xfd,0xec,0x4e,0xf9,0xb8,0x6c,0xa2,0xed,0x31,0x62,0xf2,0xae,0xa4,0xb6,0x08,0x8c,0xef,0xbe,0x99,0x2a,0x32,0xbd};
 rfl_sha("",0,hash);if(!equal(hash,empty_sha,32))return 10;
 rfl_sha("abc",3,hash);if(!equal(hash,abc_sha,32))return 11;
 if(rfl_vectorize(nodes,&a)!=RFL_OK)return 12;
 if(!equal(a.digest,expected_node_sha,32))return 21;
 {struct rfl_node q=nodes[0];q.tokens=y;if(rfl_vectorize(&q,&b)!=RFL_OK)return 13;}
 if(!equal(a.digest,b.digest,32)||!equal(a.bins,b.bins,64u))return 14;
 n=0;for(i=0;i<64;i++)n+=a.bins[i];if(n!=3u)return 15;
 if(rfl_candidates(nodes,3u,edges,64u,&n)!=RFL_OK||n!=3u)return 16;
 if(rfl_reach(edges,n,3u,0u,1u,seen)!=RFL_OK||!seen[1]||!seen[2])return 17;
 if(rfl_reach(edges,n,3u,0u,4u,seen)!=RFL_INVALID)return 18;
 {static const struct rfl_span bad[]={SPAN("bad token")};struct rfl_node q=nodes[0];q.tokens=bad;q.token_count=1;
 if(rfl_vectorize(&q,&b)!=RFL_INVALID)return 19;}
 {static const struct rfl_span dup[]={SPAN("mapa"),SPAN("mapa")};struct rfl_node q=nodes[0];q.tokens=dup;q.token_count=2;
 if(rfl_vectorize(&q,&b)!=RFL_DUPLICATE)return 20;}
 return 0;
}
