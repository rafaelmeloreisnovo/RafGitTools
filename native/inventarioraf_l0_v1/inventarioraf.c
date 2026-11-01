/* SPDX-License-Identifier: GPL-3.0-or-later
 * inventarioRAF: bounded, freestanding, deterministic format probe.
 * No libc calls. All parses are little-endian and length-checked.
 */
#include "inventarioraf.h"

#define IR16(b,p) ((ir_u32)(b)[p] | ((ir_u32)(b)[(p)+1u] << 8))
#define IR32(b,p) ((ir_u32)IR16(b,p) | ((ir_u32)IR16(b,(p)+2u) << 16))
#define IR64(b,p) ((ir_u64)IR32(b,p) | ((ir_u64)IR32(b,(p)+4u) << 32))

/* These static helpers are compiler-local, not platform/runtime imports. */
static ir_u32 ir_fit(ir_size n, ir_size off, ir_size amount) {
    return (off <= n && amount <= n - off) ? 1u : 0u;
}
static ir_u32 ir_sig(const ir_u8 *b, ir_size n, ir_size off,
                     ir_u8 a, ir_u8 c, ir_u8 d, ir_u8 e) {
    return ir_fit(n, off, 4u) &&
           b[off] == a && b[off+1u] == c &&
           b[off+2u] == d && b[off+3u] == e;
}
static ir_u32 ir_table(ir_size n, ir_u64 offset, ir_u32 count, ir_u32 stride) {
    if (offset > (ir_u64)n || (count && stride == 0u)) return 0u;
    return !count || ((ir_u64)count * (ir_u64)stride) <= ((ir_u64)n-offset);
}
static ir_u32 ir_zip_name_safe(const ir_u8 *s, ir_u32 n) {
    ir_u32 i;
    if (!n || s[0] == (ir_u8)'/') return 0u;
    for (i = 0u; i < n; ++i) {
        if (s[i] == 0u || s[i] == (ir_u8)'\\' || s[i] == (ir_u8)':')
            return 0u;
        if (s[i] == (ir_u8)'.' &&
            (i == 0u || s[i-1u] == (ir_u8)'/') &&
            i + 1u < n && s[i+1u] == (ir_u8)'.' &&
            (i + 2u == n || s[i+2u] == (ir_u8)'/')) return 0u;
    }
    return 1u;
}
static ir_u32 ir_zip(const ir_u8 *b, ir_size n, ir_u32 *entries) {
    ir_size p = 0u, cdir_start = 0u;
    ir_u32 locals = 0u, central = 0u;
    while (ir_sig(b,n,p,0x50u,0x4bu,0x03u,0x04u)) {
        ir_u32 flags, method, compressed, expanded, name, extra;
        ir_size body;
        if (!ir_fit(n,p,30u) || locals >= IR_MAX_MEMBERS) return IR_BLOCKED;
        flags=IR16(b,p+6u); method=IR16(b,p+8u);
        compressed=IR32(b,p+18u); expanded=IR32(b,p+22u);
        name=IR16(b,p+26u); extra=IR16(b,p+28u);
        if ((flags & 0x0009u) || (method != 0u && method != 8u) ||
            compressed == 0xffffffffu || expanded == 0xffffffffu)
            return IR_UNSUPPORTED;
        if (!ir_fit(n,p+30u,(ir_size)name+(ir_size)extra) ||
            !ir_zip_name_safe(b+p+30u,name)) return IR_BLOCKED;
        if ((!compressed && expanded) ||
            (compressed && (ir_u64)expanded > (ir_u64)compressed * 200u))
            return IR_BLOCKED;
        body=p+30u+(ir_size)name+(ir_size)extra;
        if (!ir_fit(n,body,(ir_size)compressed)) return IR_BLOCKED;
        p=body+(ir_size)compressed;
        ++locals;
    }
    cdir_start=p;
    while (ir_sig(b,n,p,0x50u,0x4bu,0x01u,0x02u)) {
        ir_u32 name, extra, comment;
        ir_size span;
        if (!ir_fit(n,p,46u) || central >= IR_MAX_MEMBERS) return IR_BLOCKED;
        /* Central-directory UNIX symlink entries must not be followed by hosts. */
        if (((IR32(b,p+38u) >> 16) & 0170000u) == 0120000u) return IR_BLOCKED;
        name=IR16(b,p+28u); extra=IR16(b,p+30u); comment=IR16(b,p+32u);
        span=46u+(ir_size)name+(ir_size)extra+(ir_size)comment;
        if (!ir_fit(n,p,span) || !ir_zip_name_safe(b+p+46u,name))
            return IR_BLOCKED;
        p+=span;
        ++central;
    }
    if (!ir_sig(b,n,p,0x50u,0x4bu,0x05u,0x06u) ||
        !ir_fit(n,p,22u) || locals != central ||
        IR16(b,p+8u) != (ir_u32)central ||
        IR16(b,p+10u) != (ir_u32)central ||
        IR32(b,p+12u) != (ir_u32)(p-cdir_start) ||
        IR32(b,p+16u) != (ir_u32)cdir_start ||
        !ir_fit(n,p+22u,(ir_size)IR16(b,p+20u)) ||
        p+22u+(ir_size)IR16(b,p+20u) != n)
        return IR_BLOCKED;
    *entries=locals;
    return IR_OK; /* headers only: no inflate / CRC / content attestation */
}

