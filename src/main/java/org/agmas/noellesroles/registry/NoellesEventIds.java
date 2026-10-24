package org.agmas.noellesroles.registry;

import net.minecraft.util.Identifier;

/**
 * NoellesRoles 的回放事件、托盘/床效果和护盾来源 id。
 *
 * <p>这些 id 不是职业本身，而是“玩法事件”的稳定协议字段。
 * 拆出来后，记录事件的业务代码和注册 formatter 的代码可以共享同一份定义。</p>
 */
public final class NoellesEventIds {
    /** 酸液托盘扩展效果 id。 */
    public static final Identifier KILLER_QUEEN_ACID_TRAY_EFFECT = NoellesRolesCore.id("killer_queen_acid");
    /** 酸液拿取后添加杀手皇后标记。 */
    public static final Identifier KILLER_QUEEN_ACID_MARK_EVENT = NoellesRolesCore.id("killer_queen_acid_mark");
    /** 酸液给予杀手皇后或机器人移速加成。 */
    public static final Identifier KILLER_QUEEN_SPEED_START_EVENT = NoellesRolesCore.id("killer_queen_speed_start");
    /** 酸液移速加成自然结束。 */
    public static final Identifier KILLER_QUEEN_SPEED_END_EVENT = NoellesRolesCore.id("killer_queen_speed_end");
    /** 酸液使普通玩家进入晕眩状态。 */
    public static final Identifier KILLER_QUEEN_DAZE_START_EVENT = NoellesRolesCore.id("killer_queen_daze_start");
    /** 酸液晕眩状态结束。 */
    public static final Identifier KILLER_QUEEN_DAZE_END_EVENT = NoellesRolesCore.id("killer_queen_daze_end");
    /** 标记法杖命中后开始定身。 */
    public static final Identifier KILLER_QUEEN_ROOT_START_EVENT = NoellesRolesCore.id("killer_queen_root_start");
    /** 标记法杖定身结束。 */
    public static final Identifier KILLER_QUEEN_ROOT_END_EVENT = NoellesRolesCore.id("killer_queen_root_end");
    /** 疯魔期间由标记法杖添加标记。 */
    public static final Identifier KILLER_QUEEN_PSYCHO_MARK_EVENT = NoellesRolesCore.id("killer_queen_psycho_mark");
    /** 标记法杖累计两次有效首次标记后增加疯魔护盾。 */
    public static final Identifier KILLER_QUEEN_PSYCHO_SHIELD_GAIN_EVENT = NoellesRolesCore.id("killer_queen_psycho_shield_gain");
    /** 疯魔真正结束后回退本轮新增标记。 */
    public static final Identifier KILLER_QUEEN_MARK_ROLLBACK_EVENT = NoellesRolesCore.id("killer_queen_mark_rollback");
    /** 杀手皇后洗牌杀手职业。 */
    public static final Identifier KILLER_QUEEN_SHUFFLE_EVENT = NoellesRolesCore.id("killer_queen_shuffle");
    /** 杀手皇后招募玩家。 */
    public static final Identifier KILLER_QUEEN_RECRUIT_EVENT = NoellesRolesCore.id("killer_queen_recruit");
    /** 被招募玩家锁定初始击杀目标。 */
    public static final Identifier KILLER_QUEEN_RECRUIT_TARGET_EVENT = NoellesRolesCore.id("killer_queen_recruit_target");
    /** 被招募玩家因旧目标失效而切换击杀目标。 */
    public static final Identifier KILLER_QUEEN_RECRUIT_TARGET_CHANGED_EVENT = NoellesRolesCore.id("killer_queen_recruit_target_changed");
    public static final Identifier DEFENSE_TRAY_EFFECT = NoellesRolesCore.id("defense_vial");
    public static final Identifier DELUSION_TRAY_EFFECT = NoellesRolesCore.id("delusion_vial");
    public static final Identifier SEDATIVE_TRAY_EFFECT = NoellesRolesCore.id("sedative");
    public static final Identifier DELUSION_STARTED_EVENT = NoellesRolesCore.id("delusion_started");
    public static final Identifier DELUSION_ENDED_EVENT = NoellesRolesCore.id("delusion_ended");
    public static final Identifier COWARD_DANGER_SENSED_EVENT = NoellesRolesCore.id("coward_danger_sensed");
    public static final Identifier COWARD_DANGER_LEFT_EVENT = NoellesRolesCore.id("coward_danger_left");
    public static final Identifier SEDATIVE_STARTED_EVENT = NoellesRolesCore.id("sedative_started");
    public static final Identifier SEDATIVE_ENDED_EVENT = NoellesRolesCore.id("sedative_ended");
    public static final Identifier TIMED_BOMB_ACTIVATED_EVENT = NoellesRolesCore.id("timed_bomb_activated");
    public static final Identifier TIMED_BOMB_TRAY_EMBEDDED_EVENT = NoellesRolesCore.id("timed_bomb_tray_embedded");
    public static final Identifier TIMED_BOMB_BED_EMBEDDED_EVENT = NoellesRolesCore.id("timed_bomb_bed_embedded");
    public static final Identifier TIMED_BOMB_BED_TRIGGERED_EVENT = NoellesRolesCore.id("timed_bomb_bed_triggered");
    public static final Identifier ROLE_MINE_DETECTED_EVENT = NoellesRolesCore.id("role_mine_detected");
    public static final Identifier ROLE_MINE_REPORT_EVENT = NoellesRolesCore.id("role_mine_report");
    public static final Identifier CAPTURE_DEVICE_TRIGGERED_EVENT = NoellesRolesCore.id("capture_device_triggered");
    public static final Identifier CAPTURE_DEVICE_REPORT_EVENT = NoellesRolesCore.id("capture_device_report");
    public static final Identifier CAPTURE_DEVICE_EXPIRED_EVENT = NoellesRolesCore.id("capture_device_expired");
    public static final Identifier CAPTURE_DEVICE_RELEASED_EVENT = NoellesRolesCore.id("capture_device_released");
    public static final Identifier POWER_RESTORED_EVENT = NoellesRolesCore.id("power_restored");
    public static final Identifier JESTER_PSYCHO_STARTED_EVENT = NoellesRolesCore.id("jester_psycho_started");
    public static final Identifier EXECUTIONER_TARGET_LOCKED_EVENT = NoellesRolesCore.id("executioner_target_locked");
    public static final Identifier EXECUTIONER_TARGET_CHANGED_EVENT = NoellesRolesCore.id("executioner_target_changed");
    public static final Identifier BOUNTY_HUNTER_TARGET_LOCKED_EVENT = NoellesRolesCore.id("bounty_hunter_target_locked");
    public static final Identifier BOUNTY_HUNTER_TARGET_CHANGED_EVENT = NoellesRolesCore.id("bounty_hunter_target_changed");
    public static final Identifier VULTURE_PROGRESS_EVENT = NoellesRolesCore.id("vulture_progress");
    public static final Identifier RECALLER_POSITION_SAVED_EVENT = NoellesRolesCore.id("recaller_position_saved");
    public static final Identifier RECALLER_TELEPORTED_EVENT = NoellesRolesCore.id("recaller_teleported");
    public static final Identifier RECALLER_ENDER_PEARL_THROWN_EVENT = NoellesRolesCore.id("recaller_ender_pearl_thrown");
    public static final Identifier PHANTOM_INVISIBILITY_STARTED_EVENT = NoellesRolesCore.id("phantom_invisibility_started");
    public static final Identifier PHANTOM_INVISIBILITY_ENDED_EVENT = NoellesRolesCore.id("phantom_invisibility_ended");
    public static final Identifier PROPHET_MARKED_EVENT = NoellesRolesCore.id("prophet_marked");
    public static final Identifier PROPHET_REMARKED_EVENT = NoellesRolesCore.id("prophet_remarked");
    public static final Identifier PROPHET_REVEALED_EVENT = NoellesRolesCore.id("prophet_revealed");
    public static final Identifier PROPHET_VOODOO_IMMUNITY_EVENT = NoellesRolesCore.id("prophet_voodoo_immunity");
    public static final Identifier WINDER_WIND_MARK_APPLIED_EVENT = NoellesRolesCore.id("winder_wind_mark_applied");
    public static final Identifier WINDER_WIND_CHARGE_USED_EVENT = NoellesRolesCore.id("winder_wind_charge_used");
    public static final Identifier WINDER_WIND_MARK_EXPIRED_EVENT = NoellesRolesCore.id("winder_wind_mark_expired");
    public static final Identifier WINDER_WIND_MARK_TRIGGERED_EVENT = NoellesRolesCore.id("winder_wind_mark_triggered");
    public static final Identifier WINDER_FLOAT_STARTED_EVENT = NoellesRolesCore.id("winder_float_started");
    public static final Identifier WINDER_FLOAT_ENDED_EVENT = NoellesRolesCore.id("winder_float_ended");
    public static final Identifier WINDER_FLOAT_STOPPED_EARLY_EVENT = NoellesRolesCore.id("winder_float_stopped_early");
    public static final Identifier AMNESIAC_ROLE_STOLEN_EVENT = NoellesRolesCore.id("amnesiac_role_stolen");
    public static final Identifier ARSONIST_DOUSED_EVENT = NoellesRolesCore.id("arsonist_doused");
    public static final Identifier ARSONIST_LIGHTER_COOLDOWN_STARTED_EVENT = NoellesRolesCore.id("arsonist_lighter_cooldown_started");
    public static final Identifier ARSONIST_LIGHTER_COOLDOWN_FINISHED_EVENT = NoellesRolesCore.id("arsonist_lighter_cooldown_finished");
    public static final Identifier CONVENER_SUMMON_EVENT = NoellesRolesCore.id("convener_summon");
    public static final Identifier CONVENER_COUNTER_SHIELD_GAINED_EVENT = NoellesRolesCore.id("convener_counter_shield_gained");
    public static final Identifier CONVENER_VOODOO_IMMUNITY_EVENT = NoellesRolesCore.id("convener_voodoo_immunity");
    public static final Identifier CONVENER_COUNTER_SHIELD_SOURCE = NoellesRolesCore.id("convener_counter_shield");
    public static final Identifier ALLERGIC_POISON_TRIGGERED_EVENT = NoellesRolesCore.id("allergic_poison_triggered");
    public static final Identifier ALLERGIC_INSTINCT_TRIGGERED_EVENT = NoellesRolesCore.id("allergic_instinct_triggered");
    public static final Identifier ALLERGIC_INSTINCT_ENDED_EVENT = NoellesRolesCore.id("allergic_instinct_ended");
    public static final Identifier ALLERGIC_SHIELD_GAINED_EVENT = NoellesRolesCore.id("allergic_shield_gained");
    public static final Identifier ALLERGIC_SHIELD_SOURCE = NoellesRolesCore.id("allergic_shield");
    public static final Identifier DUAL_ACTIVE_STARTED_EVENT = NoellesRolesCore.id("dual_active_started");
    public static final Identifier STALKER_PHASE_ADVANCE_1_TO_2_EVENT = NoellesRolesCore.id("stalker_phase_1_to_2");
    public static final Identifier STALKER_PHASE_ADVANCE_2_TO_3_EVENT = NoellesRolesCore.id("stalker_phase_2_to_3");
    public static final Identifier STALKER_PHASE_REGRESS_3_TO_2_EVENT = NoellesRolesCore.id("stalker_phase_3_to_2");
    public static final Identifier NOISEMAKER_GLOW_STARTED_EVENT = NoellesRolesCore.id("noisemaker_glow_started");
    public static final Identifier NOISEMAKER_GLOW_ENDED_EVENT = NoellesRolesCore.id("noisemaker_glow_ended");
    public static final Identifier MORPHLING_MORPH_STARTED_EVENT = NoellesRolesCore.id("morphling_morph_started");
    public static final Identifier MORPHLING_MORPH_ENDED_EVENT = NoellesRolesCore.id("morphling_morph_ended");
    public static final Identifier MORPH_REAGENT_SAMPLED_EVENT = NoellesRolesCore.id("morph_reagent_sampled");
    public static final Identifier MORPH_REAGENT_MARKED_EVENT = NoellesRolesCore.id("morph_reagent_marked");
    public static final Identifier MORPH_MARK_TRIGGERED_EVENT = NoellesRolesCore.id("morph_mark_triggered");
    public static final Identifier MORPH_MARK_ENDED_EVENT = NoellesRolesCore.id("morph_mark_ended");
    public static final Identifier SWAPPER_SWAP_SELECTED_EVENT = NoellesRolesCore.id("swapper_swap_selected");
    public static final Identifier SWAPPER_SWAP_EXECUTED_EVENT = NoellesRolesCore.id("swapper_swap_executed");
    public static final Identifier CORPSEMAKER_FORGED_BODY_EVENT = NoellesRolesCore.id("corpsemaker_forged_body");
    public static final Identifier VOODOO_BOUND_EVENT = NoellesRolesCore.id("voodoo_bound");
    public static final Identifier GUESSER_DECLARED_EVENT = NoellesRolesCore.id("guesser_declared");
    public static final Identifier GUESSER_CORRECT_EVENT = NoellesRolesCore.id("guesser_correct");
    public static final Identifier GUESSER_WRONG_EVENT = NoellesRolesCore.id("guesser_wrong");
    public static final Identifier CONTROLLER_POSSESS_STARTED_EVENT = NoellesRolesCore.id("controller_possess_started");
    public static final Identifier CONTROLLER_POSSESS_STOPPED_EARLY_EVENT = NoellesRolesCore.id("controller_possess_stopped_early");
    public static final Identifier CONTROLLER_POSSESS_ENDED_EVENT = NoellesRolesCore.id("controller_possess_ended");
    public static final Identifier ANGEL_SOOTHE_CAST_EVENT = NoellesRolesCore.id("angel_soothe_cast");
    public static final Identifier ANGEL_SOOTHED_EVENT = NoellesRolesCore.id("angel_soothed");
    public static final Identifier ANGEL_GUARD_SELECTED_EVENT = NoellesRolesCore.id("angel_guard_selected");
    public static final Identifier ANGEL_SACRIFICE_EVENT = NoellesRolesCore.id("angel_sacrifice");
    public static final Identifier ANGEL_GUARD_SHIELD_SOURCE = NoellesRolesCore.id("angel_guard");
    public static final Identifier SPIRITUALIST_PROJECTION_STARTED_EVENT = NoellesRolesCore.id("spiritualist_projection_started");
    public static final Identifier SPIRITUALIST_PROJECTION_ENDED_EVENT = NoellesRolesCore.id("spiritualist_projection_ended");
    public static final Identifier SPIRITUALIST_POSSESSION_STARTED_EVENT = NoellesRolesCore.id("spiritualist_possession_started");
    public static final Identifier SPIRITUALIST_POSSESSION_ENDED_EVENT = NoellesRolesCore.id("spiritualist_possession_ended");
    public static final Identifier OPERATOR_CONNECTION_FAILED_BOTH_DEAD_EVENT = NoellesRolesCore.id("operator_connection_failed_both_dead");
    public static final Identifier OPERATOR_CONNECTION_FAILED_ONE_DEAD_EVENT = NoellesRolesCore.id("operator_connection_failed_one_dead");
    public static final Identifier OPERATOR_CONNECTION_STARTED_EVENT = NoellesRolesCore.id("operator_connection_started");
    public static final Identifier OPERATOR_CONNECTION_ENDED_EVENT = NoellesRolesCore.id("operator_connection_ended");
    public static final Identifier OPERATOR_CONNECTION_INTERRUPTED_EVENT = NoellesRolesCore.id("operator_connection_interrupted");
    public static final Identifier OPERATOR_BROADCAST_FAILED_EVENT = NoellesRolesCore.id("operator_broadcast_failed");
    public static final Identifier OPERATOR_BROADCAST_STARTED_EVENT = NoellesRolesCore.id("operator_broadcast_started");
    public static final Identifier OPERATOR_BROADCAST_ENDED_EVENT = NoellesRolesCore.id("operator_broadcast_ended");
    public static final Identifier OPERATOR_BROADCAST_INTERRUPTED_EVENT = NoellesRolesCore.id("operator_broadcast_interrupted");
    /** 感知者成功选择存活玩家并开始 30 秒被动透视效果。 */
    public static final Identifier PERCEIVER_SELECTED_EFFECT_STARTED_EVENT = NoellesRolesCore.id("perceiver_selected_effect_started");
    /** 感知者的目标效果自然到时或因目标死亡提前消失。 */
    public static final Identifier PERCEIVER_SELECTED_EFFECT_ENDED_EVENT = NoellesRolesCore.id("perceiver_selected_effect_ended");
    /** 感知者尝试选择非存活玩家而失败。 */
    public static final Identifier PERCEIVER_SELECTION_FAILED_EVENT = NoellesRolesCore.id("perceiver_selection_failed");
    public static final Identifier REMEMBERER_RECALL_EVENT = NoellesRolesCore.id("rememberer_recall");
    public static final Identifier REMEMBERER_SNIPER_RELOADED_EVENT = NoellesRolesCore.id("rememberer_sniper_reloaded");
    public static final Identifier WAITER_SERVE_EVENT = NoellesRolesCore.id("waiter_serve");
    public static final Identifier WAITER_SELF_USE_EVENT = NoellesRolesCore.id("waiter_self_use");
    public static final Identifier COOK_FEED_EVENT = NoellesRolesCore.id("cook_feed");
    public static final Identifier DREAMER_COUNTS_EVENT = NoellesRolesCore.id("dreamer_counts");
    public static final Identifier DREAM_IMPRINT_SHIELD_SOURCE = NoellesRolesCore.id("dream_imprint");
    public static final Identifier HACKER_REVEAL_EVENT = NoellesRolesCore.id("hacker_reveal");
    public static final Identifier BELLRINGER_REDUCE_TIME_EVENT = NoellesRolesCore.id("bellringer_reduce_time");
    public static final Identifier DETECTIVE_CHECK_EVENT = NoellesRolesCore.id("detective_check");
    public static final Identifier MEDICAL_KIT_USE_EVENT = NoellesRolesCore.id("medical_kit");
    public static final Identifier PILL_SHIELD_SOURCE = NoellesRolesCore.id("pill");
    public static final Identifier PAN_SHIELD_SOURCE = NoellesRolesCore.id("pan");
    public static final Identifier PAN_STUN_END_EVENT = NoellesRolesCore.id("pan_stun_end");
    public static final Identifier STARSTRUCK_ABILITY_EVENT = NoellesRolesCore.id("starstruck_ability");
    public static final Identifier STARSTRUCK_ABILITY_END_EVENT = NoellesRolesCore.id("starstruck_ability_end");
    public static final Identifier TAPE_REMOVED_EVENT = NoellesRolesCore.id("tape_removed");
    public static final Identifier CLEANER_CLEAR_ITEMS_EVENT = NoellesRolesCore.id("cleaner_clear_items");
    public static final Identifier HUNTER_REFRESH_EVENT = NoellesRolesCore.id("hunter_refresh");
    public static final Identifier SULFURIC_ACID_BARREL_USE_EVENT = NoellesRolesCore.id("sulfuric_acid_barrel");
    public static final Identifier SPIRITUALIST_ACTIVE_SHIELD_SOURCE = NoellesRolesCore.id("spiritualist_active_shield");
    public static final Identifier SPIRITUALIST_LINGERING_SHIELD_SOURCE = NoellesRolesCore.id("spiritualist_lingering_shield");
    public static final Identifier MAGICIAN_RECORDING_STARTED_EVENT = NoellesRolesCore.id("magician_recording_started");
    public static final Identifier MAGICIAN_RECORDING_FINISHED_EVENT = NoellesRolesCore.id("magician_recording_finished");
    public static final Identifier MAGICIAN_RECORDING_STOPPED_EARLY_EVENT = NoellesRolesCore.id("magician_recording_stopped_early");
    public static final Identifier MAGICIAN_PLAYBACK_STARTED_EVENT = NoellesRolesCore.id("magician_playback_started");
    public static final Identifier MAGICIAN_PLAYBACK_FINISHED_EVENT = NoellesRolesCore.id("magician_playback_finished");
    public static final Identifier MAGICIAN_PLAYBACK_STOPPED_EARLY_EVENT = NoellesRolesCore.id("magician_playback_stopped_early");
    public static final Identifier MAGICIAN_PLAYBACK_FORCED_END_EVENT = NoellesRolesCore.id("magician_playback_forced_end");
    public static final Identifier AVARICIOUS_STOLE_COINS_EVENT = NoellesRolesCore.id("avaricious_stole_coins");
    public static final Identifier NECROMANCER_REVIVED_EVENT = NoellesRolesCore.id("necromancer_revived");
    public static final Identifier ROBOT_NIGHT_VISION_EVENT = NoellesRolesCore.id("robot_night_vision");
    public static final Identifier ROBOT_NIGHT_VISION_END_EVENT = NoellesRolesCore.id("robot_night_vision_end");
    public static final Identifier ROBOT_POISON_IMMUNE_EVENT = NoellesRolesCore.id("robot_poison_immune");
    public static final Identifier ROBOT_BED_POISON_IMMUNE_EVENT = NoellesRolesCore.id("robot_bed_poison_immune");
    public static final Identifier KIDNAPPER_RELEASE_EVENT = NoellesRolesCore.id("kidnapper_release");
    public static final Identifier THIEF_ATTEMPT_EVENT = NoellesRolesCore.id("thief_attempt");
    public static final Identifier THIEF_SUCCESS_EVENT = NoellesRolesCore.id("thief_success");
    public static final Identifier THIEF_FAIL_EVENT = NoellesRolesCore.id("thief_fail");
    public static final Identifier TIMEKEEPER_WATCH_USED_EVENT = NoellesRolesCore.id("timekeeper_watch_used");
    public static final Identifier TIMEKEEPER_WATCH_BROKEN_EVENT = NoellesRolesCore.id("timekeeper_watch_broken");
    public static final Identifier TIMEKEEPER_WATCH_REPAIRED_EVENT = NoellesRolesCore.id("timekeeper_watch_repaired");
    public static final Identifier TIMEKEEPER_WATCH_UPGRADED_EVENT = NoellesRolesCore.id("timekeeper_watch_upgraded");
    public static final Identifier SPRING_TRAP_ROOTED_EVENT = NoellesRolesCore.id("spring_trap_rooted");
    public static final Identifier SPRING_TRAP_UNROOTED_EVENT = NoellesRolesCore.id("spring_trap_unrooted");
    public static final Identifier SPRING_TRAP_SHIELD_SOURCE = NoellesRolesCore.id("spring_trap_shield");
    public static final Identifier JASON_WOUNDED_EVENT = NoellesRolesCore.id("jason_wounded");
    public static final Identifier JASON_RESCUED_EVENT = NoellesRolesCore.id("jason_rescued");
    public static final Identifier JASON_JERRY_CAN_IGNITED_EVENT = NoellesRolesCore.id("jason_jerry_can_ignited");
    public static final Identifier JASON_JERRY_CAN_AUTO_IGNITED_EVENT = NoellesRolesCore.id("jason_jerry_can_auto_ignited");
    public static final Identifier JASON_GASOLINE_DOUSED_EVENT = NoellesRolesCore.id("jason_gasoline_doused");
    public static final Identifier JASON_ABILITY_STARTED_EVENT = NoellesRolesCore.id("jason_ability_started");
    public static final Identifier JASON_ABILITY_EXIT_REQUESTED_EVENT = NoellesRolesCore.id("jason_ability_exit_requested");
    public static final Identifier JASON_ABILITY_EXIT_FINISHED_EVENT = NoellesRolesCore.id("jason_ability_exit_finished");
    public static final Identifier JASON_ABILITY_SCARED_EVENT = NoellesRolesCore.id("jason_ability_scared");
    public static final Identifier JASON_ABILITY_SCARE_ENDED_EVENT = NoellesRolesCore.id("jason_ability_scare_ended");
    public static final Identifier SHADOW_JESTER_STAGE_EVENT = NoellesRolesCore.id("shadow_jester_stage");
    /** 执照恶棍首次满足阵营全灭条件并进入时刻。暂停后恢复不会重复记录。 */
    public static final Identifier LICENSED_VILLAIN_MOMENT_STARTED_EVENT = NoellesRolesCore.id("licensed_villain_moment_started");
    /** 已进入过时刻的执照恶棍确认死亡，包含其正在为亡命徒让位的情况。 */
    public static final Identifier LICENSED_VILLAIN_MOMENT_ENDED_EVENT = NoellesRolesCore.id("licensed_villain_moment_ended");
    /** 巫妖骷髅命中玩家时的全局回放事件，用于显示“谁发射的哪类骷髅命中了谁”。 */
    public static final Identifier LICH_SKELETON_HIT_EVENT = NoellesRolesCore.id("lich_skeleton_hit");
    /** 巫妖成功释放魔法屏障后的全局回放事件，只在屏障实体成功生成后记录。 */
    public static final Identifier LICH_MAGIC_BARRIER_CAST_EVENT = NoellesRolesCore.id("lich_magic_barrier_cast");
    /** 巫妖魔法屏障到达最大飞行距离并自然消失时的全局回放事件。 */
    public static final Identifier LICH_MAGIC_BARRIER_DISAPPEAR_EVENT = NoellesRolesCore.id("lich_magic_barrier_disappear");
    /** 受屏障影响的玩家第一次进入屏障范围时的全局回放事件。 */
    public static final Identifier LICH_MAGIC_BARRIER_ENTER_EVENT = NoellesRolesCore.id("lich_magic_barrier_enter");
    /** 受屏障影响的玩家离开屏障范围或屏障自然消失导致范围闭合时的全局回放事件。 */
    public static final Identifier LICH_MAGIC_BARRIER_EXIT_EVENT = NoellesRolesCore.id("lich_magic_barrier_exit");
    /** 巫妖控门术能力的技能回放事件，用于记录锁门和修门数量。 */
    public static final Identifier LICH_DOOR_CONTROL_EVENT = NoellesRolesCore.id("lich_door_control");
    public static final Identifier VECNA_MARK_APPLIED_EVENT = NoellesRolesCore.id("vecna_mark_applied");
    public static final Identifier VECNA_MARK_ENDED_EVENT = NoellesRolesCore.id("vecna_mark_ended");
    /** 平民开始等待转化为亡命徒的全局回放事件。 */
    public static final Identifier COMMONER_REVIVAL_STARTED_EVENT = NoellesRolesCore.id("commoner_revival_started");
    /** 平民完成复活并进入亡命时刻的全局回放事件。 */
    public static final Identifier OUTLAW_REVIVED_EVENT = NoellesRolesCore.id("outlaw_revived");
    /** 亡命时刻结束（倒计时归零或掉线）的全局回放事件。 */
    public static final Identifier OUTLAW_TIME_ENDED_EVENT = NoellesRolesCore.id("outlaw_time_ended");
    /** 亡命徒通过击杀累计获得护盾层的全局回放事件。 */
    public static final Identifier OUTLAW_SHIELD_GAINED_EVENT = NoellesRolesCore.id("outlaw_shield_gained");
    /** 亡命护盾抵挡伤害时使用的稳定护盾来源 id。 */
    public static final Identifier OUTLAW_SHIELD_SOURCE = NoellesRolesCore.id("outlaw");
    /** 迈尔斯屠刀冲刺撞坏 Wathe 门的回放事件。 */
    public static final Identifier MYERS_DOOR_BROKEN_EVENT = NoellesRolesCore.id("myers_door_broken");
    /** 派对狂标记变声目标事件。 */
    public static final Identifier PARTY_ANIMAL_MARK_EVENT = NoellesRolesCore.id("party_animal_mark");
    /** 派对狂对自己释放变声事件。 */
    public static final Identifier PARTY_ANIMAL_SELF_PITCH_EVENT = NoellesRolesCore.id("party_animal_self_pitch");
    /** 派对狂对目标释放变声事件。 */
    public static final Identifier PARTY_ANIMAL_TARGET_PITCH_EVENT = NoellesRolesCore.id("party_animal_target_pitch");

    private NoellesEventIds() {
    }
}
