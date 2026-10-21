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
    /* Bounded input; reject before writing to buf. */
    if(!host||!path||!buf)return 0u;
    u32 hlen=0u,plen=0u;
    while(hlen<254u&&host[hlen])hlen++;
    while(plen<512u&&path[plen])plen++;
    if(!hlen||hlen>253u||!plen||plen>=512u||path[0]!='/')return 0u;
    for(u32 i=0u;i<hlen;i++){
        u8 c=(u8)host[i];
        if(c<=0x20u||c>=0x7Fu)return 0u;
    }
    for(u32 i=0u;i<plen;i++){
        u8 c=(u8)path[i];
        if(c<=0x20u||c==0x7Fu)return 0u;
    }
    /* Literal payload 194 bytes; each variable field checked above. */
    if(cap<194u+hlen+plen)return 0u;
    u8*p=buf;
    /* GET {path} HTTP/1.1\r\n */
    MC(p,(const u8*)"GET ",4u);p+=4;
    MC(p,(const u8*)path,plen);p+=plen;
    MC(p,(const u8*)" HTTP/1.1\r\n",11u);p+=11;
    /* Host: {host}\r\n */
    MC(p,(const u8*)"Host: ",6u);p+=6;
    MC(p,(const u8*)host,hlen);p+=hlen;
    MC(p,(const u8*)"\r\n",2u);p+=2;
    /* Connection: close\r\n */
    MC(p,(const u8*)"Connection: close\r\n",19u);p+=19;
    /* User-Agent: RAFAELIA-Browser/1.0\r\n */
    MC(p,(const u8*)"User-Agent: RAFAELIA-Browser/1.0 (ARM; freestanding; nolibc)\r\n",62u);p+=62;
    /* Accept: text/html,text/plain\r\n */
    MC(p,(const u8*)"Accept: text/html,text/plain;q=0.9,*/*;q=0.8\r\n",46u);p+=46;
    /* Accept-Language: pt-BR,pt;q=0.9,en;q=0.8\r\n */
    MC(p,(const u8*)"Accept-Language: pt-BR,pt;q=0.9,en;q=0.8\r\n",42u);p+=42;
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
static u32 HTTP_HEADERS_END(const u8*buf,u32 n);

/* Compara memória ASCII sem libc, normalizando apenas A-Z -> a-z. */
static int BR_MEMCASECMP_ASCII(const u8*a,const char*b,u32 n){
    for(u32 i=0;i<n;i++){
        u8 ca=a[i];
        u8 cb=(u8)b[i];
        if(ca>='A'&&ca<='Z')ca=(u8)(ca+('a'-'A'));
        if(cb>='A'&&cb<='Z')cb=(u8)(cb+('a'-'A'));
        if(ca!=cb)return(int)ca-(int)cb;
    }
    return 0;
}

/* Procura "Key: value\r\n" em buf[0..n], retorna ponteiro para value */
static const u8* HTTP_FIND_HEADER(const u8*buf,u32 n,const char*key){
    u32 kl=SL(key);
    u32 hdr_end=HTTP_HEADERS_END(buf,n);
    for(u32 i=0;i+kl+2u<hdr_end;i++){
        u32 bol=(i==0u)||((i>=2u)&&buf[i-2u]=='\r'&&buf[i-1u]=='\n');
        if(bol&&BR_MEMCASECMP_ASCII(buf+i,key,kl)==0&&buf[i+kl]==':'){
            u32 j=i+kl+1u;
            while(j<hdr_end&&(buf[j]==' '||buf[j]=='\t'))j++;
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

/* Strict bounded Content-Length: 0 absent, 1 valid, -1 malformed.
 * Do not reuse STR2U32: HTML numeric entities need separate semantics. */
static s32 HTTP_CONTENT_LENGTH(const u8*buf,u32 n,u32*out){
    if(!buf||!out)return-1;
    u32 hend=HTTP_HEADERS_END(buf,n);
    if(!hend)return-1;
    const u8*value=HTTP_FIND_HEADER(buf,n,"Content-Length");
    if(!value)return 0;
    u32 off=(u32)(value-buf);
    u32 v=0u,digits=0u;
    for(u32 i=off;i<hend;i++){
        u8 c=buf[i];
        if(c=='\r'){
            if(!digits||i+1u>=hend||buf[i+1u]!='\n')return-1;
            *out=v;
            return 1;
        }
        if(c<'0'||c>'9')return-1;
        u32 digit=(u32)(c-'0');
        if(v>429496729u||(v==429496729u&&digit>5u))return-1;
        v=v*10u+digit;
        digits++;
    }
    return-1;
}

/* ── FIND END OF HEADERS ──────────────────────────────────────────────────── */
/* Retorna offset do início do body (após \r\n\r\n) */


/* ── DECHUNK IN-PLACE ───────────────────────────────────────────────────── */
/* Retorna tamanho do body dechunkado, ou 0xFFFFFFFF em erro */
static u32 HTTP_DECHUNK(u8*buf,u32 n){
    u32 r=0u,w=0u;
    while(r<n){
        u32 sz=0u;
        u32 got_digit=0u;
        while(r<n){
            u8 c=buf[r++];
            if(c=='\r')break;
            if(c==';'){
                while(r<n&&buf[r]!='\r')r++;
                break;
            }
            u32 v;
            if(c>='0'&&c<='9')v=(u32)(c-'0');
            else if(c>='a'&&c<='f')v=10u+(u32)(c-'a');
            else if(c>='A'&&c<='F')v=10u+(u32)(c-'A');
            else return 0xFFFFFFFFu;
            got_digit=1u;
            if(sz>0x0FFFFFFFu)return 0xFFFFFFFFu;
            sz=(sz<<4u)|v;
        }
        if(!got_digit)return 0xFFFFFFFFu;
        if(r>=n||buf[r]!='\n')return 0xFFFFFFFFu;
        r++;

        if(sz==0u){
            if(r+1u>=n||buf[r]!='\r'||buf[r+1u]!='\n')return 0xFFFFFFFFu;
            /* Only an explicit terminal CRLF is accepted here. Trailer
             * support is deliberately NOT implied by this subset parser. */
            r+=2u;
            if(r!=n)return 0xFFFFFFFFu;
            return w;
        }

        if(sz>n-r)return 0xFFFFFFFFu; /* subtraction avoids unsigned wrap */
        /* In-place dechunk writes below source (w<=r). MC copies backwards,
         * which corrupts overlapping ranges when dest<src. Move forwards. */
        for(u32 i=0u;i<sz;i++)buf[w+i]=buf[r+i];
        w+=sz;
        r+=sz;

        if(r+1u>=n||buf[r]!='\r'||buf[r+1u]!='\n')return 0xFFFFFFFFu;
        r+=2u;
    }
    return 0xFFFFFFFFu;
}
static u32 HTTP_HEADERS_END(const u8*buf,u32 n){
    for(u32 i=0;i+3u<n;i++){
        if(buf[i]=='\r'&&buf[i+1]=='\n'&&buf[i+2]=='\r'&&buf[i+3]=='\n')
            return i+4u;
    }
    return 0u; /* incomplete header: reject, not a zero-length page */
}
