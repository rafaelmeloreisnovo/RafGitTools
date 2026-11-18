#ifndef RAF_ASTROMETRY_L0_H
#define RAF_ASTROMETRY_L0_H
/* L0 exact discrete astrometry; scale and unit are caller-defined.
 * No OS, libc, heap, floating point, or implicit data completion. */
#if defined(__CHAR_BIT__) && __CHAR_BIT__ != 8
#error "8-bit bytes required"
#endif
_Static_assert(sizeof(short) == 2, "16-bit short required");
_Static_assert(sizeof(unsigned int) == 4, "32-bit unsigned int required");
_Static_assert(sizeof(long long) == 8, "64-bit long long required");

/* Presence is explicit. Zero coordinates and zero time are valid values. */
struct raf_obs2 {
    signed short x, y;
    unsigned short tick;
    unsigned short present; /* must equal 1 */
};

enum raf_geo_status {
    RAF_GEO_OK=0,
    RAF_GEO_MISSING=1,
    RAF_GEO_BAD_EPOCHS=2,
    RAF_GEO_DEGENERATE_CHORD=3,
    RAF_GEO_INVALID_COEFFICIENT=4
};

struct raf_geo3 {
    signed long long area2; /* det(P1-P0,P2-P0): exact, signed */
    unsigned long long chord01_sq, chord12_sq, chord02_sq;
    signed long long acc_x_num2, acc_y_num2;
    /* acceleration = acc_*_num2/(dt01*dt12*dt02), factored exact */
    unsigned int dt01, dt12, dt02;
    unsigned short collinear; /* 1 means exactly collinear */
};

/* No physical cause is inferred. Returns OK for collinear triples with
 * collinear=1; their circumcircle is undefined. Output invalid on error. */
enum raf_geo_status raf_geo3_eval(const struct raf_obs2 p[3],
                                   struct raf_geo3 *out);

/* Exact ray/conic discriminant: Delta=b*b-4*a*c. If Delta>=0, roots
 * are formal (-b+/-sqrt(Delta))/(2*a), never rounded here. */
enum raf_geo_status raf_geo_quadratic_delta(signed short a,
    signed short b, signed short c, signed long long *delta);
#endif
