package org.agmas.noellesroles.roles.coroner;

/**
 * 验尸官相关 HUD / 射线常量。
 *
 * <p>把这些位置和范围单独收进常量类，后续如果再想微调尸体信息、检查提示、
 * 理智警告的位置，不需要回头在多个 HUD handler 里逐个找坐标。</p>
 */
public final class CoronerConstants {
    private CoronerConstants() {
    }

    public static final float BODY_HUD_RANGE = 3.0F;
    public static final float BODY_EXAMINE_RANGE = 3.0F;

    public static final float HUD_TRANSLATE_Y = 6.0F;
    public static final int SANITY_REQUIREMENTS_Y = 32;
    public static final int BODY_INFO_Y = 32;
    public static final int BODY_ROLE_INFO_Y = 48;

    public static final int BODY_EXAMINE_PROMPT_Y = 0;
    public static final int BODY_EXAMINE_STATS_Y = 12;
}
