#include "../app/src/main/cpp/atlas_core.h"
int main(void){
 atlas_traits a=atlas_classify("000051_sample.JSONL",19u,0);
 atlas_traits b=atlas_classify("something.dat",13u,0);
 atlas_traits c=atlas_classify("00012",5u,1);
 atlas_traits d=atlas_classify("abc",3u,0);
 if(a.kind!=3u||a.numeric_prefix!=6u)return 1;
 if(b.kind!=4u||b.extension_offset!=10u)return 2;
 if(c.kind!=1u||d.kind!=8u)return 3;
 if(atlas_classify(0,0,0).kind!=8u)return 4;
 return 0;
}
