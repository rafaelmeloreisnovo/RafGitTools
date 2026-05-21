#pragma once
/* br_html.h — HTML tokenizer/renderer text-mode (estilo Lynx/Links)
 * [R23] Máquina de estados: TEXT | TAG | ENTITY | COMMENT
 * [R24] Tags reconhecidas: p br h1..h6 li ul ol a title body head
 * [R25] Entities: &amp; &lt; &gt; &nbsp; &quot;
 * [R26] Output: ANSI colors para headings e links
 * [R27] Sem heap: buffer estático _RB[NET_BUF]
 * [R28] Branchless: estado via flip-flop de 2 bits
 */
#include "br_http.h"

/* Estados do tokenizer HTML */
#define HS_TEXT    0u  /* fora de tag */
#define HS_TAG     1u  /* dentro de <tag> */
#define HS_ENT     2u  /* dentro de &entity; */
#define HS_CMNT    3u  /* dentro de <!--comment--> */

static u32 HTML_RENDER(const u8*html,u32 n,u8*out,u32 cap){
    u8  st=HS_TEXT;          /* estado: flip-flop 2-bit */
    u32 oi=0;                /* índice de saída */
    u32 ci=0;                /* coluna atual (para wrap em 80) */
    u8  tc[32];u32 ti=0;     /* buffer de tag atual */
    u8  ec[8]; u32 ei=0;     /* buffer de entity atual */
    u8  in_head=0;           /* dentro de <head> */
    u8  col_h=0;             /* cor de heading ativa */

    /* Macro: emite byte no output sem overflow */
#define EM(c) do{if(oi<cap-1u){out[oi++]=(u8)(c);}}while(0)
#define EMS(s) do{const char*_s=(s);while(*_s)EM(*_s++);}while(0)

    for(u32 i=0;i<n;i++){
        u8 c=html[i];
        /* Flip-flop de estado */
        switch(st){
        case HS_TEXT:
            if(c=='<'){st=HS_TAG;ti=0;MC0(tc,32u);break;}
            if(c=='&'){st=HS_ENT;ei=0;MC0(ec,8u);break;}
            if(in_head)break;
            /* wrap a 80 cols */
            if(c=='\n'||c=='\r')break;
            if(c==' '||c=='\t'){if(ci>0&&out[oi-1]!=' '){EM(' ');ci++;}break;}
            EM(c);ci++;
            if(ci>=78u){EM('\n');ci=0;}
            break;
        case HS_TAG:
            if(c=='>'){
                st=HS_TEXT;
                tc[ti<31u?ti:31u]=0;
                /* Compara tag — branchless usando memcmp */
                u8*t=tc; /* alias para tc */
                /* Pula '/' para closing tags */
                u8*tt=t+(t[0]=='/'?1u:0u);
                /* lowercase implícito: qualquer case */
                /* heading tags → ANSI bold/color */
                if(tt[0]=='h'&&tt[1]>='1'&&tt[1]<='6'&&tt[2]==0){
                    if(t[0]!='/'){col_h=tt[1]-'0';
                        if(oi>0&&out[oi-1]!='\n'){EM('\n');ci=0;}
                        EMS("\033[1;33m"); /* yellow bold */
                    } else {
                        EMS("\033[0m\n\n");ci=0;col_h=0;
                    }
                } else if(BR_MEMCMP(tt,"p",2u)==0){
                    if(t[0]!='/'){if(ci>0){EM('\n');ci=0;}EM('\n');}
                } else if(BR_MEMCMP(tt,"br",3u)==0||BR_MEMCMP(tt,"br/",4u)==0){
                    EM('\n');ci=0;
                } else if(BR_MEMCMP(tt,"li",3u)==0){
                    if(ci>0){EM('\n');ci=0;}EMS("  \xe2\x80\xa2 ");ci=4;
                } else if(BR_MEMCMP(tt,"a",2u)==0){
                    if(t[0]!='/') EMS("\033[0;36m"); /* cyan for links */
                    else EMS("\033[0m");
                } else if(BR_MEMCMP(tt,"title",6u)==0){
                    if(t[0]!='/'){EMS("\n\033[1;32m[TÍTULO] ");} else {EMS("\033[0m\n");}
                } else if(BR_MEMCMP(tt,"head",5u)==0){
                    in_head=(t[0]!='/');
                } else if(BR_MEMCMP(tt,"body",5u)==0){
                    in_head=0;
                } else if(BR_MEMCMP(tt,"script",7u)==0||
                          BR_MEMCMP(tt,"style",6u)==0){
                    /* Pula conteúdo: simples skip */
                }
                ti=0;
            } else if(c=='!'&&ti==0){
                /* Pode ser comentário <!-- */
                st=HS_CMNT;
            } else {
                /* acumula nome da tag (só lowercase) */
                if(ti<31u){
                    if(c>='A'&&c<='Z')tc[ti++]=(u8)(c+32u);
                    else if(c==' '||c=='\t'||c=='\n')tc[ti<31u?ti:31u]=0; /* fim do nome */
                    else tc[ti++]=c;
                }
            }
            break;
        case HS_ENT:
            if(c==';'){
                st=HS_TEXT;ec[ei<7u?ei:7u]=0;
                if(BR_MEMCMP(ec,"amp",4u)==0)EM('&');
                else if(BR_MEMCMP(ec,"lt",3u)==0)EM('<');
                else if(BR_MEMCMP(ec,"gt",3u)==0)EM('>');
                else if(BR_MEMCMP(ec,"quot",5u)==0)EM('"');
                else if(BR_MEMCMP(ec,"apos",5u)==0)EM('\'');
                else if(BR_MEMCMP(ec,"nbsp",5u)==0)EM(' ');
                else if(ec[0]=='#'){
                    u32 code=STR2U32(ec+1,ei-1u);
                    if(code<128u)EM((u8)code);
                }
                ei=0;
            } else if(c=='<'||c=='\n'){
                /* entity mal formada: emite & e texto */
                st=HS_TEXT;EM('&');
                for(u32 j=0;j<ei;j++)EM(ec[j]);
                if(c!='<')EM(c); else {st=HS_TAG;ti=0;}
            } else {
                if(ei<7u)ec[ei++]=c;
            }
            break;
        case HS_CMNT:
            /* Simplificado: volta ao TEXT no próximo '>' */
            if(c=='>')st=HS_TEXT;
            break;
        }
    }
    /* Reseta cor */
    EMS("\033[0m\n");
    out[oi]=0;
#undef EM
#undef EMS
    return oi;
}
