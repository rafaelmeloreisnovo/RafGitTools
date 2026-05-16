/* br_main.c — Browser entry point
 * Fluxo: URL → DNS → TCP → (TLS) → HTTP → HTML → RENDER
 * [R35] Rollback: GM/GRS de arena em caso de erro
 * [R36] Failsafe: TTL 3 tentativas por fase
 * [R37] Flags lineares: abertos em sequência (FL_DNS→FL_CONNECT→...)
 * [R38] Inline ASM para seções críticas de timing
 */
#include "br_types.h"
#include "br_sys.h"
#include "br_tls.h"
#include "br_http.h"
#include "br_html.h"
#include "br_dns.h"
#ifndef AT_FDCWD
#define AT_FDCWD (-100)
#endif


/* ── UI DE STATUS ──────────────────────────────────────────────────────── */
static void STATUS(u8 flags,const char*msg){
    PS("\033[1;36m[");
    if(FF_GET(flags,FL_DNS))   PS("DNS ");
    if(FF_GET(flags,FL_CONNECT))PS("TCP ");
    if(FF_GET(flags,FL_TLS_HS))PS("TLS ");
    if(FF_GET(flags,FL_HTTP_TX))PS("TX ");
    if(FF_GET(flags,FL_HTTP_RX))PS("RX ");
    if(FF_GET(flags,FL_HTML_RND))PS("HTML ");
    if(FF_GET(flags,FL_ERROR)) PS("ERR ");
    if(FF_GET(flags,FL_DONE))  PS("DONE ");
    PS("]\033[0m ");PS(msg);PS("\n");
}

