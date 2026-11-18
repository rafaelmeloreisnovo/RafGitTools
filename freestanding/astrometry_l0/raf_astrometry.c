#include "raf_astrometry.h"

enum raf_geo_status raf_geo3_eval(const struct raf_obs2 p[3],
                                   struct raf_geo3 *out)
{
    signed long long x01, y01, x12, y12, x02, y02;
    unsigned int t01, t12, t02;
    struct raf_geo3 r;
    if (!p || !out) return RAF_GEO_MISSING;
    if (p[0].present != 1 || p[1].present != 1 || p[2].present != 1)
        return RAF_GEO_MISSING;
    if (!(p[0].tick < p[1].tick && p[1].tick < p[2].tick))
        return RAF_GEO_BAD_EPOCHS;
    x01=(signed long long)p[1].x-p[0].x;
    y01=(signed long long)p[1].y-p[0].y;
    x12=(signed long long)p[2].x-p[1].x;
    y12=(signed long long)p[2].y-p[1].y;
    x02=(signed long long)p[2].x-p[0].x;
    y02=(signed long long)p[2].y-p[0].y;
    r.chord01_sq=(unsigned long long)(x01*x01+y01*y01);
    r.chord12_sq=(unsigned long long)(x12*x12+y12*y12);
    r.chord02_sq=(unsigned long long)(x02*x02+y02*y02);
    if (!r.chord01_sq || !r.chord12_sq || !r.chord02_sq)
        return RAF_GEO_DEGENERATE_CHORD;
    r.area2=x01*y02-y01*x02;
    r.collinear=(r.area2==0);
    t01=(unsigned int)p[1].tick-p[0].tick;
    t12=(unsigned int)p[2].tick-p[1].tick;
    t02=(unsigned int)p[2].tick-p[0].tick;
    r.dt01=t01; r.dt12=t12; r.dt02=t02;
    /* exact second divided difference: 2[(P2-P1)*dt01-(P1-P0)*dt12]
       / (dt01*dt12*dt02). No float, sqrt, or large denominator product. */
    r.acc_x_num2=2*(x12*t01-x01*t12);
    r.acc_y_num2=2*(y12*t01-y01*t12);
    *out=r;
    return RAF_GEO_OK;
}

enum raf_geo_status raf_geo_quadratic_delta(signed short a,
    signed short b, signed short c, signed long long *delta)
{
    if (!delta || a==0) return RAF_GEO_INVALID_COEFFICIENT;
    *delta=(signed long long)b*b-4*(signed long long)a*c;
    return RAF_GEO_OK;
}
