/* POSIX host/Termux adapter is isolated from the freestanding core.
 * Only adapter uses libc, mmap, pthread and clocks. Not an ABI-independent ELF.
 * Build on ARM32 and ARM64 separately; record actual runtime architecture.
 */
#define _GNU_SOURCE
#define _POSIX_C_SOURCE 200809L
#include "raf_lab_core.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#include <pthread.h>
#include <stdatomic.h>
#include <sched.h>
#include <unistd.h>
#include <sys/mman.h>
typedef struct {
    raf_u8 *ptr;
    raf_u64 n,first,last,sum;
    raf_u32 rounds,mode,stride;
    atomic_int *ready,*start;
} Job;
static void *worker(void *opaque) {
    Job *j=(Job *)opaque;
    atomic_fetch_add(j->ready,1);
    while (!atomic_load(j->start)) sched_yield();
    j->sum=raf_lab_partition(j->ptr,j->n,j->first,j->last,j->rounds,j->mode,j->stride);
    return 0;
}
static raf_u64 nanos(void) {
    struct timespec t;
    if (clock_gettime(CLOCK_MONOTONIC,&t)) return 0;
    return (raf_u64)t.tv_sec*1000000000ull+(raf_u64)t.tv_nsec;
}
static raf_u32 choose(const char *s) {
    if (!strcmp(s,"stream")) return RAF_STREAM_WRITE;
    if (!strcmp(s,"reuse")) return RAF_CACHE_REUSE;
    if (!strcmp(s,"compute")) return RAF_COMPUTE_ONLY;
    if (!strcmp(s,"stride")) return RAF_STRIDE_READ;
    if (!strcmp(s,"q16")) return RAF_Q16_GEOMETRY;
    return 0;
}
static int memory_allowed(raf_u64 bytes) {
    long pages=sysconf(_SC_AVPHYS_PAGES),size=sysconf(_SC_PAGESIZE);
    if(pages<=0 || size<=0) return 0;
    return bytes<=((raf_u64)pages*(raf_u64)size)/3u;
}
int main(int argc,char **argv) {
    pthread_t ids[8]; Job jobs[8]; atomic_int ready=ATOMIC_VAR_INIT(0);
    atomic_int start=ATOMIC_VAR_INIT(0);
    raf_u64 n,t0,t1,total=0,expected,hash;
    raf_u32 mode,workers,rounds=4,stride=RAF_DEFAULT_STRIDE;
    int large=0,i,created=0;raf_u8 *buf;
    double mib; char *end=0;
    if(argc<4){fputs("Usage: raf_lab_host MODE WORKERS MiB [--allow-large] [--rounds=4] [--stride=4093]\n",stderr);return 64;}
    mode=choose(argv[1]);workers=(raf_u32)strtoul(argv[2],&end,10);
    if(!end || *end || (workers!=1 && workers!=2 && workers!=4 && workers!=8) || !mode) return 64;
    mib=strtod(argv[3],&end);
    if(!end || *end || (mib!=0.25 && mib!=2.0 && mib!=16.0 && mib!=64.0 && mib!=150.0)) return 64;
    n=(raf_u64)(mib*1048576.0);
    for(i=4;i<argc;i++){
        if(!strcmp(argv[i],"--allow-large")) large=1;
        else if(!strncmp(argv[i],"--rounds=",9)) {
            rounds=(raf_u32)strtoul(argv[i]+9,&end,10);
            if(!end||*end||rounds<1||rounds>16) return 64;
        } else if(!strncmp(argv[i],"--stride=",9)){
            stride=(raf_u32)strtoul(argv[i]+9,&end,10);
            if(!end||*end||stride<1||stride>8191) return 64;
        } else return 64;
    }
    if(n>=64ull*1048576ull&&!large){fputs("BLOCKED_LARGE_BUFFER_CONSENT\n",stderr);return 77;}
    if(!memory_allowed(n)){fputs("BLOCKED_AVAILABLE_RAM\n",stderr);return 77;}
    buf=(raf_u8*)mmap(0,(size_t)n,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
    if(buf==MAP_FAILED){fputs("BLOCKED_MMAP\n",stderr);return 77;}
    raf_lab_init(buf,n);
    for(i=0;i<(int)workers;i++){
        Job *j=&jobs[i];
        j->ptr=buf;j->n=n;j->first=n*(raf_u64)i/workers;
        j->last=n*(raf_u64)(i+1)/workers;
        j->rounds=rounds;j->mode=mode;j->stride=stride;j->sum=0;
        j->ready=&ready;j->start=&start;
        if(pthread_create(&ids[i],0,worker,j)){fputs("THREAD_CREATE_FAIL\n",stderr);break;}
        created++;
    }
    if(created!=(int)workers){
        atomic_store(&start,1);
        for(i=0;i<created;i++) pthread_join(ids[i],0);
        munmap(buf,(size_t)n);return 77;
    }
    while(atomic_load(&ready)!=(int)workers) sched_yield();
    t0=nanos();atomic_store(&start,1);
    for(i=0;i<(int)workers;i++) pthread_join(ids[i],0);
    t1=nanos();
    for(i=0;i<(int)workers;i++) total+=jobs[i].sum;
    hash=raf_lab_digest(buf,n);
    expected=raf_lab_reference(n,rounds,mode,stride);
    printf("{\"schema\":\"rafaelia.lab.measure.v1\",\"mode\":\"%s\",\"workers\":%u,"
           "\"buffer_bytes\":%llu,\"rounds\":%u,\"stride\":%u,\"elapsed_ns\":%llu,"
           "\"checksum\":%llu,\"reference\":%llu,\"fnv64\":%llu,\"kat_pass\":%s,"
           "\"claim_allowed\":false}\n",
           argv[1],workers,n,rounds,stride,t1>=t0?t1-t0:0,total,expected,hash,
           total==expected?"true":"false");
    munmap(buf,(size_t)n);
    return total==expected && t1>t0?0:1;
}
