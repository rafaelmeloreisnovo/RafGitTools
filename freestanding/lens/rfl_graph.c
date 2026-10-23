#include "rfl_lens.h"
static int eq(struct rfl_span a,struct rfl_span b){unsigned k;
 if(a.len!=b.len)return 0;for(k=0;k<a.len;k++)if(a.ptr[k]!=b.ptr[k])return 0;return 1;
}
enum rfl_status rfl_candidates(const struct rfl_node *nodes,unsigned count,
 struct rfl_edge *out,unsigned cap,unsigned *written){
 unsigned i,j,a,b,n=0;struct rfl_vector scratch;
 if(!nodes||!out||!written||count>RFL_MAX_NODES||cap>RFL_MAX_EDGES)return RFL_INVALID;
 *written=0;
 for(i=0;i<count;i++){
   enum rfl_status st=rfl_vectorize(nodes+i,&scratch);if(st!=RFL_OK)return st;
   for(j=0;j<i;j++)if(eq(nodes[i].logical_id,nodes[j].logical_id))return RFL_DUPLICATE;
 }
 for(i=0;i<count;i++)for(j=i+1u;j<count;j++){
   unsigned shared=0,united;
   for(a=0;a<nodes[i].token_count;a++)for(b=0;b<nodes[j].token_count;b++)
       shared+=eq(nodes[i].tokens[a],nodes[j].tokens[b]);
   if(!shared)continue;
   if(n>=cap){*written=n;return RFL_CAPACITY;}
   united=nodes[i].token_count+nodes[j].token_count-shared;
   out[n].from=(unsigned char)i;out[n].to=(unsigned char)j;
   out[n].shared=(unsigned char)shared;out[n].united=(unsigned char)united;n++;
 }
 *written=n;return RFL_OK;
}
enum rfl_status rfl_reach(const struct rfl_edge *edges,unsigned edge_count,
 unsigned node_count,unsigned start,unsigned depth,unsigned char *seen){
 unsigned i,d;unsigned char frontier[64],next[64];
 if(!seen||(!edges&&edge_count)||node_count>64u||!node_count||start>=node_count||
   edge_count>64u||depth>3u)return RFL_INVALID;
 /* Reject malformed edges before touching any caller-owned output buffer. */
 for(i=0;i<edge_count;i++)if(edges[i].from>=node_count||edges[i].to>=node_count||
   edges[i].from==edges[i].to)return RFL_INVALID;
 for(i=0;i<node_count;i++){frontier[i]=0u;seen[i]=0u;}frontier[start]=seen[start]=1u;
 for(d=0;d<depth;d++){
   for(i=0;i<node_count;i++)next[i]=0u;
   for(i=0;i<edge_count;i++){
     unsigned a=edges[i].from,b=edges[i].to;
     if(frontier[a]&&!seen[b]){seen[b]=1u;next[b]=1u;}
     if(frontier[b]&&!seen[a]){seen[a]=1u;next[a]=1u;}
   }
   for(i=0;i<node_count;i++)frontier[i]=next[i];
 }
 return RFL_OK;
}