ir_u32 inventarioraf_step(struct ir_state *state,
                          const struct ir_request *q,
                          struct ir_receipt *r) {
    ir_u32 status=IR_TOKEN_VAZIO, kind=IR_FORMAT_UNKNOWN;
    ir_u32 warnings=IR_WARN_HOST_REQUIRED;
    ir_u32 arch=0u, cls=0u, entries=0u;
    ir_u64 hash=0xcbf29ce484222325ull;
    ir_size i;

    if (!r) return IR_BLOCKED;
    r->status=IR_BLOCKED; r->format=IR_FORMAT_UNKNOWN;
    r->warnings=IR_WARN_HOST_REQUIRED; r->backend_used=IR_BACKEND_SCALAR;
    r->entries_observed=0u; r->arch_machine=0u; r->format_class=0u;
    r->committed=0u; r->bytes_seen=0u; r->digest_fnv64=0u;
    r->generation=state ? state->generation : 0u;
    r->claim_allowed=0u; r->full_inventory=0u;

    if (!state || !q || !q->data || !q->authorized_source ||
        q->commit > 1u || q->size == 0u || q->size > IR_MAX_BYTES ||
        q->expected_generation != state->generation ||
        state->generation == ~(ir_u64)0u ||
        (q->verified_caps & ~q->advertised_caps)) return IR_BLOCKED;
    /* Conservative total work ceiling: hashing + bounds/name/table passes.
       The host must enforce elapsed-time/thermal watchdogs separately. */
    if ((ir_u64)q->size > q->watchdog_bytes ||
        ((ir_u64)q->size * 4u + (IR_MAX_MEMBERS * 32u)) > q->watchdog_fuel) {
        r->status=IR_BUDGET;
        return IR_BUDGET;
    }
    if (q->backend != IR_BACKEND_AUTO && q->backend != IR_BACKEND_SCALAR) {
        if (q->backend > IR_BACKEND_NUMA ||
            !(q->verified_caps & IR_CAP_SCALAR)) return IR_BLOCKED;
        warnings |= IR_WARN_FALLBACK;
    }
    /* Core implements SCALAR ONLY, never simulates hardware acceleration. */
    for (i=0u;i<q->size;i++) {
        hash = (hash ^ (ir_u64)q->data[i]) * 0x100000001b3ull;
    }
    if (ir_sig(q->data,q->size,0u,0x7fu,0x45u,0x4cu,0x46u)) {
        ir_u64 phoff, shoff;
        ir_u32 phent, phnum, shent, shnum, ehsize, minhdr;
        kind=IR_FORMAT_ELF;
        if (!ir_fit(q->size,0u,52u)) status=IR_BLOCKED;
        else {
            cls=q->data[4]; minhdr=cls==1u?52u:64u;
            if ((cls!=1u && cls!=2u) || q->data[5]!=1u ||
                q->data[6]!=1u || !ir_fit(q->size,0u,minhdr)) status=IR_UNSUPPORTED;
            else {
                arch=IR16(q->data,18u);
                if (cls==1u) {
                    phoff=IR32(q->data,28u); shoff=IR32(q->data,32u);
                    ehsize=IR16(q->data,40u); phent=IR16(q->data,42u);
                    phnum=IR16(q->data,44u); shent=IR16(q->data,46u);
                    shnum=IR16(q->data,48u);
                } else {
                    phoff=IR64(q->data,32u); shoff=IR64(q->data,40u);
                    ehsize=IR16(q->data,52u); phent=IR16(q->data,54u);
                    phnum=IR16(q->data,56u); shent=IR16(q->data,58u);
                    shnum=IR16(q->data,60u);
                }
                if (ehsize!=minhdr || (phnum && phent<(cls==1u?32u:56u)) ||
                    (shnum && shent<(cls==1u?40u:64u)) ||
                    !ir_table(q->size,phoff,phnum,phent) ||
                    !ir_table(q->size,shoff,shnum,shent)) status=IR_BLOCKED;
                else if (shoff && shnum==0u) status=IR_UNSUPPORTED; /* ELF extended count */
                else status=IR_OK;
                if (arch!=40u && arch!=183u && arch!=62u && arch!=243u)
                    warnings |= IR_WARN_UNKNOWN_ARCH;
            }
        }
    } else if (ir_sig(q->data,q->size,0u,'d','e','x','\n')) {
        kind=IR_FORMAT_DEX;
        if (!ir_fit(q->size,0u,112u)) status=IR_BLOCKED;
        else if (q->data[7]!=0u || q->data[4]<'0' || q->data[4]>'9' ||
                 q->data[5]<'0' || q->data[5]>'9' ||
                 q->data[6]<'0' || q->data[6]>'9') status=IR_UNSUPPORTED;
        else if (IR32(q->data,32u)!=(ir_u32)q->size ||
                 IR32(q->data,36u)!=112u ||
                 IR32(q->data,40u)!=0x12345678u) status=IR_BLOCKED;
        else status=IR_OK;
        warnings |= IR_WARN_PARTIAL_FORMAT; /* no DEX method/class graph */
    } else if (ir_sig(q->data,q->size,0u,0x50u,0x4bu,0x03u,0x04u)) {
        kind=IR_FORMAT_ZIP;
        status=ir_zip(q->data,q->size,&entries);
        warnings |= IR_WARN_PARTIAL_FORMAT; /* ZIP metadata, not decompression */
    } else if (ir_sig(q->data,q->size,0u,'o','a','t','\n')) {
        kind=IR_FORMAT_ART;
        status=IR_UNSUPPORTED; /* ART runtime formats are version-specific */
    } else status=IR_TOKEN_VAZIO;

    r->status=status; r->format=kind;
    r->warnings=warnings; r->arch_machine=arch; r->format_class=cls;
    r->entries_observed=entries; r->bytes_seen=(ir_u64)q->size;
    r->digest_fnv64=hash;
    if (status==IR_OK && q->commit) {
        /* Commit is atomic at the caller's single-threaded state boundary.
         * Multithreaded callers MUST serialize or use a host-side transaction.
         */
        state->generation+=1u;
        state->last_digest_fnv64=hash;
        state->last_format=kind;
        state->last_entries=entries;
        r->committed=1u;
        r->generation=state->generation;
    }
    return status;
}
