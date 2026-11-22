/* SPDX-License-Identifier: GPL-3.0-or-later
 * Deterministic host-only mutation/property test.
 * No private data, no random service/network calls.
 */
#include "inventarioraf.h"
#include <stdio.h>

int main(void) {
    ir_u32 seed=0x5a17c9e3u, trials=4096u, i, j;
    ir_u8 data[512];
    struct ir_state state={0u,0u,0u,0u};
    struct ir_request q;
    struct ir_receipt x, y;
    for (i=0u;i<trials;++i) {
        ir_size n=(ir_size)((i*37u)%511u+1u);
        for (j=0u;j<(ir_u32)n;++j) {
            seed^=seed<<13; seed^=seed>>17; seed^=seed<<5;
            data[j]=(ir_u8)(seed&255u);
        }
        /* Mutation families, each reaches a distinct parser entrance. */
        if (n>=4u) {
            if (i%4u==0u) {data[0]=0x7fu;data[1]='E';data[2]='L';data[3]='F';}
            if (i%4u==1u) {data[0]='d';data[1]='e';data[2]='x';data[3]='\n';}
            if (i%4u==2u) {data[0]='P';data[1]='K';data[2]=3u;data[3]=4u;}
        }
        q.data=data;q.size=n;q.authorized_source=1u;q.commit=0u;
        q.backend=IR_BACKEND_AUTO;q.advertised_caps=IR_CAP_SCALAR;
        q.verified_caps=IR_CAP_SCALAR;q.watchdog_fuel=10000000u;
        q.watchdog_bytes=10000000u;q.expected_generation=0u;
        if (inventarioraf_step(&state,&q,&x)!=x.status ||
            inventarioraf_step(&state,&q,&y)!=y.status ||
            x.status!=y.status || x.format!=y.format ||
            x.digest_fnv64!=y.digest_fnv64 ||
            x.claim_allowed || y.claim_allowed ||
            x.full_inventory || y.full_inventory ||
            x.committed || y.committed || state.generation!=0u) {
            printf("FAIL PROPERTY_SEED trial=%u\n",i);
            return 1;
        }
    }
    printf("PASS DETERMINISTIC_MUTATIONS trials=%u; claim=false; unchanged_state=true\n",trials);
    return 0;
}
