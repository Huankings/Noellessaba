package org.agmas.noellesroles.roles.shadow_jester;

/**
 * 影子小丑的阶段枚举。
 *
 * <p>组件里只持久化数字，是为了让 HUD、回放和胜利规则都能用同一套稳定阶段定义，
 * 避免各处手写 1/2/3/4 后后续调阶段时漏改。</p>
 */
public enum ShadowJesterPhase {
    TASKS(1),
    CHOICE(2),
    VOW_BOUND(3),
    CURTAIN_CALL(4);

    private final int id;

    ShadowJesterPhase(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    public boolean atLeast(ShadowJesterPhase other) {
        return this.id >= other.id;
    }

    public static ShadowJesterPhase fromId(int id) {
        for (ShadowJesterPhase phase : values()) {
            if (phase.id == id) {
                return phase;
            }
        }
        return TASKS;
    }
}
