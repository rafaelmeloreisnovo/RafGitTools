/* SPDX-License-Identifier: GPL-3.0-or-later
 * Host-only falsifiers; libc is permitted HERE, never in the L0 core.
 */
#include "inventarioraf.h"
#include <stdio.h>

static unsigned failures;
#define CHECK(name,expr) do { \
    if (!(expr)) { ++failures; printf("FAIL %s\n",name); } \
    else printf("PASS %s\n",name); \
} while (0)

static struct ir_request request_for(const ir_u8 *data, ir_size length,
                                     ir_u64 generation) {
    struct ir_request q;
    q.data=data; q.size=length; q.authorized_source=1u; q.commit=1u;
    q.backend=IR_BACKEND_AUTO; q.advertised_caps=IR_CAP_SCALAR;
    q.verified_caps=IR_CAP_SCALAR; q.watchdog_fuel=1000000u;
    q.watchdog_bytes=1000000u; q.expected_generation=generation;
    return q;
}

int main(void) {
    ir_u8 elf32[52]={0}, elf64[64]={0}, dex[112]={0}, zip[100]={0};
    ir_u8 arbitrary[4]={'A','B','C','D'};
    struct ir_state st={0u,0u,0u,0u}, old;
    struct ir_request q;
    struct ir_receipt r, a;

    elf32[0]=0x7f; elf32[1]='E'; elf32[2]='L'; elf32[3]='F';
    elf32[4]=1; elf32[5]=1; elf32[6]=1; elf32[18]=40; elf32[40]=52;
    q=request_for(elf32,sizeof elf32,0u);
    q.commit=0;
    CHECK("ELF32_DRYRUN", inventarioraf_step(&st,&q,&r)==IR_OK &&
        r.format==IR_FORMAT_ELF && r.arch_machine==40u && !r.committed);
    CHECK("NO_COMMIT_NO_STATE_CHANGE",st.generation==0u && st.last_digest_fnv64==0u);
    a=r;
    CHECK("DETERMINISTIC_REPLAY", inventarioraf_step(&st,&q,&r)==IR_OK &&
        r.digest_fnv64==a.digest_fnv64 && r.bytes_seen==a.bytes_seen);
    q.commit=1;
    CHECK("COMMIT_GENERATION", inventarioraf_step(&st,&q,&r)==IR_OK &&
        st.generation==1u && r.committed==1u);
    old=st;
    CHECK("STALE_GENERATION_ROLLBACK", inventarioraf_step(&st,&q,&r)==IR_BLOCKED &&
        st.generation==old.generation && st.last_digest_fnv64==old.last_digest_fnv64);
    q.expected_generation=st.generation;
    q.watchdog_bytes=51u;
    CHECK("BYTE_WATCHDOG_FAILCLOSED", inventarioraf_step(&st,&q,&r)==IR_BUDGET &&
        st.generation==old.generation);
    q.watchdog_bytes=1000000u; q.watchdog_fuel=52u;
    CHECK("FUEL_WATCHDOG_FAILCLOSED", inventarioraf_step(&st,&q,&r)==IR_BUDGET &&
        st.generation==old.generation);
    q.watchdog_fuel=1000000u; q.authorized_source=0u;
    CHECK("UNAUTHORIZED_SOURCE_DENIED", inventarioraf_step(&st,&q,&r)==IR_BLOCKED);
    q.authorized_source=1u;
    q.backend=IR_BACKEND_NEON;
    CHECK("NEON_REQUEST_SCALAR_FALLBACK",inventarioraf_step(&st,&q,&r)==IR_OK &&
        r.backend_used==IR_BACKEND_SCALAR && (r.warnings & IR_WARN_FALLBACK));
    q.backend=IR_BACKEND_GPU; q.verified_caps=0u;
    CHECK("GPU_WITHOUT_TESTED_FALLBACK_BLOCKED", inventarioraf_step(&st,&q,&r)==IR_BLOCKED &&
        st.generation==old.generation);
    q=request_for(elf32,sizeof elf32,st.generation);
    elf32[40]=0;
    CHECK("ELF_BAD_HEADER_ROLLBACK", inventarioraf_step(&st,&q,&r)==IR_BLOCKED &&
        st.generation==old.generation);
    elf32[40]=52;
    elf64[0]=0x7f; elf64[1]='E';elf64[2]='L';elf64[3]='F';
    elf64[4]=2;elf64[5]=1;elf64[6]=1;elf64[18]=183;elf64[52]=64;
    q=request_for(elf64,sizeof elf64,st.generation);q.commit=0;
    CHECK("ELF64_AARCH64",inventarioraf_step(&st,&q,&r)==IR_OK &&
        r.arch_machine==183u && r.format_class==2u);
    dex[0]='d';dex[1]='e';dex[2]='x';dex[3]='\n';
    dex[4]='0';dex[5]='3';dex[6]='9';dex[7]=0;
    dex[32]=112;dex[36]=112;
    dex[40]=0x78;dex[41]=0x56;dex[42]=0x34;dex[43]=0x12;
    q=request_for(dex,sizeof dex,st.generation);q.commit=0;
    CHECK("DEX_STRUCTURAL_ONLY", inventarioraf_step(&st,&q,&r)==IR_OK &&
        r.format==IR_FORMAT_DEX && r.full_inventory==0u &&
        (r.warnings & IR_WARN_PARTIAL_FORMAT));
    dex[32]=111;
    CHECK("DEX_SIZE_FALSIFIER", inventarioraf_step(&st,&q,&r)==IR_BLOCKED);
    dex[32]=112;
    /* One zero-byte stored member x: local at 0, central at 31, EOCD at 78. */
    zip[0]='P';zip[1]='K';zip[2]=3;zip[3]=4;
    zip[26]=1;zip[30]='x';
    zip[31]='P';zip[32]='K';zip[33]=1;zip[34]=2;
    zip[31+28]=1;zip[31+46]='x';
    zip[78]='P';zip[79]='K';zip[80]=5;zip[81]=6;
    zip[78+8]=1;zip[78+10]=1;zip[78+12]=47;zip[78+16]=31;
    q=request_for(zip,sizeof zip,st.generation);q.commit=0;
    CHECK("ZIP_HEADERS_TOPOLOGY", inventarioraf_step(&st,&q,&r)==IR_OK &&
        r.format==IR_FORMAT_ZIP && r.entries_observed==1u &&
        r.full_inventory==0u);
    zip[30]='\\';
    CHECK("ZIP_UNSAFE_PATH_BLOCKED",inventarioraf_step(&st,&q,&r)==IR_BLOCKED);
    zip[30]='x';zip[78+10]=2;
    CHECK("ZIP_EOCD_COUNT_FALSIFIER",inventarioraf_step(&st,&q,&r)==IR_BLOCKED);
    zip[78+10]=1;
    q=request_for(arbitrary,sizeof arbitrary,st.generation);q.commit=1;
    CHECK("UNKNOWN_IS_NOT_ZERO_OR_PASS",inventarioraf_step(&st,&q,&r)==IR_TOKEN_VAZIO &&
        st.generation==old.generation && r.full_inventory==0u);
    CHECK("CLAIM_NEVER_PROMOTED",r.claim_allowed==0u);
    CHECK("NULL_RECEIPT_BLOCKED",inventarioraf_step(&st,&q,0)==IR_BLOCKED);
    printf("RESULT %s (%u failures)\n",failures?"FAIL":"PASS",failures);
    return failures?1:0;
}
