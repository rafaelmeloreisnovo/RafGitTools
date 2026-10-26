/* RAFAELIA session preflight: freestanding, bounded, fail-closed.
 * No libc, heap, syscall, mutable global state, I/O or hosted dependencies.
 * Draft source; governing license must follow the repository, with provenance.
 */
#include <stdint.h>
#include <stddef.h>

_Static_assert(sizeof(uint32_t) == 4, "requires 32-bit uint32_t");
#define RQ_FIELDS 15u
#define RQ_LIMIT 4096u
#define RQ_VALUE 129u
#define RQ_INPUT 1u
#define RQ_SCHEMA 2u
#define RQ_ID 4u
#define RQ_PACKET 8u
#define RQ_AUTHORITY 16u
#define RQ_STATE 32u
#define RQ_PRIVACY 64u
#define RQ_PAYLOAD 128u
#define RQ_PROMOTION 256u
#define RQ_DIGEST 512u
#define RQ_HOLD 0u
#define RQ_PREFLIGHT_ONLY 1u

struct rq_result { uint32_t errors; uint32_t state; };
struct rq_field { unsigned char text[RQ_VALUE]; uint32_t size; uint32_t kind; uint32_t present; };
struct rq_scan { const unsigned char *in; size_t size; size_t at; };

enum { F_SCHEMA, F_ENVELOPE, F_SESSION, F_TARGET, F_PACKET,
       F_COMMIT, F_BLOB, F_OPERATION, F_STATE, F_PRIVACY, F_PAYLOAD,
       F_EXECUTION, F_HUMAN, F_CLAIM, F_IDEMPOTENCY };
static const char *const rq_names[RQ_FIELDS] = {
 "schema", "envelope_id", "session_id", "target_id", "packet_id",
 "mapa_commit", "mapa_blob", "operation", "state", "privacy_class",
 "payload_mode", "execution_enabled", "human_approved", "claim_allowed",
 "idempotency_key"
};
static const char rq_commit[] = "ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09";
static const char rq_blob[] = "c1d1d8f1692c74957f8b70968fa863f855d93cb0";

