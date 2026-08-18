package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.api.death.DeathContext;
import dev.doctor4t.wathe.api.death.DeathDecision;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.UUID;

/**
 * 影子小丑死亡流程接入。
 *
 * <p>第二阶段的匕首是“选择工具”，不是普通杀人工具：
 * 只能杀死另一半。若尝试杀死其他人，本次受害者死亡会被取消，影子小丑自己因愧疚反噬死亡。</p>
 */
public final class ShadowJesterDeathHandler {
    private static boolean initialized = false;

    private ShadowJesterDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerFatalInterceptor(
                NoellesRolesCore.id("shadow_jester_illegal_phase_two_kill"),
                DeathApi.PRIORITY_FATAL_INTERCEPT + 50,
                ShadowJesterDeathHandler::interceptIllegalPhaseTwoKill
        );
        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("shadow_jester_after_death"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH,
                ShadowJesterDeathHandler::afterDeath
        );
    }

    private static DeathDecision interceptIllegalPhaseTwoKill(DeathContext context) {
        ServerPlayerEntity killer = context.serverKiller();
        ServerPlayerEntity victim = context.serverVictim();
        if (killer == null || victim == null) {
            return DeathDecision.PASS;
        }
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(killer.getServerWorld());
        if (!component.contains(killer.getUuid())
                || component.getPhase(killer.getUuid()) != ShadowJesterPhase.CHOICE
                || !isKnifeChoiceDeath(context)
                || component.arePartners(killer.getUuid(), victim.getUuid())) {
            return DeathDecision.PASS;
        }

        if (GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(context.deathReason())) {
            /*
             * 非法把别人打下车时，目标死亡会被取消。
             * 但落轨伤害可能每 tick 重复触发，所以先把目标传回攻击者当前位置，避免取消后又立刻再次掉出边界死亡。
             */
            victim.teleport(killer.getServerWorld(), killer.getX(), killer.getY(), killer.getZ(), victim.getYaw(), victim.getPitch());
            victim.setVelocity(0.0D, 0.0D, 0.0D);
        }

        NbtCompound extra = new NbtCompound();
        extra.putUuid("shadow_jester_illegal_target", victim.getUuid());
        GameFunctions.killPlayer(killer, true, null, NoellesDeathReasons.MODDED_BACKFIRE_DEATH_REASON, extra);
        return DeathDecision.CANCEL;
    }

    private static void afterDeath(DeathContext context) {
        if (!context.confirmedDeath()) {
            return;
        }

        ServerPlayerEntity killer = context.serverKiller();
        ServerPlayerEntity victim = context.serverVictim();
        if (victim == null) {
            return;
        }

        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(victim.getServerWorld());
        if (!component.hasPair() || !component.contains(victim.getUuid())) {
            return;
        }

        if (killer != null
                && component.arePartners(killer.getUuid(), victim.getUuid())
                && component.getPhase(killer.getUuid()) == ShadowJesterPhase.CHOICE
                && isKnifeChoiceDeath(context)) {
            /*
             * 选择杀死另一半：存活的小丑立刻转成狂信者，没收选择阶段匕首，并拆掉影子小丑配对。
             * 清任务参数保持 true，兼容“刚完成任务进第二阶段前后”的边界状态。
             */
            ShadowJesterManager.transformToJester(killer, true);
            component.removePairKeepPendingDeaths();
            return;
        }

        ShadowJesterPhase victimPhase = component.getPhase(victim.getUuid());
        if (!victimPhase.atLeast(ShadowJesterPhase.VOW_BOUND)) {
            handleEarlyExplicitDeath(victim, component);
            return;
        }

        /*
         * 从缔结誓言开始，第四阶段入口和谢幕音乐只关心“是否真的死过”，
         * 不能再用 creative / spectator 这类调试状态来猜测。
         * 因此只有 DeathApi 确认过的死亡才写入 confirmedDead 标记。
         */
        component.setConfirmedDead(victim.getUuid(), true);

        UUID partnerUuid = component.getPartner(victim.getUuid());
        ServerPlayerEntity partner = partnerUuid == null ? null : victim.getServer().getPlayerManager().getPlayer(partnerUuid);
        if (partner != null && GameFunctions.isPlayerAliveAndSurvival(partner)) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("broken_heart_partner", victim.getUuid());
            GameFunctions.killPlayer(partner, true, null, NoellesDeathReasons.BROKEN_HEART_DEATH_REASON, extra);
        } else if (partnerUuid != null) {
            component.markPendingOfflineDeath(partnerUuid, NoellesDeathReasons.BROKEN_HEART_DEATH_REASON);
        }
    }

    private static void handleEarlyExplicitDeath(ServerPlayerEntity victim, ShadowJesterComponent component) {
        UUID partnerUuid = component.getPartner(victim.getUuid());
        ServerPlayerEntity partner = partnerUuid == null ? null : victim.getServer().getPlayerManager().getPlayer(partnerUuid);
        if (partner != null && GameFunctions.isPlayerAliveAndSurvival(partner)) {
            /*
             * 未缔结誓言前的“明确死因”仍然要让另一半转成狂信者。
             * 这条路径不受调试开关影响；开关只关闭离线/创造旁观等没有死亡流程的 tick 扫描触发。
             */
            ShadowJesterManager.transformToJester(partner, true);
        }
        component.removePairKeepPendingDeaths();
    }

    private static boolean isKnifeChoiceDeath(DeathContext context) {
        return GameConstants.DeathReasons.KNIFE.equals(context.deathReason())
                || GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(context.deathReason());
    }
}
