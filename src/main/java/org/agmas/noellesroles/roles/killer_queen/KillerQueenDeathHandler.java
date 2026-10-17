package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.api.death.DeathContext;
import dev.doctor4t.wathe.api.death.DeathDecision;
import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.harpymodloader.component.WorldModifierComponent;

import java.util.UUID;

/** 杀手皇后的标记法杖、招募限制、机会和奖励所需死亡链。 */
public final class KillerQueenDeathHandler {
    private KillerQueenDeathHandler() {
    }

    public static void init() {
        DeathApi.registerEarlyInterceptor(NoellesRolesCore.id("killer_queen/marker_staff"),
                DeathApi.PRIORITY_SPECIAL_SURVIVAL_PROTECTION + KillerQueenConstants.MARKER_STAFF_DEATH_PRIORITY_BONUS, KillerQueenDeathHandler::interceptMarkerStaff);
        DeathApi.registerEarlyInterceptor(NoellesRolesCore.id("killer_queen/psycho_kill_suppression"),
                DeathApi.PRIORITY_SPECIAL_SURVIVAL_PROTECTION + KillerQueenConstants.PSYCHO_KILL_SUPPRESSION_PRIORITY_BONUS, KillerQueenDeathHandler::suppressKillerDeathsDuringPsycho);
        DeathApi.registerEarlyInterceptor(NoellesRolesCore.id("killer_queen/recruit_target_only"),
                DeathApi.PRIORITY_SPECIAL_SURVIVAL_PROTECTION + KillerQueenConstants.RECRUIT_TARGET_ONLY_PRIORITY_BONUS, KillerQueenDeathHandler::restrictRecruitedKiller);
        DeathApi.registerFatalInterceptor(NoellesRolesCore.id("killer_queen/recruit_unbind"),
                DeathApi.PRIORITY_FATAL_INTERCEPT + KillerQueenConstants.RECRUIT_UNBIND_PRIORITY_BONUS, KillerQueenDeathHandler::clearFatalBindings);
        DeathApi.registerAfterAttempt(NoellesRolesCore.id("killer_queen/death_results"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH, KillerQueenDeathHandler::afterDeath);
    }

    private static DeathDecision interceptMarkerStaff(DeathContext context) {
        ServerPlayerEntity queen = context.serverKiller();
        ServerPlayerEntity victim = context.serverVictim();
        if (queen == null || victim == null || queen.getMainHandStack().getItem() != ModItems.MARKER_STAFF
                || !PsychoModeApi.isActive(queen, KillerQueenPsychoHandler.PROFILE_ID)
                || !GameConstants.DeathReasons.BAT.equals(context.deathReason())) {
            return DeathDecision.PASS;
        }

        StunnedPlayerComponent.KEY.get(victim).stun(KillerQueenConstants.MARKER_STAFF_ROOT_TICKS,
                NoellesEventIds.KILLER_QUEEN_ROOT_END_EVENT);
        NbtCompound rootExtra = new NbtCompound();
        rootExtra.putUuid("target", victim.getUuid());
        rootExtra.putUuid("queen", queen.getUuid());
        rootExtra.putString("item_name", queen.getMainHandStack().getName().getString());
        GameRecordManager.recordGlobalEvent(queen.getServerWorld(), NoellesEventIds.KILLER_QUEEN_ROOT_START_EVENT, queen, rootExtra);

        KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(queen.getServerWorld());
        if (KillerQueenRules.isLivingGoodOrIndependent(victim)) {
            KillerQueenWorldComponent.PsychoMarkerResult markResult =
                    state.addPsychoMarker(queen.getUuid(), victim.getUuid());
            if (!markResult.added()) return DeathDecision.CANCEL;

            PsychoModeApi.addRemainingTicks(queen, KillerQueenPsychoHandler.PROFILE_ID,
                    KillerQueenConstants.MARKER_STAFF_PSYCHO_BONUS_TICKS);
            KillerQueenReplayHelper.record(queen, NoellesEventIds.KILLER_QUEEN_PSYCHO_MARK_EVENT,
                    queen, victim.getUuid(), null);
            /* 每两次首次标记合法目标增加一层护盾；重复击打已标记者只刷新定身，不累计进度。 */
            if (markResult.grantsShield() && PsychoModeApi.addArmourLayers(
                    queen,
                    KillerQueenPsychoHandler.PROFILE_ID,
                    KillerQueenConstants.MARKER_STAFF_SHIELD_REWARD_LAYERS)) {
                KillerQueenReplayHelper.record(queen, NoellesEventIds.KILLER_QUEEN_PSYCHO_SHIELD_GAIN_EVENT,
                        queen, null, null);
            }
        }
        return DeathDecision.CANCEL;
    }

    private static DeathDecision suppressKillerDeathsDuringPsycho(DeathContext context) {
        ServerPlayerEntity killer = context.serverKiller();
        if (killer == null || !KillerQueenManager.hasActivePsycho(killer.getServerWorld())) return DeathDecision.PASS;
        if (killer.getUuid().equals(context.victim().getUuid())) return DeathDecision.PASS;
        var role = GameWorldComponent.KEY.get(killer.getServerWorld()).getRole(killer);
        if (!KillerQueenRules.isKillerOrKillerSidedNeutral(role)) return DeathDecision.PASS;
        if (GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(context.deathReason())
                && context.serverVictim() != null) {
            context.serverVictim().teleport(killer.getServerWorld(), killer.getX(), killer.getY(), killer.getZ(),
                    context.serverVictim().getYaw(), context.serverVictim().getPitch());
            context.serverVictim().setVelocity(0.0D, 0.0D, 0.0D);
        }
        return DeathDecision.CANCEL;
    }

    private static DeathDecision restrictRecruitedKiller(DeathContext context) {
        ServerPlayerEntity killer = context.serverKiller();
        if (killer == null) return DeathDecision.PASS;
        if (killer.getUuid().equals(context.victim().getUuid())) return DeathDecision.PASS;
        KillerQueenWorldComponent.Recruitment recruitment = KillerQueenWorldComponent.KEY.get(killer.getServerWorld())
                .getRecruitment(killer.getUuid());
        if (recruitment == null || recruitment.target().equals(context.victim().getUuid())) return DeathDecision.PASS;
        if (GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(context.deathReason())
                && context.serverVictim() != null) {
            /* 只取消落轨死亡会让目标留在车外并每 tick 重复触发，因此同时拉回攻击者位置。 */
            context.serverVictim().teleport(killer.getServerWorld(), killer.getX(), killer.getY(), killer.getZ(),
                    context.serverVictim().getYaw(), context.serverVictim().getPitch());
            context.serverVictim().setVelocity(0.0D, 0.0D, 0.0D);
        }
        return DeathDecision.CANCEL;
    }

    private static DeathDecision clearFatalBindings(DeathContext context) {
        ServerPlayerEntity killer = context.serverKiller();
        ServerPlayerEntity victim = context.serverVictim();
        if (killer == null || victim == null) return DeathDecision.PASS;
        KillerQueenWorldComponent.Recruitment recruitment = KillerQueenWorldComponent.KEY.get(killer.getServerWorld())
                .getRecruitment(killer.getUuid());
        if (recruitment == null || !recruitment.target().equals(victim.getUuid())) return DeathDecision.PASS;

        ShadowJesterComponent shadow = ShadowJesterComponent.KEY.get(killer.getServerWorld());
        if (shadow.arePartners(killer.getUuid(), victim.getUuid())) shadow.removePairKeepPendingDeaths();

        WorldModifierComponent modifiers = WorldModifierComponent.KEY.get(killer.getServerWorld());
        LoversPairComponent lovers = LoversPairComponent.KEY.get(killer.getServerWorld());
        UUID loverPartner = lovers.getPartnerOrFallback(
                killer.getUuid(), modifiers.getAllWithModifier(NoellesModifierRegistry.LOVERS));
        if (victim.getUuid().equals(loverPartner)) {
            modifiers.getModifiers(killer).removeIf(NoellesModifierRegistry.LOVERS::equals);
            modifiers.getModifiers(victim).removeIf(NoellesModifierRegistry.LOVERS::equals);
            lovers.removePair(killer.getUuid());
            modifiers.sync();
        }
        return DeathDecision.PASS;
    }

    private static void afterDeath(DeathContext context) {
        if (!context.confirmedDeath() || context.serverVictim() == null) return;
        ServerPlayerEntity victim = context.serverVictim();
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getServerWorld());
        KillerQueenWorldComponent queenWorld = KillerQueenWorldComponent.KEY.get(victim.getServerWorld());

        /*
         * 招募会在发动时消耗一份世界共享机会，但被招募者在完成指定击杀前仍保留原职业，
         * 所以其死亡不会被下面的“杀手阵营死亡”分支识别。这里在 Wathe 确认真实死亡后
         * 原子地移除招募关系并返还机会；关系只可能成功移除一次，因此重复死亡不会重复返还。
         *
         * 死灵法师拥有独立的世界复活计数，杀手皇后的消费从未扣除该计数，所以这里不能再给
         * 死灵法师额外加一，否则同一次原始杀手死亡会平白产生第二份复活机会。
         */
        queenWorld.clearRecruitmentAndRefundOpportunity(victim.getUuid());
        if (gameWorld.getRole(victim) != null && gameWorld.getRole(victim).getFaction() == Faction.KILLER) {
            queenWorld.addRecruitmentOpportunity();
        }

        ServerPlayerEntity killer = context.serverKiller();
        if (killer != null && gameWorld.getRole(killer) != null && gameWorld.getRole(killer).getFaction() == Faction.KILLER) {
            for (UUID queenUuid : gameWorld.getAllWithRole(NoellesRoleRegistry.KILLER_QUEEN)) {
                ServerPlayerEntity queen = victim.getServer().getPlayerManager().getPlayer(queenUuid);
                if (queen != null && GameFunctions.isPlayerAliveAndSurvival(queen)
                        && !TimekeeperPlayerComponent.KEY.get(queen).isInTimeRift()) {
                    PlayerShopComponent.KEY.get(queen).addToBalance(KillerQueenConstants.KILLER_KILL_REWARD_COINS);
                }
            }
        }

        if (killer != null) {
            KillerQueenWorldComponent.Recruitment recruitment = queenWorld.getRecruitment(killer.getUuid());
            if (recruitment != null && recruitment.target().equals(victim.getUuid())) {
                KillerQueenManager.completeRecruitment(killer, recruitment);
            }
        }
    }
}
