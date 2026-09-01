#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    // 交换红蓝通道并轻微提高对比度，模拟旁观末影人的反转色视角。
    vec3 inverted = vec3(1.0 - color.b, 1.0 - color.g, 1.0 - color.r);
    fragColor = vec4(mix(color.rgb, inverted, 0.92), color.a);
}
