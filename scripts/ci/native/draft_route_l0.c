/* RAFAELIA L0 Linux/x86_64 CI draft routing; no libc, heap, malloc or network.
 * Input: strict git diff -z path stream. Output: fast/android/full.
 * Explicitly scoped to hosted Ubuntu x86_64, NOT Android ARM/ARM64.
 */
#if !defined(__x86_64__) || !defined(__linux__)
#error "L0 draft router supports Linux x86_64 only"
#endif
typedef unsigned long usize;
typedef unsigned char u8;
static long call3(long n,long a,long b,long c){
    long v;
    __asm__ volatile("syscall":"=a"(v):"a"(n),"D"(a),"S"(b),"d"(c):"rcx","r11","memory");
    return v;
}
__attribute__((noreturn)) static void done(int status){
    (void)call3(60,status,0,0);__builtin_unreachable();
}
static void out(const char *s,usize n){
    while(n){long k=call3(1,1,(long)s,(long)n);if(k<=0)done(2);s+=(usize)k;n-=(usize)k;}
}
static usize size(const char *s){usize n=0;while(s[n])++n;return n;}
static int start(const u8 *s,usize n,const char *x){
    usize z=size(x);if(z>n)return 0;
    for(usize i=0;i<z;++i)if(s[i]!=(u8)x[i])return 0;
    return 1;
}
static int same(const u8*s,usize n,const char*x){return n==size(x)&&start(s,n,x);}
static int suffix(const u8*s,usize n,const char*x){
    usize z=size(x);return z<=n&&start(s+n-z,z,x);
}
static int utf8(const u8*p,usize n){
    for(usize i=0;i<n;){
        u8 a=p[i];if(a<128){++i;continue;}
        unsigned w;
        if(a>=0xc2&&a<=0xdf)w=2;
        else if(a>=0xe0&&a<=0xef)w=3;
        else if(a>=0xf0&&a<=0xf4)w=4;
        else return 0;
        if(i+w>n)return 0;
        for(unsigned t=1;t<w;++t)if((p[i+t]&0xc0)!=0x80)return 0;
        if(w==3&&((a==0xe0&&p[i+1]<0xa0)||(a==0xed&&p[i+1]>=0xa0)))return 0;
        if(w==4&&((a==0xf0&&p[i+1]<0x90)||(a==0xf4&&p[i+1]>=0x90)))return 0;
        i+=w;
    }
    return 1;
}
static int classify(const u8*s,usize n){
    if(!n||s[0]=='/'||!utf8(s,n))return -1;
    usize begin=0;
    for(usize i=0;i<=n;++i){
        if(i<n&&(s[i]<32||s[i]==127||s[i]=='\\'))return -1;
        if(i==n||s[i]=='/'){
            usize z=i-begin;
            if(!z||(z==1&&s[begin]=='.')||
                (z==2&&s[begin]=='.'&&s[begin+1]=='.'))return -1;
            begin=i+1;
        }
    }
    if(start(s,n,".github/")||start(s,n,"scripts/ci/")||
       start(s,n,"tests/ci/"))return 2;
    if(same(s,n,"gradlew")||same(s,n,"gradlew.bat")||
       same(s,n,"build.gradle")||same(s,n,"build.gradle.kts")||
       same(s,n,"settings.gradle")||same(s,n,"settings.gradle.kts")||
       same(s,n,"gradle.properties")||start(s,n,"app/")||
       start(s,n,"gradle/")||suffix(s,n,".gradle")||
       suffix(s,n,".gradle.kts")||suffix(s,n,".kt")||
       suffix(s,n,".java"))return 1;
    return 0;
}
__attribute__((noreturn)) void _start(void){
    u8 chunk[4096],path[4096];usize n=0;int mode=0,seen=0;
    for(;;){
        long got=call3(0,0,(long)chunk,sizeof(chunk));
        if(got<0)done(2);if(!got)break;
        for(long k=0;k<got;++k){
            if(!chunk[k]){
                if(!n)continue; /* original Python classifier skips empty NUL fields */
                int grade=classify(path,n);
                if(grade<0)done(2);
                if(grade>mode)mode=grade;
                seen=1;n=0;
            }else{
                if(n>=sizeof(path))done(2);
                path[n++]=chunk[k];
            }
        }
    }
    if(n)done(2);
    if(!seen)mode=2;
    if(mode==2)out("full\n",5);
    else if(mode==1)out("android\n",8);
    else out("fast\n",5);
    done(0);
}
