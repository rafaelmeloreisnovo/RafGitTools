/* Hosted test harness, never linked into L0. */
#include "raf_astrometry.h"
#include <stdio.h>
#define CHECK(x) do { if (!(x)) { fprintf(stderr, "FAIL %d: %s\n",__LINE__,#x);return 1; } } while (0)
int main(void) {
    struct raf_obs2 p[3]={{0,0,0,1},{3,0,2,1},{3,4,5,1}};
    struct raf_geo3 r, old;
    signed long long delta=777;
    unsigned k;
    CHECK(raf_geo3_eval(p,&r)==RAF_GEO_OK);
    CHECK(r.area2==12 && r.collinear==0);
    CHECK(r.chord01_sq==9 && r.chord12_sq==16 && r.chord02_sq==25);
    CHECK(r.acc_x_num2==-18 && r.acc_y_num2==16);
    CHECK(r.dt01==2 && r.dt12==3 && r.dt02==5);
    old=r;
    CHECK(raf_geo_quadratic_delta(1,0,-1,&delta)==RAF_GEO_OK && delta==4);
    CHECK(raf_geo_quadratic_delta(1,2,1,&delta)==RAF_GEO_OK && delta==0);
    CHECK(raf_geo_quadratic_delta(1,0,1,&delta)==RAF_GEO_OK && delta==-4);
    CHECK(raf_geo_quadratic_delta(0,2,1,&delta)==RAF_GEO_INVALID_COEFFICIENT);
    CHECK(raf_geo_quadratic_delta(1,2,1,0)==RAF_GEO_INVALID_COEFFICIENT);
    CHECK(raf_geo3_eval(0,&r)==RAF_GEO_MISSING);
    CHECK(raf_geo3_eval(p,0)==RAF_GEO_MISSING);
    p[2].present=0;
    CHECK(raf_geo3_eval(p,&r)==RAF_GEO_MISSING && r.area2==old.area2);
    p[2].present=1;
    p[2].tick=2;
    CHECK(raf_geo3_eval(p,&r)==RAF_GEO_BAD_EPOCHS);
    p[2].tick=5; p[2].x=3; p[2].y=0;
    CHECK(raf_geo3_eval(p,&r)==RAF_GEO_DEGENERATE_CHORD);
    p[2].x=6; p[2].y=0;
    CHECK(raf_geo3_eval(p,&r)==RAF_GEO_OK && r.collinear==1 && r.area2==0);
    p[0].x=-32768; p[0].y=-32768; p[0].tick=0;
    p[1].x=32767; p[1].y=-32768; p[1].tick=32767;
    p[2].x=-32768; p[2].y=32767; p[2].tick=65535;
    CHECK(raf_geo3_eval(p,&r)==RAF_GEO_OK);
    CHECK(r.area2==4294836225LL && r.chord01_sq==4294836225ULL);
    CHECK(r.chord02_sq==4294836225ULL && r.chord12_sq==8589672450ULL);
    /* 10k differential mathematical invariants under translation/time shift */
    for (k=0;k<10000u;k++) {
        signed short dx=(signed short)(k%97u),dy=(signed short)(k%71u);
        struct raf_geo3 s;
        p[0]=(struct raf_obs2){(signed short)(dx),(signed short)(dy),10,1};
        p[1]=(struct raf_obs2){(signed short)(dx+3),(signed short)(dy),12,1};
        p[2]=(struct raf_obs2){(signed short)(dx+3),(signed short)(dy+4),15,1};
        CHECK(raf_geo3_eval(p,&s)==RAF_GEO_OK && s.area2==12 && s.acc_x_num2==-18 && s.acc_y_num2==16);
    }
    puts("HOST_SYNTHETIC_TESTS:PASS_SCOPED 10000+ cases");
    return 0;
}
