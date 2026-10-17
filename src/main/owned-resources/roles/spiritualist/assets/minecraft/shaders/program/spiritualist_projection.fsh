#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));

    /*
     * 轻微压暗后再染成偏紫的灵视色。
     * 这样既能保留场景亮度层次，也能明显区分“当前处于出窍状态”。
     */
    vec3 purpleTint = vec3(0.57, 0.43, 0.95);
    vec3 finalColor = mix(color.rgb * 0.45, purpleTint * luminance, 0.7);
    fragColor = vec4(finalColor, color.a);
}
