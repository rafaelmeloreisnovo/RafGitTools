/* Freestanding in-memory deterministic tests; no I/O or libc. */
#include <stdint.h>
#include <stddef.h>
struct rq_result { uint32_t errors; uint32_t state; };
extern int rafaelia_queue_preflight(const unsigned char *, size_t, struct rq_result *);
extern int rafaelia_queue_transition(unsigned char,unsigned char);
#define RQ_STATE 32u
#define RQ_PRIVACY 64u
#define RQ_PAYLOAD 128u
#define RQ_PROMOTION 256u
#define RQ_DIGEST 512u
#define RQ_AUTHORITY 16u
static const unsigned char fixture[] =
 "{\"schema\":\"rafgittools.session-queue-preflight.v1\","
 "\"envelope_id\":\"11111111111111111111111111111111\","
 "\"session_id\":\"22222222222222222222222222222222\","
 "\"target_id\":\"33333333333333333333333333333333\","
 "\"packet_id\":\"SP08_SESSION_TO_WORK_PACKETS\","
 "\"mapa_commit\":\"ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09\","
 "\"mapa_blob\":\"c1d1d8f1692c74957f8b70968fa863f855d93cb0\","
 "\"operation\":\"PLAN_ONLY\",\"state\":\"IN\","
 "\"privacy_class\":\"UNKNOWN_REVIEW_REQUIRED\","
 "\"payload_mode\":\"NONE\",\"execution_enabled\":false,"
 "\"human_approved\":false,\"claim_allowed\":false,"
 "\"idempotency_key\":\"a55180ecbfd5fd0bb948f402d64fdf5457914e43d07993467b4fa7e82734e1bc\"}";
static size_t find(const unsigned char *b,size_t n,const char *word) {
 size_t i,j;
 for(i=0;i<n;++i){
  for(j=0;word[j] && i+j<n && b[i+j]==(unsigned char)word[j];++j){}
  if(!word[j]) return i;
 }
 return n;
}
static int check_byte_mutation(const char *needle,unsigned char replacement,uint32_t mask){
 size_t n=sizeof(fixture)-1u, p;
 unsigned char x[sizeof(fixture)];struct rq_result result;
 for(p=0;p<n;++p)x[p]=fixture[p];
 p=find(x,n,needle);
 if(p==n)return 0;
 x[p]=replacement;
 if(rafaelia_queue_preflight(x,n,&result))return 0;
 return (result.errors&mask)!=0u && result.state==0u;
}
int rafaelia_queue_native_selftest(void){
 struct rq_result r;size_t i,n=sizeof(fixture)-1u;
 unsigned char x[sizeof(fixture)];
 if(!rafaelia_queue_preflight(fixture,n,&r)||r.errors||r.state!=1u)return 1;
 if(!check_byte_mutation("IN\"",'O',RQ_STATE))return 2;
 if(!check_byte_mutation("NONE\"",'X',RQ_PAYLOAD))return 3;
 if(!check_byte_mutation("UNKNOWN_REVIEW_REQUIRED\"",'X',RQ_PRIVACY))return 4;
 if(!check_byte_mutation("ad2efc2b",'x',RQ_AUTHORITY))return 5;
 if(!check_byte_mutation("a55180ec",'x',RQ_DIGEST))return 6;
 for(i=0;i<n;++i)x[i]=fixture[i];
 i=find(x,n,"false");
 if(i==n)return 7;
 x[i]='t';x[i+1u]='r';x[i+2u]='u';x[i+3u]='e';x[i+4u]=' ';
 if(rafaelia_queue_preflight(x,n,&r)||!(r.errors&RQ_PROMOTION))return 7;
 if(rafaelia_queue_preflight(fixture,n+1u,&r))return 8; /* trailing NUL forbidden */
 if(rafaelia_queue_preflight(fixture,4097u,&r))return 9; /* bounds reject before reading */
 for(i=0;i<n;++i)x[i]=fixture[i];
 x[0]='[';
 if(rafaelia_queue_preflight(x,n,&r))return 10;
 if(rafaelia_queue_preflight((const unsigned char *)0,n,&r))return 11;
 if(rafaelia_queue_preflight(fixture,n,(struct rq_result *)0))return 12;
 if(!rafaelia_queue_transition(1,2)||!rafaelia_queue_transition(1,3)||
    !rafaelia_queue_transition(2,3)||!rafaelia_queue_transition(3,2)||
     rafaelia_queue_transition(2,1)||rafaelia_queue_transition(3,4))return 13;
 return 0;
}
