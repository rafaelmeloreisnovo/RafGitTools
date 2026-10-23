#ifndef RFL_LENS_H
#define RFL_LENS_H
/* L0 RAFAELIA Lens ABI v1: C11 freestanding. No libc, OS, allocation, IO. */
#if defined(__CHAR_BIT__) && __CHAR_BIT__ != 8
#error "RFL L0 requires 8-bit bytes"
#endif
_Static_assert(sizeof(unsigned) == 4, "RFL L0 requires 32-bit unsigned");
#define RFL_DIM 64u
#define RFL_MAX_TOKENS 16u
#define RFL_MAX_NODES 64u
#define RFL_MAX_EDGES 64u
#define RFL_MAX_DEPTH 3u

enum rfl_status { RFL_OK = 0, RFL_INVALID = 1, RFL_DUPLICATE = 2, RFL_CAPACITY = 3 };
/* No implicit mapping of unknown to zero; 0 means invalid enum. */
enum rfl_source { RFL_DRIVE_PRIVATE=1, RFL_DRIVE_DASHBOARD, RFL_GITHUB_PRIVATE,
    RFL_GITHUB_PUBLIC, RFL_MAPA_PUBLIC, RFL_PAPER_RESEARCH, RFL_ETHICS_RESEARCH };
enum rfl_gap { RFL_TOKEN_VAZIO=1, RFL_MISSING_AUTHORITY, RFL_MISSING_EVIDENCE,
    RFL_UNVERIFIED, RFL_EVIDENCED_SCOPED };
struct rfl_span { const unsigned char *ptr; unsigned len; };
struct rfl_node { struct rfl_span logical_id; unsigned source; unsigned gap;
    const struct rfl_span *tokens; unsigned token_count; };
struct rfl_vector { unsigned char bins[RFL_DIM]; unsigned char digest[32]; unsigned occupied; };
struct rfl_edge { unsigned char from, to, shared, united; };
struct rfl_sha256 { unsigned h[8], bit_low, bit_high, used; unsigned char block[64]; };
void rfl_sha_init(struct rfl_sha256 *s);
void rfl_sha_update(struct rfl_sha256 *s, const void *bytes, unsigned count);
void rfl_sha_final(struct rfl_sha256 *s, unsigned char out[32]);
void rfl_sha(const void *bytes, unsigned count, unsigned char out[32]);
enum rfl_status rfl_vectorize(const struct rfl_node *node, struct rfl_vector *out);
enum rfl_status rfl_candidates(const struct rfl_node *nodes, unsigned count,
    struct rfl_edge *out, unsigned capacity, unsigned *written);
enum rfl_status rfl_reach(const struct rfl_edge *edges, unsigned edge_count,
    unsigned node_count, unsigned start, unsigned depth, unsigned char *seen);
#endif
