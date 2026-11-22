/* RAFAELIA Session Queue deterministic freestanding preflight V1.
 * Authorial implementation of a SHA-256 correlation checksum (FIPS 180-4).
 * SHA-256 is NOT a MAC, signature, encryption, identity or authorization.
 * Host must validate JSON object types, duplicate keys, path and limits.
 */
#include "session_queue_core_v1.h"

static uint32_t same(raf_span_v1 a, const char *s, uint32_t n) {
    uint32_t i, delta=0;
    if (a.size != n || (n != 0u && a.bytes == (const uint8_t *)0)) return 0u;
    for (i=0; i<n; ++i) delta |= (uint32_t)(a.bytes[i] ^ (uint8_t)s[i]);
    return delta == 0u;
}
static uint32_t lowerhex(raf_span_v1 a, uint32_t n) {
    uint32_t i;
    if (a.size != n || a.bytes == (const uint8_t *)0) return 0u;
    for (i=0; i<n; ++i) {
        uint8_t c=a.bytes[i];
        if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return 0u;
    }
    return 1u;
}
static uint32_t packet_ok(raf_span_v1 a) {
    uint32_t i;
    if (a.size < 8u || a.size > 65u || a.bytes == (const uint8_t *)0) return 0u;
    if (a.bytes[0] != 'S' || a.bytes[1] != 'P' || a.bytes[2] < '0' ||
        a.bytes[2] > '9' || a.bytes[3] < '0' || a.bytes[3] > '9' || a.bytes[4] != '_') return 0u;
    for (i=5; i<a.size; ++i) {
        uint8_t c=a.bytes[i];
        if (!((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')) return 0u;
    }
    return 1u;
}
#define ROR(x,n) (((x) >> (n)) | ((x) << (32u-(n))))
static const uint32_t k256[64] = {
    0x428a2f98u,0x71374491u,0xb5c0fbcfu,0xe9b5dba5u,0x3956c25bu,0x59f111f1u,0x923f82a4u,0xab1c5ed5u,
    0xd807aa98u,0x12835b01u,0x243185beu,0x550c7dc3u,0x72be5d74u,0x80deb1feu,0x9bdc06a7u,0xc19bf174u,
    0xe49b69c1u,0xefbe4786u,0x0fc19dc6u,0x240ca1ccu,0x2de92c6fu,0x4a7484aau,0x5cb0a9dcu,0x76f988dau,
    0x983e5152u,0xa831c66du,0xb00327c8u,0xbf597fc7u,0xc6e00bf3u,0xd5a79147u,0x06ca6351u,0x14292967u,
    0x27b70a85u,0x2e1b2138u,0x4d2c6dfcu,0x53380d13u,0x650a7354u,0x766a0abbu,0x81c2c92eu,0x92722c85u,
    0xa2bfe8a1u,0xa81a664bu,0xc24b8b70u,0xc76c51a3u,0xd192e819u,0xd6990624u,0xf40e3585u,0x106aa070u,
    0x19a4c116u,0x1e376c08u,0x2748774cu,0x34b0bcb5u,0x391c0cb3u,0x4ed8aa4au,0x5b9cca4fu,0x682e6ff3u,
    0x748f82eeu,0x78a5636fu,0x84c87814u,0x8cc70208u,0x90befffau,0xa4506cebu,0xbef9a3f7u,0xc67178f2u
};
typedef struct { uint32_t h[8]; uint8_t block[64]; uint32_t used; uint64_t total; } sha_ctx;
static void block(sha_ctx *ctx) {
    uint32_t w[64],a,b,c,d,e,f,g,h,t1,t2,i;
    for (i=0; i<16u; ++i) {
        uint32_t j=i*4u;
        w[i]=((uint32_t)ctx->block[j]<<24)|((uint32_t)ctx->block[j+1u]<<16)|
             ((uint32_t)ctx->block[j+2u]<<8)|(uint32_t)ctx->block[j+3u];
    }
    for (i=16; i<64u; ++i) {
        uint32_t p=w[i-15u],q=w[i-2u];
        w[i]=(ROR(q,17)^ROR(q,19)^(q>>10))+w[i-7u]+(ROR(p,7)^ROR(p,18)^(p>>3))+w[i-16u];
    }
    a=ctx->h[0];b=ctx->h[1];c=ctx->h[2];d=ctx->h[3];
    e=ctx->h[4];f=ctx->h[5];g=ctx->h[6];h=ctx->h[7];
    for (i=0;i<64u;++i) {
        t1=h+(ROR(e,6)^ROR(e,11)^ROR(e,25))+((e&f)^(~e&g))+k256[i]+w[i];
        t2=(ROR(a,2)^ROR(a,13)^ROR(a,22))+((a&b)^(a&c)^(b&c));
        h=g;g=f;f=e;e=d+t1;d=c;c=b;b=a;a=t1+t2;
    }
    ctx->h[0]+=a;ctx->h[1]+=b;ctx->h[2]+=c;ctx->h[3]+=d;
    ctx->h[4]+=e;ctx->h[5]+=f;ctx->h[6]+=g;ctx->h[7]+=h;
}
static void begin(sha_ctx *x) {
    static const uint32_t init[8] = {
        0x6a09e667u,0xbb67ae85u,0x3c6ef372u,0xa54ff53au,
        0x510e527fu,0x9b05688cu,0x1f83d9abu,0x5be0cd19u};
    uint32_t i;
    for(i=0;i<8u;++i)x->h[i]=init[i];
    x->used=0u;x->total=0u;
}
static void add(sha_ctx *x, const uint8_t *p, uint32_t n) {
    uint32_t i;
    for(i=0u;i<n;++i) {
        x->block[x->used++]=p[i];++x->total;
        if (x->used==64u) {block(x);x->used=0u;}
    }
}
static void lit(sha_ctx *x, const char *p, uint32_t n) { add(x,(const uint8_t *)p,n); }
#define LIT(x,v) lit((x),(v),(uint32_t)(sizeof(v)-1u))
static void finish(sha_ctx *x,uint8_t out[32]) {
    uint32_t i;uint64_t bits=x->total*8u;
    x->block[x->used++]=0x80u;
    if(x->used>56u) {
        while(x->used<64u)x->block[x->used++]=0u;
        block(x);x->used=0u;
    }
    while(x->used<56u)x->block[x->used++]=0u;
    for(i=0;i<8u;++i)x->block[56u+i]=(uint8_t)(bits>>(56u-8u*i));
    block(x);
    for(i=0;i<8u;++i){uint32_t h=x->h[i];
        out[4u*i]=(uint8_t)(h>>24);out[4u*i+1u]=(uint8_t)(h>>16);
        out[4u*i+2u]=(uint8_t)(h>>8);out[4u*i+3u]=(uint8_t)h;
    }
}
static void field(sha_ctx *x,const char *key,uint32_t k,raf_span_v1 v,uint32_t comma) {
    if(comma)LIT(x,",");
    LIT(x,"\"");lit(x,key,k);LIT(x,"\":\"");add(x,v.bytes,v.size);LIT(x,"\"");
}
static uint32_t hash_match(const raf_queue_intent_v1 *p) {
    sha_ctx x;uint8_t sum[32];uint32_t i,diff=0u;
    static const char hex[]="0123456789abcdef";
    /* Exactly the canonical sorted 8-key JSON of the read-only Python V1. */
    if(!lowerhex(p->idempotency_key,64u))return 0u;
    begin(&x);LIT(&x,"RAFAELIA-PREFLIGHT-V1");{
        const uint8_t zero=0u;add(&x,&zero,1u);
    }
    LIT(&x,"{");
    field(&x,"envelope_id",11u,p->envelope_id,0u);
    field(&x,"mapa_blob",9u,p->mapa_blob,1u);
    field(&x,"mapa_commit",11u,p->mapa_commit,1u);
    field(&x,"operation",9u,p->operation,1u);
    field(&x,"packet_id",9u,p->packet_id,1u);
    field(&x,"schema",6u,p->schema,1u);
    field(&x,"session_id",10u,p->session_id,1u);
    field(&x,"target_id",9u,p->target_id,1u);
    LIT(&x,"}");finish(&x,sum);
    for(i=0u;i<32u;++i) {
        diff|=(uint32_t)(p->idempotency_key.bytes[2u*i] ^ (uint8_t)hex[sum[i]>>4]);
        diff|=(uint32_t)(p->idempotency_key.bytes[2u*i+1u] ^ (uint8_t)hex[sum[i]&15u]);
    }
    return diff==0u;
}
#define SPAN_SAFE(p,key) ((p)->key.size<=128u && \
    ((p)->key.size==0u || (p)->key.bytes!=(const uint8_t *)0))
static uint32_t input_ok(const raf_queue_intent_v1 *p) {
    if (p==(const raf_queue_intent_v1 *)0)return 0u;
    return SPAN_SAFE(p,schema)&&SPAN_SAFE(p,envelope_id)&&SPAN_SAFE(p,session_id)&&
           SPAN_SAFE(p,target_id)&&SPAN_SAFE(p,packet_id)&&SPAN_SAFE(p,mapa_commit)&&
           SPAN_SAFE(p,mapa_blob)&&SPAN_SAFE(p,operation)&&SPAN_SAFE(p,state)&&
           SPAN_SAFE(p,privacy_class)&&SPAN_SAFE(p,payload_mode)&&SPAN_SAFE(p,idempotency_key);
}
uint32_t raf_queue_preflight_v1(const raf_queue_intent_v1 *p) {
    uint32_t e=0u;
    if(!input_ok(p))return RAFQ_BAD_POINTER;
    if(!same(p->schema,"rafgittools.session-queue-preflight.v1",38u))e|=RAFQ_SCHEMA;
    if(!lowerhex(p->envelope_id,32u)||!lowerhex(p->session_id,32u)||!lowerhex(p->target_id,32u))e|=RAFQ_ID;
    if(!packet_ok(p->packet_id))e|=RAFQ_PACKET;
    if(!same(p->mapa_commit,"ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09",40u)||
       !same(p->mapa_blob,"c1d1d8f1692c74957f8b70968fa863f855d93cb0",40u))e|=RAFQ_PIN;
    if(!same(p->operation,"PLAN_ONLY",9u))e|=RAFQ_OPERATION;
    if(!(same(p->state,"IN",2u)||same(p->state,"GATES",5u)||same(p->state,"HOLD",4u)))e|=RAFQ_STATE;
    if(!(same(p->privacy_class,"PUBLIC_CANDIDATE",16u)||same(p->privacy_class,"PRIVATE_REQUIRED",16u)||
         same(p->privacy_class,"RESTRICTED_PERSONAL",19u)||same(p->privacy_class,"UNKNOWN_REVIEW_REQUIRED",23u)))e|=RAFQ_PRIVACY;
    if(!same(p->payload_mode,"NONE",4u))e|=RAFQ_PAYLOAD;
    if(p->execution_enabled||p->human_approved||p->claim_allowed)e|=RAFQ_PROMOTION;
    if(p->boolean_types_verified!=1u)e|=RAFQ_PARSER_TYPE;
    /* Avoid hashing malformed/untrusted strings; all inputs require full validation first. */
    if(!e && !hash_match(p))e|=RAFQ_IDEMPOTENCY;
    return e;
}
int raf_queue_transition_v1(uint32_t before,uint32_t after) {
    return (before==RAFQ_IN&&(after==RAFQ_GATES||after==RAFQ_HOLD))||
           (before==RAFQ_GATES&&after==RAFQ_HOLD)||
           (before==RAFQ_HOLD&&after==RAFQ_GATES);
}
