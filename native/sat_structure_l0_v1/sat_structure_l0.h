#ifndef RAF_SAT_STRUCTURE_L0_H
#define RAF_SAT_STRUCTURE_L0_H
/* Contract: C11 freestanding, no headers, libc, heap, system calls, or FP.
   Separate host harness owns I/O, timing, and external data handling. */
typedef unsigned char raf_u8;
typedef unsigned int raf_u32;
typedef unsigned long long raf_u64;
#define RAF_SAT_N_MAX 32u
#define RAF_SAT_M_MAX 160u
#define RAF_SAT_F_MAX 224u
#define RAF_SAT_WIDTH_MAX 8u
#define RAF_SAT_UNKNOWN (-1)
#define RAF_SAT_WIDTH_BUDGET (-2)
#define RAF_SAT_NODE_BUDGET (-3)
#define RAF_SAT_INVALID (-4)
typedef struct {raf_u8 size;raf_u8 vars[3];raf_u8 neg[3];} raf_sat_clause;
typedef struct {raf_u32 n;raf_u32 m;raf_sat_clause clauses[RAF_SAT_M_MAX];} raf_sat_formula;
typedef struct {raf_u32 mask;raf_u8 alive;raf_u8 truth[1u<<RAF_SAT_WIDTH_MAX];} raf_sat_factor;
typedef struct {raf_sat_factor factors[RAF_SAT_F_MAX];} raf_sat_workspace;
typedef struct {raf_u64 factor_checks;raf_u64 clause_checks;raf_u32 max_width;raf_u32 search_nodes;raf_u32 factors_created;raf_u32 terminal_status;} raf_sat_stats;
/* Return 1 SAT, 0 UNSAT, negative typed failure. Caller zeros stats. */
int raf_sat_eliminate(const raf_sat_formula *,raf_sat_workspace *,raf_sat_stats *);
int raf_sat_dpll(const raf_sat_formula *,raf_sat_stats *);
int raf_sat_check_certificate(const raf_sat_formula *,raf_u32 assignment);
#endif