static void HEADER_LINE(void){
    PS("\033[1;34m");
    PS("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    PS("\033[0m");
}


static s32 SEND_ALL(s32 fd,const void*buf,u32 n){
    const u8*p=(const u8*)buf;
    u32 off=0;
    while(off<n){
        s32 w=SEND(fd,p+off,n-off);
        if(w<=0)return-1;
        off+=(u32)w;
    }
    return(s32)off;
}

static void WR_SAFE(u32 fd,const void*b,u32 n){
    if(n)WR(fd,b,n);
}

/* ── FETCH HTTP ─────────────────────────────────────────────────────────── */
static s32 DO_FETCH(BCtx*ctx){
    ctx->flags=FL_IDLE;
    u64 t0=NS();

    /* ── FASE 1: DNS resolve ─────────────────────────────────────────── */
    FF_SET(ctx->flags,FL_DNS);
    STATUS(ctx->flags,"Resolvendo DNS...");
    u8 ttl=3;
    s32 dns_ok=-1;
    while(ttl--){
        dns_ok=DNS_RESOLVE(ctx->host,ctx->ip);
        if(dns_ok==0)break;
        PS("  [RETRY DNS]\n");
    }
    if(dns_ok!=0){
        FF_SET(ctx->flags,FL_ERROR);
        STATUS(ctx->flags,"DNS falhou");
        return-1;
    }
    PS("  IP: ");PN(ctx->ip[0]);
    /* Exibe IP */
    {char ipstr[20];u32 i=0;
     for(u32 o=0;o<4u;o++){
         if(o)ipstr[i++]='.';
         UTOA(ctx->ip[o],ipstr+i);
         i+=SL(ipstr+i);
     }
     ipstr[i]=0;PS("  ");PS(ipstr);PS("\n");}
    FF_CLR(ctx->flags,FL_DNS);

    /* ── FASE 2: TCP connect ─────────────────────────────────────────── */
    FF_SET(ctx->flags,FL_CONNECT);
    STATUS(ctx->flags,"Conectando TCP...");
    GM(); /* checkpoint arena antes de alocar recursos de rede */

    SA4 sa;MC0(&sa,sizeof(sa));
    sa.fam=(u16)AF_INET;
    sa.port_be=HTON16((u16)ctx->port);
    MC(sa.ip,ctx->ip,4u);

    s32 conn_ok=-1;
    ttl=3;
    while(ttl--){
        ctx->fd=SOCKET();
        if(ctx->fd<0){
            PS("  [RETRY CONNECT socket fail]\n");
            continue;
        }
        if(SET_RECV_TIMEOUT(ctx->fd,(usize)3u)!=0){
            FF_SET(ctx->flags,FL_ERROR);
            STATUS(ctx->flags,"TCP falhou (SO_RCVTIMEO)");
            CLOSE(ctx->fd);
            ctx->fd=-1;
            FF_CLR(ctx->flags,FL_CONNECT);
            GRS();
            return-1;
        }
        if(CONNECT(ctx->fd,&sa)==0){
            conn_ok=0;
            break;
        }

        CLOSE(ctx->fd);
        ctx->fd=-1;
        PS("  [RETRY CONNECT]\n");
    }
    if(conn_ok!=0){
        FF_SET(ctx->flags,FL_ERROR);
        FF_CLR(ctx->flags,FL_CONNECT);
        STATUS(ctx->flags,"TCP falhou");
        PS("  Falha TCP\n");
        GRS();
        return-1;
    }
    FF_CLR(ctx->flags,FL_CONNECT);

    /* ── FASE 3: TLS (se HTTPS) ─────────────────────────────────────── */
    if(ctx->use_tls){
        FF_SET(ctx->flags,FL_TLS_HS);
        STATUS(ctx->flags,"TLS 1.3 ClientHello...");
        TLS_INIT(&_TLS);

        /* Constrói e envia ClientHello */
        u32 chlen=TLS_BUILD_CLIENT_HELLO(&_TLS,ctx->host,_NB,NET_BUF);
        _TLS.state=TLS_TRANSITION(_TLS.state,TLS_HT_CLIENT_HELLO);

        s32 sent=SEND(ctx->fd,_NB,chlen);
        ctx->tx_bytes+=(sent>0?(u32)sent:0u);

        if(sent<(s32)chlen){
            PS("  [TLS] ClientHello parcial\n");
        } else {
            PS("  [TLS] ClientHello enviado (");PN(chlen);PS("B)\n");
        }

        /* Recebe ServerHello */
        s32 rx=RECV(ctx->fd,_NB,NET_BUF);
        if(rx>4){
            TLSRec rec;
            MC0(&rec,(u32)sizeof(rec));
            if(TLS_PARSE_RECORD(_NB,(u32)rx,&rec)==0){
                PS("  [TLS] Record type=");PH(rec.type);
                PS("  [TLS] version=");PH(rec.version);
            }
            _TLS.state=TLS_TRANSITION(_TLS.state,TLS_HT_SERVER_HELLO);
            PS("  [TLS] Estado=");
            /* Exibe estado TLS */
            if(_TLS.state==TLS_CLI_HELLO)PS("CLI_HELLO\n");
            else if(_TLS.state==TLS_SRV_HELLO)PS("SRV_HELLO\n");
            else if(_TLS.state==TLS_ENCRYPTED)PS("ENCRYPTED\n");
            else PS("UNKNOWN\n");
            /* NOTA: sem crypto completo, handshake não avança além daqui */
            /* Em produção: implementar X25519 + AES-GCM + HKDF */
            PS("  [TLS] NOTA: crypto não implementado — usando HTTP para demo\n");
        }
        /* Fallback: fecha e reconecta em HTTP para demonstração */
        CLOSE(ctx->fd);
        ctx->port=80u;ctx->use_tls=0;
        sa.port_be=HTON16((u16)ctx->port);
        ctx->fd=SOCKET();
        if(ctx->fd<0){FF_SET(ctx->flags,FL_ERROR);GRS();return-1;}
        if(SET_RECV_TIMEOUT(ctx->fd,(usize)3u)!=0){
            FF_SET(ctx->flags,FL_ERROR);
            CLOSE(ctx->fd);
            ctx->fd=-1;
            GRS();
            return-1;
        }
        if(CONNECT(ctx->fd,&sa)!=0){FF_SET(ctx->flags,FL_ERROR);CLOSE(ctx->fd);GRS();return-1;}
        FF_CLR(ctx->flags,FL_TLS_HS);
        PS("  [FALLBACK] Usando HTTP para demo\n");
    }

    /* ── FASE 4: HTTP request ────────────────────────────────────────── */
    FF_SET(ctx->flags,FL_HTTP_TX);
    STATUS(ctx->flags,"Enviando request HTTP...");
    u32 reqlen=HTTP_BUILD_REQ(ctx->host,ctx->path,_NB,NET_BUF);
    PS("  Request (");PN(reqlen);PS("B):\n");
    WR_SAFE(2,_NB,reqlen); /* debug: imprime request no stderr */
    s32 sent=SEND_ALL(ctx->fd,_NB,reqlen);
    if(sent<0){
        FF_SET(ctx->flags,FL_ERROR);
        CLOSE(ctx->fd);
        GRS();
        return-1;
    }
    ctx->tx_bytes+=(u32)sent;
    FF_CLR(ctx->flags,FL_HTTP_TX);

    /* ── FASE 5: HTTP response ───────────────────────────────────────── */
    FF_SET(ctx->flags,FL_HTTP_RX);
    STATUS(ctx->flags,"Recebendo response...");
    u32 total=0;
    /* Acumula response em _NB */
    {
        u32 max=NET_BUF-1u;
        while(total<max){
            s32 r=RECV(ctx->fd,(void*)(_NB+total),max-total);
            if(r<=0)break;
            total+=(u32)r;
        }
        _NB[total]=0;
    }
    if(total==0u){
        FF_SET(ctx->flags,FL_ERROR);
        if(ctx->fd>=0){
            CLOSE(ctx->fd);
            ctx->fd=-1;
        }
        FF_CLR(ctx->flags,FL_HTTP_RX);
        GRS();
        return-1;
    }
    ctx->rx_bytes=total;
    FF_CLR(ctx->flags,FL_HTTP_RX);

    CLOSE(ctx->fd);
    ctx->fd=-1;

    /* Parse status */
    ctx->status=HTTP_PARSE_STATUS(_NB,total);
    PS("  Status HTTP: ");PN(ctx->status);

    /* Content-Length */
    const u8*clv=HTTP_FIND_HEADER(_NB,total,"Content-Length");
    if(clv)ctx->content_len=STR2U32(clv,16u);
    PS("  Content-Length: ");PN(ctx->content_len);

    /* ── FASE 6: Render HTML ─────────────────────────────────────────── */
    FF_SET(ctx->flags,FL_HTML_RND);
    u32 body_off=HTTP_HEADERS_END(_NB,total);
    u32 body_len=total>body_off?total-body_off:0u;
    STATUS(ctx->flags,"Renderizando HTML...");
    PS("  Body: ");PN(body_len);PS("B\n");

    u32 rlen=HTML_RENDER(_NB+body_off,body_len,_RB,NET_BUF);
    FF_CLR(ctx->flags,FL_HTML_RND);

    /* ── OUTPUT RENDERIZADO ───────────────────────────────────────────── */
    HEADER_LINE();
    PS("\033[1;32m");PS(ctx->host);PS(ctx->path);PS("\033[0m\n");
    HEADER_LINE();
    WR(1,_RB,rlen);
    HEADER_LINE();

    ctx->t_ns=NS()-t0;
    FF_SET(ctx->flags,FL_DONE);
    return 0;
}

/* ── ENTRY POINT ─────────────────────────────────────────────────────────── */
/* URLs de teste — passados via argvX no _start como posição fixa da stack */
/* Em Termux sem argc/argv: URL hardcoded ou lido de /proc/self/cmdline     */
static const char DEFAULT_URL[]="http://example.com/";

void browser_main(void){
    GR(); /* reset arena */

    /* ASCII art logo */
    PS("\033[1;36m");
    PS("╔══════════════════════════════════════════════════════════════╗\n");
    PS("║  \033[1;33m██████╗ ██████╗  ██████╗ ██╗    ██╗███████╗███████╗██████╗\033[1;36m  ║\n");
    PS("║  \033[1;32m RAFAELIA BROWSER · TLS1.3 · HTTP/1.1 · freestanding    \033[1;36m  ║\n");
    PS("║  \033[0;37m ARM32/ARM64/x86-64 · nolibc · nomalloc · inline ASM    \033[1;36m  ║\n");
    PS("║  \033[0;35m Turing Geométrica · Flip-Flop · Branchless · F*=23.158 \033[1;36m  ║\n");
    PS("╚══════════════════════════════════════════════════════════════╝\n");
    PS("\033[0m\n");

    /* Tenta ler URL de /proc/self/cmdline (argumento 1) */
    const char*url=DEFAULT_URL;
    /* Lê cmdline para obter argumento */
    static u8 _CMD[512];
    MC0(_CMD,512u);
#if defined(__arm__)
    s32 cfd=(s32)_sc3(5u,(u32)(usize)"/proc/self/cmdline",0,0);
    if(cfd>=0){_sc3(3u,(u32)cfd,(u32)(usize)_CMD,511u);_sc1(6u,(u32)cfd);}
#elif defined(__aarch64__)
    s32 cfd=(s32)_sc3(56u,(u64)(usize)AT_FDCWD,(u64)(usize)"/proc/self/cmdline",0u);
    if(cfd>=0){_sc3(63u,(u64)cfd,(u64)(usize)_CMD,511u);_sc1(57u,(u64)cfd);}
#elif defined(__x86_64__)
    s32 cfd=(s32)_sc3(2u,(u64)(usize)"/proc/self/cmdline",0,0);
    if(cfd>=0){_sc3(0u,(u64)cfd,(u64)(usize)_CMD,511u);_sc1(3u,(u64)cfd);}
#endif
    /* Pula arg0 (programa), pega arg1 se existe */
    {u32 ci=0;while(ci<511u&&_CMD[ci])ci++;ci++;
     if(ci<511u&&_CMD[ci]){url=(const char*)(_CMD+ci);}}

    PS("\033[1;37mURL: \033[0;36m");PS(url);PS("\033[0m\n\n");

    /* Inicializa contexto */
    MC0(&_BC,sizeof(_BC));
    if(URL_PARSE(url,&_BC)!=0){
        PS("\033[1;31m[ERRO] URL inválida\033[0m\n");
        EX();
    }

    PS("Host: ");PS(_BC.host);PS("\n");
    PS("Port: ");PN(_BC.port);
    PS("Path: ");PS(_BC.path);PS("\n");
    PS("TLS:  ");PS(_BC.use_tls?"SIM":"NAO");PS("\n\n");

    /* Executa fetch */
    s32 res=DO_FETCH(&_BC);

    /* Relatório final */
    HEADER_LINE();
    PS("\033[1;37mRELATÓRIO:\033[0m\n");
    PS("  Status:   ");PN(_BC.status);
    PS("  TX bytes: ");PN(_BC.tx_bytes);
    PS("  RX bytes: ");PN(_BC.rx_bytes);
    PS("  Latência: ");PN(_BC.t_ns/1000000u);PS("ms\n");
    PS("  Flags:    ");PH(_BC.flags);
    PS("  TLS:      ");PH(_BC.tls);
    HEADER_LINE();

    if(res==0){PS("\033[1;32m[OK] Fetch completo\033[0m\n");}
    else{PS("\033[1;31m[ERRO] Fetch falhou\033[0m\n");}

    EX();
}
