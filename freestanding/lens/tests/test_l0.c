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
 /* Fail closed: invalid edges must not overwrite the caller-owned result. */
 {struct rfl_edge malformed={0u,3u,1u,2u};seen[0]=0xA5u;seen[1]=0x5Au;seen[2]=0x3Cu;
  if(rfl_reach(&malformed,1u,3u,0u,1u,seen)!=RFL_INVALID)return 22;
  if(seen[0]!=0xA5u||seen[1]!=0x5Au||seen[2]!=0x3Cu)return 23;}
 /* Bounded edge list is never silently truncated. */
 {struct rfl_edge few[1];unsigned written=99u;
  if(rfl_candidates(nodes,3u,few,1u,&written)!=RFL_CAPACITY||written!=1u)return 24;}
 /* Duplicate logical IDs never create ambiguous provenance. */
 {struct rfl_node duplicate[2]={nodes[0],nodes[0]};unsigned written=0u;
  if(rfl_candidates(duplicate,2u,edges,64u,&written)!=RFL_DUPLICATE)return 25;}
 /* SHA-256 padding across two blocks and split update match authoritative KAT. */
 {const unsigned char message[]="abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq";
  const unsigned char known[32]={0x24,0x8d,0x6a,0x61,0xd2,0x06,0x38,0xb8,0xe5,0xc0,0x26,0x93,0x0c,0x3e,0x60,0x39,
   0xa3,0x3c,0xe4,0x59,0x64,0xff,0x21,0x67,0xf6,0xec,0xed,0xd4,0x19,0xdb,0x06,0xc1};
  struct rfl_sha256 ctx;
  rfl_sha_init(&ctx);rfl_sha_update(&ctx,message,19u);
  rfl_sha_update(&ctx,message+19u,(unsigned)sizeof(message)-1u-19u);
  rfl_sha_final(&ctx,hash);
  if(!equal(hash,known,32u))return 26;
 }
 {static const struct rfl_span bad[]={SPAN("bad token")};struct rfl_node q=nodes[0];q.tokens=bad;q.token_count=1;
 if(rfl_vectorize(&q,&b)!=RFL_INVALID)return 19;}
 {static const struct rfl_span dup[]={SPAN("mapa"),SPAN("mapa")};struct rfl_node q=nodes[0];q.tokens=dup;q.token_count=2;
 if(rfl_vectorize(&q,&b)!=RFL_DUPLICATE)return 20;}
 return 0;
}
