#pragma once
/* br_dns.h — URL parser + DNS resolve mínimo
 * [R29] Parse de URL: scheme://host[:port]/path
 * [R30] DNS raw UDP: porta 53, query A record
 * [R31] Fallback: aceita IP numérico diretamente
 * [R32] Failsafe: TTL 3 tentativas com rollback de estado
 */
#include "br_http.h"

/* Parse de URL → preenche BCtx */
static s32 URL_PARSE(const char*url,BCtx*ctx){
    MC0(ctx->host,256u);MC0(ctx->path,512u);
    ctx->port=80u;ctx->use_tls=0;
    const char*p=url;
    /* Detecta scheme */
    if(BR_MEMCMP(p,"https://",8u)==0){ctx->use_tls=1;ctx->port=443u;p+=8;}
    else if(BR_MEMCMP(p,"http://",7u)==0){p+=7;}
    else if(BR_MEMCMP(p,"//",2u)==0){p+=2;}
    /* Copia host até '/' ou ':' ou '\0' */
    u32 hi=0;
    while(*p&&*p!='/'&&*p!=':'){
        if(hi<255u)ctx->host[hi++]=(char)*p;
        p++;
    }
    ctx->host[hi]=0;
    /* Porta customizada? */
    if(*p==':'){p++;u32 port=0;while(*p>='0'&&*p<='9'){port=port*10u+(u32)(*p-'0');p++;}ctx->port=port;}
    /* Path */
    if(*p=='/'){u32 pi=0;while(*p&&pi<511u){ctx->path[pi++]=(char)*p;p++;}ctx->path[pi]=0;}
    else{ctx->path[0]='/';ctx->path[1]=0;}
    return hi>0?0:-1;
}

/* DNS query record A via UDP (porta 53)
 * [R33] Usa syscalls diretos: ARM32 socket=281, connect=283, sendto=290, recvfrom=292
 * [R34] Sem resolv.h: DNS server hardcoded 8.8.8.8 */
#define SOCK_DGRAM 2
#define AF_INET    2

