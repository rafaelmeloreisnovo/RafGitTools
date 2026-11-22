/* SPDX-License-Identifier: GPL-3.0-or-later */
#include "atlas_core.h"
static unsigned lower_ascii(unsigned c){return(c>='A'&&c<='Z')?c+32u:c;}
static int ext_eq(const char*s,unsigned n,const char*e,unsigned m){
  unsigned i;
  if(n!=m)return 0;
  for(i=0u;i<n;++i)if(lower_ascii((unsigned char)s[i])!=(unsigned char)e[i])return 0;
  return 1;
}
atlas_traits atlas_classify(const char*name,unsigned len,int dir){
  atlas_traits t={8u,0u,len};unsigned i;
  if(dir){t.kind=1u;return t;}
  if(!name||!len)return t;
  while(t.numeric_prefix<len&&name[t.numeric_prefix]>='0'&&name[t.numeric_prefix]<='9')++t.numeric_prefix;
  for(i=0u;i<len;++i)if(name[i]=='.'&&i+1u<len)t.extension_offset=i+1u;
  if(t.extension_offset>=len)return t;
  const char*e=name+t.extension_offset;
  unsigned m=len-t.extension_offset;
  if(ext_eq(e,m,"json",4u))t.kind=2u;
  else if(ext_eq(e,m,"jsonl",5u))t.kind=3u;
  else if(ext_eq(e,m,"dat",3u))t.kind=4u;
  else if(ext_eq(e,m,"zip",3u))t.kind=5u;
  else if(ext_eq(e,m,"txt",3u)||ext_eq(e,m,"md",2u)||ext_eq(e,m,"csv",3u))t.kind=6u;
  else if(ext_eq(e,m,"jpg",3u)||ext_eq(e,m,"jpeg",4u)||ext_eq(e,m,"png",3u)||ext_eq(e,m,"webp",4u))t.kind=7u;
  return t;
}
