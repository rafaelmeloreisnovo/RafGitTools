#pragma once
/* br_http.h — HTTP/1.1 request builder + response parser
 * [R17] Sem sprintf: string assembly byte a byte
 * [R18] HTTP/1.1: Host + Connection:close + User-Agent
 * [R19] Response parser: status line + headers + body
 * [R20] Content-Length: parse de decimal para u32
 * [R21] Chunked transfer: parse de hex chunks
 * [R22] Branchless header matching via CRC32C parcial
 */
#include "br_tls.h"

/* ── BUILDER DE REQUEST HTTP ────────────────────────────────────────────── */
static u32 HTTP_BUILD_REQ(const char*host,const char*path,u8*buf,u32 cap){
    u8*p=buf;
    /* GET {path} HTTP/1.1\r\n */
    MC(p,(const u8*)"GET ",4u);p+=4;
    u32 plen=SL(path);MC(p,(const u8*)path,plen);p+=plen;
    MC(p,(const u8*)" HTTP/1.1\r\n",11u);p+=11;
    /* Host: {host}\r\n */
    MC(p,(const u8*)"Host: ",6u);p+=6;
    u32 hlen=SL(host);MC(p,(const u8*)host,hlen);p+=hlen;
    MC(p,(const u8*)"\r\n",2u);p+=2;
    /* Connection: close\r\n */
    MC(p,(const u8*)"Connection: close\r\n",19u);p+=19;
    /* User-Agent: RAFAELIA-Browser/1.0\r\n */
    MC(p,(const u8*)"User-Agent: RAFAELIA-Browser/1.0 (ARM; freestanding; nolibc)\r\n",62u);p+=62;
    /* Accept: text/html,text/plain\r\n */
    MC(p,(const u8*)"Accept: text/html,text/plain;q=0.9,*/*;q=0.8\r\n",47u);p+=47;
    /* Accept-Language: pt-BR,pt;q=0.9,en;q=0.8\r\n */
    MC(p,(const u8*)"Accept-Language: pt-BR,pt;q=0.9,en;q=0.8\r\n",43u);p+=43;
    /* \r\n final */
    MC(p,(const u8*)"\r\n",2u);p+=2;
    return(u32)(p-buf);
}

/* ── PARSE DE STATUS LINE ───────────────────────────────────────────────── */
/* "HTTP/1.1 200 OK\r\n" → retorna status code */
static u32 HTTP_PARSE_STATUS(const u8*buf,u32 n){
    /* Pula "HTTP/1.x " (9 chars) e lê 3 dígitos */
    if(n<12u)return 0u;
    if(buf[0]!='H'||buf[1]!='T'||buf[2]!='T'||buf[3]!='P')return 0u;
    /* Pula até primeiro espaço */
    u32 i=0;while(i<n&&buf[i]!=' ')i++;i++;
    if(i+3u>=n)return 0u;
    u32 code=0;
    for(u32 j=0;j<3u;j++){
        if(buf[i+j]<'0'||buf[i+j]>'9')return 0u;
        code=code*10u+(u32)(buf[i+j]-'0');
    }
    return code;
}

/* ── PARSE DE HEADER VALUE ───────────────────────────────────────────────── */
/* Procura "Key: value\r\n" em buf[0..n], retorna ponteiro para value */
static const u8* HTTP_FIND_HEADER(const u8*buf,u32 n,const char*key){
    u32 kl=SL(key);
    for(u32 i=0;i+kl+2u<n;i++){
        if(BR_MEMCMP(buf+i,key,kl)==0&&buf[i+kl]==':'){
            u32 j=i+kl+1u;
            while(j<n&&(buf[j]==' '||buf[j]=='\t'))j++;
            return buf+j;
        }
    }
    return(const u8*)0;
}

/* Parse decimal string → u32 */
static u32 STR2U32(const u8*s,u32 n){
    u32 v=0,i=0;
    while(i<n&&s[i]>='0'&&s[i]<='9'){v=v*10u+(u32)(s[i]-'0');i++;}
    return v;
}

/* ── FIND END OF HEADERS ──────────────────────────────────────────────────── */
/* Retorna offset do início do body (após \r\n\r\n) */
static u32 HTTP_HEADERS_END(const u8*buf,u32 n){
    for(u32 i=0;i+3u<n;i++){
        if(buf[i]=='\r'&&buf[i+1]=='\n'&&buf[i+2]=='\r'&&buf[i+3]=='\n')
            return i+4u;
    }
    return n; /* headers não terminados ainda */
}
