#include "raf_lab_core.h"
raf_u8 raf_lab_seed(raf_u64 i) { return (raf_u8)(((i * 31u) ^ 0x5au) & 255u); }
void raf_lab_init(raf_u8 *p, raf_u64 n) {
    raf_u64 i;
    for (i=0;i<n;i++) p[i]=raf_lab_seed(i);
}
static raf_u8 value(const raf_u8 *data, raf_u64 n, raf_u64 i,
                     raf_u32 round, raf_u32 mode, raf_u32 stride) {
    raf_u64 ix;
    switch (mode) {
        case RAF_STREAM_WRITE: return (raf_u8)(raf_lab_seed(i) ^ (raf_u8)round);
        case RAF_CACHE_REUSE: return data[i];
        case RAF_COMPUTE_ONLY: return (raf_u8)(raf_lab_seed(i) ^ (raf_u8)(round*17u));
        case RAF_STRIDE_READ:
            ix=(i*(raf_u64)stride)%n;
            return data[ix];
        case RAF_Q16_GEOMETRY:
            /* Fixed Q16 projection: deterministic integer model, NOT cosmology. */
            return (raf_u8)((((i & 65535u)*(raf_u64)RAF_Q16_SQRT3_OVER2)>>16)
                             ^ raf_lab_seed(i));
        default: return 0u;
    }
}
raf_u64 raf_lab_partition(raf_u8 *data, raf_u64 n, raf_u64 first,
                          raf_u64 last, raf_u32 rounds, raf_u32 mode,
                          raf_u32 stride) {
    raf_u32 r; raf_u64 i, sum=0u;
    if (!data || !n || first>last || last>n || !stride ||
        mode<RAF_STREAM_WRITE || mode>RAF_Q16_GEOMETRY) return 0u;
    for (r=0;r<rounds;r++) for (i=first;i<last;i++) {
        raf_u8 v=value(data,n,i,r,mode,stride);
        if (mode==RAF_STREAM_WRITE) data[i]=v;
        sum+=(i+1u)*(raf_u64)(v+1u); /* modulo 2^64, independent of workers */
    }
    return sum;
}
raf_u64 raf_lab_reference(raf_u64 n, raf_u32 rounds,
                          raf_u32 mode, raf_u32 stride) {
    /* Pure reference from initial input, usable for all read-only modes and
       stream writes. No memory required; intentionally independent of buffer. */
    raf_u64 i,sum=0u,ix; raf_u32 r;
    if (!n || !stride || mode<RAF_STREAM_WRITE || mode>RAF_Q16_GEOMETRY)
        return 0u;
    for (r=0;r<rounds;r++) for(i=0;i<n;i++) {
        raf_u8 v;
        if(mode==RAF_STREAM_WRITE) v=raf_lab_seed(i)^(raf_u8)r;
        else if(mode==RAF_CACHE_REUSE) v=raf_lab_seed(i);
        else if(mode==RAF_COMPUTE_ONLY) v=raf_lab_seed(i)^(raf_u8)(r*17u);
        else if(mode==RAF_STRIDE_READ) {
            ix=(i*(raf_u64)stride)%n;
            v=raf_lab_seed(ix);
        } else v=(raf_u8)((((i & 65535u)*(raf_u64)RAF_Q16_SQRT3_OVER2)>>16)
                                   ^ raf_lab_seed(i));
        sum+=(i+1u)*(raf_u64)(v+1u);
    }
    return sum;
}
raf_u64 raf_lab_digest(const raf_u8 *p, raf_u64 n) {
    raf_u64 i,h=14695981039346656037ull;
    for (i=0;i<n;i++) { h^=p[i]; h*=1099511628211ull; }
    return h; /* FNV-1a checksum: not signature or cryptographic digest */
}
raf_u32 raf_lab_rle_pack(const raf_u8 *s, raf_u32 n, raf_u8 *d, raf_u32 cap) {
    raf_u32 i=0,j=0;
    if (!s || !d) return 0u;
    while (i<n) {
        raf_u32 z=1;
        while (i+z<n && s[i+z]==s[i] && z<255u) z++;
        if (j>cap || cap-j<2u) return 0u;
        d[j++]=(raf_u8)z;d[j++]=s[i];i+=z;
    }
    return j;
}
raf_u32 raf_lab_rle_unpack(const raf_u8 *s, raf_u32 n, raf_u8 *d, raf_u32 cap) {
    raf_u32 i=0,j=0;
    if (!s || !d || (n&1u)) return 0u;
    while(i<n) {
        raf_u32 count=s[i++];raf_u8 v=s[i++];
        if (!count || j>cap || count>cap-j) return 0u;
        while(count--) d[j++]=v;
    }
    return j;
}
