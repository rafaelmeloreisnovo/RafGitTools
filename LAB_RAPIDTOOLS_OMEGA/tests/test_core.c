#include "raf_lab_core.h"
#include <stdio.h>
static raf_u8 data[256u*1024u],scratch[512],packed[1024];
static int fail(const char *msg) {fprintf(stderr,"FAIL %s\n",msg);return 1;}
int main(void){
    const raf_u32 modes[]={1,2,3,4,5};
    const raf_u32 worker_counts[]={1,2,4,8};
    raf_u32 m,w,part,rounds=4;
    raf_u64 reference,sum;raf_u32 n,out,decoded,i;
    if(RAF_Q16_SQRT3_OVER2!=56756u)return fail("Q16 constant");
    for(m=0;m<5;m++){
        reference=raf_lab_reference((raf_u64)sizeof data,rounds,modes[m],RAF_DEFAULT_STRIDE);
        for(w=0;w<4;w++) {
            raf_lab_init(data,(raf_u64)sizeof data);sum=0;
            for(part=0;part<worker_counts[w];part++)
                sum+=raf_lab_partition(data,(raf_u64)sizeof data,
                   ((raf_u64)sizeof data*part)/worker_counts[w],
                   ((raf_u64)sizeof data*(part+1))/worker_counts[w],
                   rounds,modes[m],RAF_DEFAULT_STRIDE);
            if(sum!=reference) return fail("workers modify semantic checksum");
        }
    }
    for(i=0;i<sizeof scratch;i++)scratch[i]=(raf_u8)(i/64u);
    n=(raf_u32)sizeof scratch;
    out=raf_lab_rle_pack(scratch,n,packed,(raf_u32)sizeof packed);
    if(!out||out>=n)return fail("RLE compression");
    for(i=0;i<n;i++)scratch[i]=0;
    decoded=raf_lab_rle_unpack(packed,out,scratch,n);
    if(decoded!=n)return fail("RLE roundtrip len");
    for(i=0;i<n;i++) if(scratch[i]!=(raf_u8)(i/64u))return fail("RLE roundtrip bytes");
    if(raf_lab_rle_pack(scratch,n,packed,1u)!=0u)return fail("overflow pack");
    if(raf_lab_rle_unpack(packed,3u,scratch,n)!=0u)return fail("truncated unpack");
    puts("PASS_CORE: 5 modes * 4 partitions, Q16, RLE, bounds");
    return 0;
}
