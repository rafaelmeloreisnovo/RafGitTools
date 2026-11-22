#include "sat_structure_l0.h"
/* No libc, host headers, heap, intrinsics, or undefined external functions. */
static raf_u32 count_bits(raf_u32 x){raf_u32 c=0;while(x){c+=x&1u;x>>=1u;}return c;}
static raf_u32 position(raf_u32 mask,raf_u32 v){raf_u32 lower=v==0u?0u:((1u<<v)-1u);return count_bits(mask&lower);}
static raf_u32 project(raf_u32 assignment,raf_u32 frommask,raf_u32 tomask){
 raf_u32 v,out=0,bit=0;
 for(v=0;v<32u;v++){
   if(frommask&(1u<<v)){if(tomask&(1u<<v)){out|=((assignment>>v)&1u)<<bit;bit++;}}
 }
 return out;
}
static int valid(const raf_sat_formula *f){
 raf_u32 j,k,t;
 if(!f||f->n>RAF_SAT_N_MAX||f->m>RAF_SAT_M_MAX)return 0;
 for(j=0;j<f->m;j++){
  const raf_sat_clause *c=&f->clauses[j];
  if(c->size<1u||c->size>3u)return 0;
  for(k=0;k<c->size;k++){
   if(c->vars[k]>=f->n||c->neg[k]>1u)return 0;
   for(t=0;t<k;t++)if(c->vars[k]==c->vars[t])return 0;
  }
 }
 return 1;
}
static int clause_value(const raf_sat_clause *c,raf_u32 mask,raf_u32 bits){
 raf_u32 k;
 for(k=0;k<c->size;k++){
  raf_u32 p=position(mask,c->vars[k]);
  raf_u32 b=(bits>>p)&1u;
  if((b^c->neg[k])!=0u)return 1;
 }
 return 0;
}
int raf_sat_check_certificate(const raf_sat_formula *f,raf_u32 assignment){
 raf_u32 j,k;
 if(!valid(f))return RAF_SAT_INVALID;
 for(j=0;j<f->m;j++){
  int ok=0;
  for(k=0;k<f->clauses[j].size;k++){
   raf_u32 v=f->clauses[j].vars[k];
   if((((assignment>>v)&1u)^f->clauses[j].neg[k])!=0u){ok=1;break;}
  }
  if(!ok)return 0;
 }
 return 1;
}
int raf_sat_eliminate(const raf_sat_formula *f,raf_sat_workspace *w,raf_sat_stats *s){
 raf_u32 used=0,j,v,i,k,u,nstate,restmask,fullmask,xbit;
 if(!valid(f)||!w||!s)return RAF_SAT_INVALID;
 for(j=0;j<f->m;j++){
  raf_sat_factor *a=&w->factors[used++];
  const raf_sat_clause *c=&f->clauses[j];raf_u32 m=0;
  for(k=0;k<c->size;k++)m|=(1u<<c->vars[k]);
  a->mask=m;a->alive=1u;nstate=1u<<count_bits(m);
  for(i=0;i<nstate;i++)a->truth[i]=(raf_u8)clause_value(c,m,i);
 }
 s->factors_created=used;
 for(v=0;v<f->n;v++){
  xbit=1u<<v;fullmask=0;
  for(j=0;j<used;j++)if(w->factors[j].alive&&(w->factors[j].mask&xbit))fullmask|=w->factors[j].mask;
  if(!fullmask)continue;
  restmask=fullmask&~xbit;u=count_bits(restmask);
  if(u>s->max_width)s->max_width=u;
  if(u>RAF_SAT_WIDTH_MAX)return RAF_SAT_WIDTH_BUDGET;
  if(used>=RAF_SAT_F_MAX)return RAF_SAT_WIDTH_BUDGET;
  {
   raf_sat_factor *newf=&w->factors[used];
   newf->mask=restmask;newf->alive=1u;
   nstate=1u<<u;
   for(i=0;i<nstate;i++){
    raf_u32 b,full=0,from=0;
    int good=0;
    for(k=0;k<f->n;k++)if(restmask&(1u<<k)){if(i&(1u<<from))full|=(1u<<k);from++;}
    for(b=0;b<2u;b++){
     int pass=1;raf_u32 bits=full|(b?xbit:0u);
     for(j=0;j<used;j++){
      raf_sat_factor *a=&w->factors[j];
      if(!a->alive||!(a->mask&xbit))continue;
      s->factor_checks++;
      if(!a->truth[project(bits,fullmask,a->mask)]){pass=0;break;}
     }
     if(pass){good=1;break;}
    }
    newf->truth[i]=(raf_u8)good;
   }
   for(j=0;j<used;j++)if(w->factors[j].alive&&(w->factors[j].mask&xbit))w->factors[j].alive=0u;
   used++;s->factors_created++;
   if(!restmask&&!newf->truth[0])return 0;
  }
 }
 for(j=0;j<used;j++)if(w->factors[j].alive&&w->factors[j].mask==0u&&!w->factors[j].truth[0])return 0;
 return 1;
}
static int dpll_search(const raf_sat_formula *f,raf_u8 *as,raf_sat_stats *s){
 raf_u32 j,k,v,loops=0;
 raf_u8 cur[RAF_SAT_N_MAX];
 if(++s->search_nodes>2000000u)return RAF_SAT_NODE_BUDGET;
 for(v=0;v<f->n;v++)cur[v]=as[v];
 for(;;){
  int changed=0;
  for(j=0;j<f->m;j++){
   const raf_sat_clause *c=&f->clauses[j];int sat=0;raf_u32 open=0,lastv=0,lastneg=0;
   s->clause_checks++;
   for(k=0;k<c->size;k++){
    raf_u32 cv=c->vars[k];
    if(cur[cv]==2u){open++;lastv=cv;lastneg=c->neg[k];}
    else if(((cur[cv]^c->neg[k])&1u)!=0u){sat=1;break;}
   }
   if(sat)continue;
   if(!open)return 0;
   if(open==1u){raf_u8 need=(raf_u8)(1u^lastneg);if(cur[lastv]==2u){cur[lastv]=need;changed=1;}}
  }
  if(!changed)break;
  if(++loops>f->n)return RAF_SAT_INVALID;
 }
 for(v=0;v<f->n;v++)if(cur[v]==2u)break;
 if(v==f->n)return 1;
 for(k=0;k<2u;k++){
  int r;raf_u8 next[RAF_SAT_N_MAX];
  for(j=0;j<f->n;j++)next[j]=cur[j];
  next[v]=(raf_u8)k;
  r=dpll_search(f,next,s);
  if(r==1||r<0)return r;
 }
 return 0;
}
int raf_sat_dpll(const raf_sat_formula *f,raf_sat_stats *s){
 raf_u8 as[RAF_SAT_N_MAX];raf_u32 i;
 if(!valid(f)||!s)return RAF_SAT_INVALID;
 for(i=0;i<f->n;i++)as[i]=2u;
 return dpll_search(f,as,s);
}
