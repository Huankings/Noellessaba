package org.agmas.noellesroles.registry;

import net.minecraft.util.Identifier;

/**
 * NoellesRoles 自定义死因 id。
 *
 * <p>死因会被死亡链、回放和跨模组保护逻辑共同使用，集中到这里后，
 * 新增死因时不需要再翻入口类里的大段初始化代码。</p>
 */
public final class NoellesDeathReasons {
    public static final Identifier FAKE_DEATH_REASON = NoellesRolesCore.id("fake");
    public static final Identifier STALKER_EXECUTION_DEATH = NoellesRolesCore.id("stalker_execution");
    public static final Identifier DEATH_REASON_BOMB = NoellesRolesCore.id("bomb");
    public static final Identifier DEATH_REASON_THROWING_AXE = NoellesRolesCore.id("throwing_axe");
    public static final Identifier DEATH_REASON_AXE = NoellesRolesCore.id("axe");
    public static final Identifier DEATH_REASON_SEDATIVE_OVERDOSE = NoellesRolesCore.id("sedative_overdose");
    public static final Identifier DEATH_REASON_SNIPER_RIFLE = NoellesRolesCore.id("sniper_rifle");
    public static final Identifier ARSONIST_IGNITED_DEATH_REASON = NoellesRolesCore.id("ignited");
    public static final Identifier ARSONIST_FAILED_IGNITE_DEATH_REASON = NoellesRolesCore.id("failed_ignite");
    public static final Identifier CONVENER_COUNTER_KILL_DEATH_REASON = NoellesRolesCore.id("convener_counter_kill");
    public static final Identifier SILENCED_OUTSIDE_DEATH_REASON = NoellesRolesCore.id("silenced_and_outside");
    public static final Identifier SILENCED_TAPE_REMOVED_DEATH_REASON = NoellesRolesCore.id("tape_removed_low_mood");
    public static final Identifier ANGEL_SACRIFICE_DEATH_REASON = NoellesRolesCore.id("angel_sacrifice");
    public static final Identifier SPIRITUALIST_SOUL_GUARD_DEATH_REASON = NoellesRolesCore.id("spiritualist_soul_guard");
    public static final Identifier VOODOO_MAGIC_DEATH_REASON = NoellesRolesCore.id("voodoo");
    public static final Identifier GUESS_EXPLODE_DEATH_REASON = NoellesRolesCore.id("guess_explode");
    public static final Identifier GUESS_EXPLODE_NEARBY_DEATH_REASON = NoellesRolesCore.id("guess_explode_nearby");
    public static final Identifier ALLERGIES_DEATH_REASON = NoellesRolesCore.id("allergies");
    public static final Identifier BROKEN_HEART_DEATH_REASON = NoellesRolesCore.id("broken_heart");
    public static final Identifier MODDED_BACKFIRE_DEATH_REASON = NoellesRolesCore.id("modded_backfire");
    public static final Identifier MENTAL_BREAKDOWN_DEATH_REASON = NoellesRolesCore.id("mental_breakdown");
    public static final Identifier DUAL_ACTIVE_TIMEOUT_DEATH_REASON = NoellesRolesCore.id("dual_active_timeout");
    public static final Identifier FAILED_INITIATION_DEATH_REASON = NoellesRolesCore.id("failed_initiation");
    public static final Identifier JASON_THROWING_WEAPON_DEATH_REASON = NoellesRolesCore.id("throwing_weapon");
    public static final Identifier JASON_BLEEDING_TOO_MUCH_DEATH_REASON = NoellesRolesCore.id("bleeding_too_much");
    public static final Identifier JASON_BURN_DEATH_REASON = NoellesRolesCore.id("burn");
    public static final Identifier SKELETON_DEATH_REASON = NoellesRolesCore.id("skeleton");
    /** 颠倒标记反噬造成的致死伤害。 */
    public static final Identifier REVERSE_DEATH_REASON = NoellesRolesCore.id("reverse");

    private NoellesDeathReasons() {
    }
}
