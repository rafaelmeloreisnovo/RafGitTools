#version 450
// Shader laboratory: visual projection only, no GPU device benchmark evidence.
// Authorial fractional geometry demonstration, not equivalence to fixed Q16 kernel.
layout(location=0) in vec2 uv;
layout(location=0) out vec4 fragColor;
layout(push_constant) uniform Params {
    vec2 origin;
    float scale;
    float phase;
} p;
void main() {
    vec2 v=(uv-p.origin)*p.scale;
    const float h=0.8660254037844386; // sqrt(3)/2
    vec2 basis=vec2(v.x+0.5*v.y, h*v.y);
    float radius=length(basis);
    float wave=0.5+0.5*cos(6.28318530718*radius+p.phase);
    fragColor=vec4(vec3(wave),1.0);
}
