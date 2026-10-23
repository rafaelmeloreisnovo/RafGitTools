#include "rfl_lens.h"
static const char *const src_name[]={"", "DRIVE_PRIVATE","DRIVE_DASHBOARD","GITHUB_PRIVATE",
 "GITHUB_PUBLIC","MAPA_PUBLIC","PAPER_RESEARCH","ETHICS_RESEARCH"};
static const char *const gap_name[]={"", "TOKEN_VAZIO","MISSING_AUTHORITY","MISSING_EVIDENCE",
 "UNVERIFIED","EVIDENCED_SCOPED"};
static unsigned length(const char *s){unsigned n=0;while(s[n])n++;return n;}
static int cmp(struct rfl_span a,struct rfl_span b){
 unsigned i=0;while(i<a.len && i<b.len){if(a.ptr[i]!=b.ptr[i])return a.ptr[i]<b.ptr[i]?-1:1;i++;}
 return a.len==b.len?0:(a.len<b.len?-1:1);
}
static int valid_id(struct rfl_span s){unsigned i;
 if(!s.ptr||s.len<3u||s.len>64u||s.ptr[0]<'A'||s.ptr[0]>'Z')return 0;
 for(i=1;i<s.len;i++){unsigned c=s.ptr[i];if(!((c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='_'))return 0;}
 return 1;
}
static int valid_token(struct rfl_span s){unsigned i;
 if(!s.ptr||s.len==0u||s.len>40u)return 0;
 for(i=0;i<s.len;i++){unsigned c=s.ptr[i];if(!((c>='a'&&c<='z')||(c>='0'&&c<='9')||c=='_'||c=='-'))return 0;}
 return 1;
}
static enum rfl_status sorted(const struct rfl_node *n,struct rfl_span out[16]){
 unsigned i,j;
 if(!n||!valid_id(n->logical_id)||n->source<1u||n->source>7u||
    n->gap<1u||n->gap>5u||!n->tokens||!n->token_count||n->token_count>16u)return RFL_INVALID;
 for(i=0;i<n->token_count;i++){
   struct rfl_span t=n->tokens[i];if(!valid_token(t))return RFL_INVALID;
   j=i;while(j && cmp(out[j-1u],t)>0){out[j]=out[j-1u];j--;}out[j]=t;
 }
 for(i=1;i<n->token_count;i++)if(cmp(out[i-1u],out[i])==0)return RFL_DUPLICATE;
 return RFL_OK;
}
static void feed_lit(struct rfl_sha256 *s,const char *p){rfl_sha_update(s,p,length(p));}
static void feed_span(struct rfl_sha256 *s,struct rfl_span p){rfl_sha_update(s,p.ptr,p.len);}
enum rfl_status rfl_vectorize(const struct rfl_node *n,struct rfl_vector *out){
 struct rfl_span t[16];struct rfl_sha256 h;unsigned char hash[32];unsigned i;
 enum rfl_status status;if(!out)return RFL_INVALID;
 status=sorted(n,t);if(status!=RFL_OK)return status;
 for(i=0;i<RFL_DIM;i++)out->bins[i]=0u;
 for(i=0;i<n->token_count;i++){
   rfl_sha_init(&h);feed_lit(&h,"rafgittools.lens.token.v1\n");feed_span(&h,t[i]);rfl_sha_final(&h,hash);
   out->bins[hash[0]&63u]++;
 }
 rfl_sha_init(&h);feed_lit(&h,"rafgittools.lens.node.v1\n");feed_span(&h,n->logical_id);
 feed_lit(&h,"\n");feed_lit(&h,src_name[n->source]);feed_lit(&h,"\n");
 feed_lit(&h,gap_name[n->gap]);feed_lit(&h,"\n");
 for(i=0;i<n->token_count;i++){feed_span(&h,t[i]);feed_lit(&h,"\n");}
 rfl_sha_final(&h,out->digest);
 out->occupied=0;for(i=0;i<RFL_DIM;i++)out->occupied+=(out->bins[i]!=0u);
 return RFL_OK;
}