static size_t rq_length(const char *p) { size_t i = 0; while (p[i]) ++i; return i; }
static int rq_equal(const struct rq_field *f, const char *s) {
 size_t n = rq_length(s), i;
 if (f->kind != 1u || f->size != n) return 0;
 for (i=0; i<n; ++i) if (f->text[i] != (unsigned char)s[i]) return 0;
 return 1;
}
static int rq_hex(unsigned char c) {
 if (c >= '0' && c <= '9') return (int)(c-'0');
 if (c >= 'a' && c <= 'f') return (int)(c-'a'+10);
 if (c >= 'A' && c <= 'F') return (int)(c-'A'+10);
 return -1;
}
static void rq_ws(struct rq_scan *p) {
 while (p->at < p->size) {
  unsigned char c=p->in[p->at];
  if (c!=' ' && c!='\n' && c!='\r' && c!='\t') break;
  ++p->at;
 }
}
static int rq_string(struct rq_scan *p, struct rq_field *out) {
 if (p->at >= p->size || p->in[p->at++] != '"') return 0;
 out->size=0; out->kind=1;
 while (p->at < p->size) {
  unsigned char c=p->in[p->at++];
  if (c=='"') return 1;
  if (c=='\\') {
   if (p->at >= p->size) return 0;
   c=p->in[p->at++];
   if (c=='u') {
    uint32_t u=0, k;
    if (p->size-p->at < 4u) return 0;
    for (k=0; k<4; ++k) {
     int h=rq_hex(p->in[p->at++]); if (h<0) return 0;
     u=(u<<4) | (uint32_t)h;
    }
    if (u>127u) return 0;
    c=(unsigned char)u;
   } else if (c=='"' || c=='\\' || c=='/') {
    /* decoded ASCII */
   } else if (c=='b') c='\b';
   else if (c=='f') c='\f';
   else if (c=='n') c='\n';
   else if (c=='r') c='\r';
   else if (c=='t') c='\t';
   else return 0;
  }
  if (c<32u || c>=128u || out->size>=RQ_VALUE-1u) return 0;
  out->text[out->size++]=c;
 }
 return 0;
}
static int rq_literal(struct rq_scan *p, const char *lit) {
 size_t i, n=rq_length(lit);
 if (p->size-p->at<n) return 0;
 for (i=0;i<n;++i) if (p->in[p->at+i]!=(unsigned char)lit[i]) return 0;
 p->at+=n;return 1;
}
static int rq_parse(const unsigned char *input, size_t length,
                    struct rq_field fields[RQ_FIELDS]) {
 struct rq_scan p={input,length,0};
 unsigned int count=0, i;
 struct rq_field key;
 for (i=0;i<RQ_FIELDS;++i) {
  ((volatile struct rq_field *)fields)[i].size=0;
  ((volatile struct rq_field *)fields)[i].kind=0;
  ((volatile struct rq_field *)fields)[i].present=0;
 }
 rq_ws(&p);
 if (p.at>=p.size || p.in[p.at++]!='{') return 0;
 rq_ws(&p);
 if (p.at<p.size && p.in[p.at]=='}') return 0;
 while (p.at < p.size && count<RQ_FIELDS) {
  int found=-1;
  if (!rq_string(&p,&key)) return 0;
  for (i=0; i<RQ_FIELDS; ++i) if (rq_equal(&key,rq_names[i])) {found=(int)i;break;}
  if (found<0 || fields[found].present) return 0;
  rq_ws(&p);
  if (p.at>=p.size || p.in[p.at++]!=':') return 0;
  rq_ws(&p);
  if (p.at>=p.size) return 0;
  if (p.in[p.at]=='"') {
   if (!rq_string(&p,&fields[found])) return 0;
  } else if (rq_literal(&p,"false")) fields[found].kind=2u;
  else if (rq_literal(&p,"true")) fields[found].kind=3u;
  else return 0;
  fields[found].present=1u;
  ++count;rq_ws(&p);
  if (p.at>=p.size) return 0;
  if (p.in[p.at]=='}') {++p.at;break;}
  if (p.in[p.at++]!=',') return 0;
  rq_ws(&p);
 }
 rq_ws(&p);
 return count==RQ_FIELDS && p.at==p.size;
}
static int rq_opaque(const struct rq_field *f) {
 unsigned int i;
 if (f->kind!=1u || f->size!=32u) return 0;
 for (i=0;i<32u;++i) {
  unsigned char c=f->text[i];
  if (!((c>='0'&&c<='9')||(c>='a'&&c<='f'))) return 0;
 }
 return 1;
}
static int rq_packet(const struct rq_field *f) {
 unsigned int i;
 if (f->kind!=1u || f->size<8u || f->size>65u) return 0;
 if (f->text[0]!='S'||f->text[1]!='P'||f->text[2]<'0'||f->text[2]>'9'||
     f->text[3]<'0'||f->text[3]>'9'||f->text[4]!='_') return 0;
 for(i=5;i<f->size;++i) {
  unsigned char c=f->text[i];
  if (!((c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='_')) return 0;
 }
 return 1;
}

/* Streaming SHA-256 defined by FIPS 180-4; original local implementation. */
struct rq_sha { uint32_t h[8]; unsigned char b[64]; uint32_t used; uint32_t n; };
static const uint32_t rq_k[64] = {
 0x428a2f98u,0x71374491u,0xb5c0fbcfu,0xe9b5dba5u,0x3956c25bu,0x59f111f1u,0x923f82a4u,0xab1c5ed5u,
 0xd807aa98u,0x12835b01u,0x243185beu,0x550c7dc3u,0x72be5d74u,0x80deb1feu,0x9bdc06a7u,0xc19bf174u,
 0xe49b69c1u,0xefbe4786u,0x0fc19dc6u,0x240ca1ccu,0x2de92c6fu,0x4a7484aau,0x5cb0a9dcu,0x76f988dau,
 0x983e5152u,0xa831c66du,0xb00327c8u,0xbf597fc7u,0xc6e00bf3u,0xd5a79147u,0x06ca6351u,0x14292967u,
 0x27b70a85u,0x2e1b2138u,0x4d2c6dfcu,0x53380d13u,0x650a7354u,0x766a0abbu,0x81c2c92eu,0x92722c85u,
 0xa2bfe8a1u,0xa81a664bu,0xc24b8b70u,0xc76c51a3u,0xd192e819u,0xd6990624u,0xf40e3585u,0x106aa070u,
 0x19a4c116u,0x1e376c08u,0x2748774cu,0x34b0bcb5u,0x391c0cb3u,0x4ed8aa4au,0x5b9cca4fu,0x682e6ff3u,
 0x748f82eeu,0x78a5636fu,0x84c87814u,0x8cc70208u,0x90befffau,0xa4506cebu,0xbef9a3f7u,0xc67178f2u
};
static uint32_t rq_rotr(uint32_t v,uint32_t bits){return (v>>bits)|(v<<(32u-bits));}
static void rq_block(struct rq_sha *s) {
 uint32_t w[64],v[8],i,t1,t2;
 for(i=0;i<16u;++i) {
  unsigned j=i*4u;
  w[i]=((uint32_t)s->b[j]<<24)|((uint32_t)s->b[j+1u]<<16)|
       ((uint32_t)s->b[j+2u]<<8)|s->b[j+3u];
 }
 for(i=16;i<64u;++i){
  uint32_t x=w[i-15u],y=w[i-2u];
  w[i]=(rq_rotr(y,17u)^rq_rotr(y,19u)^(y>>10)) + w[i-7u] +
       (rq_rotr(x,7u)^rq_rotr(x,18u)^(x>>3)) + w[i-16u];
 }
 for(i=0;i<8u;++i) v[i]=s->h[i];
 for(i=0;i<64u;++i){
  t1=v[7]+(rq_rotr(v[4],6u)^rq_rotr(v[4],11u)^rq_rotr(v[4],25u))+
      ((v[4]&v[5])^(~v[4]&v[6]))+rq_k[i]+w[i];
  t2=(rq_rotr(v[0],2u)^rq_rotr(v[0],13u)^rq_rotr(v[0],22u))+
      ((v[0]&v[1])^(v[0]&v[2])^(v[1]&v[2]));
  v[7]=v[6];v[6]=v[5];v[5]=v[4];v[4]=v[3]+t1;
  v[3]=v[2];v[2]=v[1];v[1]=v[0];v[0]=t1+t2;
 }
 for(i=0;i<8u;++i) s->h[i]+=v[i];
}
static void rq_sha_init(struct rq_sha *s){
 static const uint32_t initial[8]={0x6a09e667u,0xbb67ae85u,0x3c6ef372u,0xa54ff53au,
   0x510e527fu,0x9b05688cu,0x1f83d9abu,0x5be0cd19u};
 uint32_t i;
 for(i=0;i<8u;++i)s->h[i]=initial[i];
 s->used=0;s->n=0;
}
static void rq_sha_byte(struct rq_sha *s,unsigned char c){
 s->b[s->used++]=c;s->n++;
 if(s->used==64u){rq_block(s);s->used=0;}
}
static void rq_sha_text(struct rq_sha *s,const char *str){
 size_t i=0;while(str[i])rq_sha_byte(s,(unsigned char)str[i++]);
}
static void rq_sha_final(struct rq_sha *s,unsigned char out[32]){
 uint32_t bits=s->n*8u;
 uint32_t i;
 rq_sha_byte(s,0x80u);
 while(s->used != 56u)rq_sha_byte(s,0);
 for(i=0;i<4u;++i)rq_sha_byte(s,0u);
 for(i=0;i<4u;++i)rq_sha_byte(s,(unsigned char)(bits>>((3u-i)*8u)));
 for(i=0;i<8u;++i){
  out[i*4u]=(unsigned char)(s->h[i]>>24);
  out[i*4u+1u]=(unsigned char)(s->h[i]>>16);
  out[i*4u+2u]=(unsigned char)(s->h[i]>>8);
  out[i*4u+3u]=(unsigned char)s->h[i];
 }
}
static int rq_idempotency(struct rq_field fields[RQ_FIELDS]) {
 static const unsigned char order[8]={F_ENVELOPE,F_BLOB,F_COMMIT,F_OPERATION,
                                       F_PACKET,F_SCHEMA,F_SESSION,F_TARGET};
 static const char hex[]="0123456789abcdef";
 struct rq_sha sha;
 unsigned char digest[32];
 uint32_t i,j,miss=0;
 const struct rq_field *key=&fields[F_IDEMPOTENCY];
 if(key->kind!=1u || key->size!=64u) return 0;
 rq_sha_init(&sha);
 rq_sha_text(&sha,"RAFAELIA-PREFLIGHT-V1");rq_sha_byte(&sha,0u);
 rq_sha_byte(&sha,'{');
 for(i=0;i<8u;++i){
  const struct rq_field *f=&fields[order[i]];
  if(i)rq_sha_byte(&sha,',');
  rq_sha_byte(&sha,'"');rq_sha_text(&sha,rq_names[order[i]]);
  rq_sha_byte(&sha,'"');rq_sha_byte(&sha,':');rq_sha_byte(&sha,'"');
  for(j=0;j<f->size;++j)rq_sha_byte(&sha,f->text[j]);
  rq_sha_byte(&sha,'"');
 }
 rq_sha_byte(&sha,'}');rq_sha_final(&sha,digest);
 for(i=0;i<32u;++i){
  miss |= (uint32_t)(key->text[2u*i] ^ (unsigned char)hex[digest[i]>>4]);
  miss |= (uint32_t)(key->text[2u*i+1u] ^ (unsigned char)hex[digest[i]&15u]);
 }
 return miss==0;
}

int rafaelia_queue_preflight(const unsigned char *input,size_t length,
                             struct rq_result *result) {
 struct rq_field f[RQ_FIELDS];
 uint32_t e=0;
 if(!result) return 0;
 result->errors=RQ_INPUT;result->state=RQ_HOLD;
 if(!input || length>RQ_LIMIT || !rq_parse(input,length,f)) return 0;
 if(!rq_equal(&f[F_SCHEMA],"rafgittools.session-queue-preflight.v1")) e|=RQ_SCHEMA;
 if(!rq_opaque(&f[F_ENVELOPE])||!rq_opaque(&f[F_SESSION])||!rq_opaque(&f[F_TARGET])) e|=RQ_ID;
 if(!rq_packet(&f[F_PACKET])) e|=RQ_PACKET;
 if(!rq_equal(&f[F_COMMIT],rq_commit)||!rq_equal(&f[F_BLOB],rq_blob)) e|=RQ_AUTHORITY;
 if(!rq_equal(&f[F_OPERATION],"PLAN_ONLY") ||
    !(rq_equal(&f[F_STATE],"IN")||rq_equal(&f[F_STATE],"GATES")||rq_equal(&f[F_STATE],"HOLD"))) e|=RQ_STATE;
 if(!(rq_equal(&f[F_PRIVACY],"PUBLIC_CANDIDATE")||rq_equal(&f[F_PRIVACY],"PRIVATE_REQUIRED")||
      rq_equal(&f[F_PRIVACY],"RESTRICTED_PERSONAL")||rq_equal(&f[F_PRIVACY],"UNKNOWN_REVIEW_REQUIRED"))) e|=RQ_PRIVACY;
 if(!rq_equal(&f[F_PAYLOAD],"NONE")) e|=RQ_PAYLOAD;
 if(f[F_EXECUTION].kind!=2u||f[F_HUMAN].kind!=2u||f[F_CLAIM].kind!=2u) e|=RQ_PROMOTION;
 /* A malformed semantic field must not provide an apparent valid digest. */
 if(e || !rq_idempotency(f)) e|=RQ_DIGEST;
 result->errors=e;
 result->state=e?RQ_HOLD:RQ_PREFLIGHT_ONLY;
 return e==0;
}

int rafaelia_queue_transition(unsigned char from,unsigned char to){
 /* IN=1, GATES=2, HOLD=3. The primitive never executes transitions. */
 return (from==1u && (to==2u||to==3u)) ||
        (from==2u && to==3u) || (from==3u && to==2u);
}