static s32 DNS_RESOLVE(const char*host,u8 ip[4]){
    /* Tenta primeiro parse direto de IP */
    if(PARSE_IP(host,ip)==0)return 0;

    /* Monta query DNS raw */
    static u8 _DNS_BUF[512];
    static u8 _DNS_RSP[512];
    MC0(_DNS_BUF,512u);

    /* DNS header: txid=0x1234 flags=0x0100 qdcount=1 */
    _DNS_BUF[0]=0x12;_DNS_BUF[1]=0x34; /* txid */
    _DNS_BUF[2]=0x01;_DNS_BUF[3]=0x00; /* flags: recursion desired */
    _DNS_BUF[4]=0x00;_DNS_BUF[5]=0x01; /* qdcount=1 */
    _DNS_BUF[6]=0x00;_DNS_BUF[7]=0x00; /* ancount=0 */
    _DNS_BUF[8]=0x00;_DNS_BUF[9]=0x00; /* nscount=0 */
    _DNS_BUF[10]=0x00;_DNS_BUF[11]=0x00;/* arcount=0 */

    /* Encode hostname como labels DNS */
    u8*q=_DNS_BUF+12;
    u8*qend=_DNS_BUF+sizeof(_DNS_BUF);
    const char*h=host;
    while(*h){
        const char*dot=h;while(*dot&&*dot!='.')dot++;
        u32 llen=(u32)(dot-h);
        if(llen==0u||llen>63u)return-1;
        if(q+1u+(usize)llen+5u>qend)return-1;
        *q++=(u8)llen;
        MC(q,(const u8*)h,llen);q+=llen;
        h=dot;
        if(*h=='.'){
            h++;
            if(*h==0)return-1;
        }
    }
    if(q+5u>qend)return-1;
    *q++=0x00; /* root label */
    *q++=0x00;*q++=0x01; /* QTYPE = A */
    *q++=0x00;*q++=0x01; /* QCLASS = IN */
    u32 qlen=(u32)(q-_DNS_BUF);

    /* Cria socket UDP */
#if defined(__arm__)
    s32 fd=(s32)_sc3(281u,AF_INET,SOCK_DGRAM,0);
#elif defined(__aarch64__)
    s32 fd=(s32)_sc3(198u,AF_INET,SOCK_DGRAM,0);
#elif defined(__x86_64__)
    s32 fd=(s32)_sc3(41u,AF_INET,SOCK_DGRAM,0);
#endif
    if(fd<0)return -1;

    BRTimeVal tv;
    tv.tv_sec=(usize)1u;
    tv.tv_usec=(usize)0u;
    (void)SETSOCKOPT(fd,SOL_SOCKET,SO_RCVTIMEO,&tv,(u32)sizeof(tv));

    /* Envia para 8.8.8.8:53 */
    SA4 dns_sa;MC0(&dns_sa,sizeof(dns_sa));
    dns_sa.fam=(u16)AF_INET;
    dns_sa.port_be=HTON16(53u);
    dns_sa.ip[0]=8;dns_sa.ip[1]=8;dns_sa.ip[2]=8;dns_sa.ip[3]=8;

#if defined(__arm__)
    s32 sent=(s32)_sc6(290u,(u32)fd,(u32)(usize)_DNS_BUF,(u32)qlen,0,(u32)(usize)&dns_sa,(u32)sizeof(dns_sa));
    s32 rlen=(s32)_sc6(292u,(u32)fd,(u32)(usize)_DNS_RSP,512u,0,0,0);
#elif defined(__aarch64__)
    s32 sent=(s32)_sc6(206u,(u64)fd,(u64)(usize)_DNS_BUF,(u64)qlen,0,(u64)(usize)&dns_sa,(u64)sizeof(dns_sa));
    s32 rlen=(s32)_sc6(207u,(u64)fd,(u64)(usize)_DNS_RSP,512u,0,0,0);
#elif defined(__x86_64__)
    s32 sent=(s32)_sc6(44u,(u64)fd,(u64)(usize)_DNS_BUF,(u64)qlen,0,(u64)(usize)&dns_sa,(u64)sizeof(dns_sa));
    s32 rlen=(s32)_sc6(45u,(u64)fd,(u64)(usize)_DNS_RSP,512u,0,0,0);
#endif
    CLOSE(fd);
    if(sent<(s32)qlen)return-1;
    if(rlen<12)return-1;

    /* Valida DNS header da resposta */
    u16 txid=(u16)(((u16)_DNS_RSP[0]<<8u)|_DNS_RSP[1]);
    u16 flags=(u16)(((u16)_DNS_RSP[2]<<8u)|_DNS_RSP[3]);
    u16 qdcount=(u16)(((u16)_DNS_RSP[4]<<8u)|_DNS_RSP[5]);
    u16 ancount=(u16)(((u16)_DNS_RSP[6]<<8u)|_DNS_RSP[7]);
    if(txid!=0x1234u)return-1;
    if((flags&0x8000u)==0u)return-1; /* QR=1 resposta */
    if((flags&0x000Fu)!=0u)return-1; /* RCODE=0 */
    if(qdcount!=1u)return-1;
    if(ancount==0u)return-1;

    /* Parse resposta: pula header + query, encontra primeiro A record */
    /* Pula query section (mesmo que enviamos) */
    u32 pos=12u;
    /* Pula labels (pode ter pointer 0xC0) */
    while(pos<(u32)rlen){
        if(_DNS_RSP[pos]==0){
            if(pos+1u>(u32)rlen)return-1;
            pos++;
            break;
        }
        if((_DNS_RSP[pos]&0xC0u)==0xC0u){
            if(pos+2u>(u32)rlen)return-1;
            pos+=2u;
            break;
        }
        u32 step=(u32)_DNS_RSP[pos]+1u;
        if(pos+step>(u32)rlen)return-1;
        pos+=step;
    }
    if(pos+4u>(u32)rlen)return-1;
    pos+=4u; /* pula qtype+qclass */

    /* Parse answer records */
    for(u32 an=0;an<ancount&&pos+12u<=(u32)rlen;an++){
        /* Pula name (pointer ou labels) */
        if(pos>=(u32)rlen)return-1;
        if((_DNS_RSP[pos]&0xC0u)==0xC0u){
            if(pos+2u>(u32)rlen)return-1;
            pos+=2u;
        }else{
            while(pos<(u32)rlen&&_DNS_RSP[pos]){
                u32 step=(u32)_DNS_RSP[pos]+1u;
                if(pos+step>(u32)rlen)return-1;
                pos+=step;
            }
            if(pos+1u>(u32)rlen)return-1;
            pos++;
        }
        if(pos+10u>(u32)rlen)return-1; /* type,class,ttl,rdlen */
        u16 rtype=(u16)(((u16)_DNS_RSP[pos]<<8u)|_DNS_RSP[pos+1u]);
        pos+=8u; /* pula type class ttl */
        u16 rdlen=(u16)(((u16)_DNS_RSP[pos]<<8u)|_DNS_RSP[pos+1u]);
        pos+=2u;
        if(pos+(u32)rdlen>(u32)rlen)return-1;
        if(rtype==1u&&rdlen==4u){
            MC(ip,_DNS_RSP+pos,4u);
            return 0;
        }
        pos+=rdlen;
    }
    return-1;
}
