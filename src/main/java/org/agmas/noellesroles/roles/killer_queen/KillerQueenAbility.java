package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 杀手皇后 G 键的杀手洗牌 / 标记招募双分支能力。 */
public final class KillerQueenAbility {
    private KillerQueenAbility() {
    }

    public static void handle(ServerPlayerEntity queen, int targetId) {
        if (!GameFunctions.isPlayerAliveAndSurvival(queen) || !KillerQueenRules.isKillerQueen(queen)
                || AbilityPlayerComponent.KEY.get(queen).cooldown > 0
                || !(queen.getServerWorld().getEntityById(targetId) instanceof ServerPlayerEntity target)
                || !GameFunctions.isPlayerAliveAndSurvival(target)
                || queen.squaredDistanceTo(target) > KillerQueenConstants.ABILITY_RANGE_SQUARED) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(queen.getServerWorld());
        Role targetRole = gameWorld.getRole(target);
        if (targetRole == null) return;
        if (targetRole.getFaction() == Faction.KILLER) {
            shuffleKiller(queen, target, targetRole);
        } else if (KillerQueenRules.isGoodOrIndependent(targetRole)) {
            recruit(queen, target, targetRole);
        }
    }

    private static void shuffleKiller(ServerPlayerEntity queen, ServerPlayerEntity target, Role oldRole) {
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(queen);
        if (shop.balance < KillerQueenConstants.SHUFFLE_COST_COINS) return;
        List<Role> candidates = new ArrayList<>(dev.doctor4t.wathe.api.WatheRoles.ROLES);
        candidates.removeIf(role -> role == oldRole || role.getFaction() != Faction.KILLER || KillerQueenManager.isDisabled(role));
        if (candidates.isEmpty()) return;
        Collections.shuffle(candidates);
        Role next = candidates.getFirst();
        shop.addToBalance(-KillerQueenConstants.SHUFFLE_COST_COINS);
        AbilityPlayerComponent.KEY.get(queen).setCooldown(KillerQueenConstants.SHUFFLE_COOLDOWN_TICKS);
        KillerQueenManager.transformToKiller(target, next);
        queen.sendMessage(Text.translatable("message.noellesroles.killer_queen.shuffle_self", target.getDisplayName())
                .withColor(KillerQueenConstants.ROLE_COLOR), true);
        target.sendMessage(Text.translatable("message.noellesroles.killer_queen.shuffle_target", queen.getDisplayName())
                .withColor(KillerQueenConstants.ROLE_COLOR), true);
        KillerQueenReplayHelper.record(queen, NoellesEventIds.KILLER_QUEEN_SHUFFLE_EVENT,
                queen, target.getUuid(), null);
    }

    private static void recruit(ServerPlayerEntity queen, ServerPlayerEntity target, Role targetRole) {
        KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(queen.getServerWorld());
        boolean mimic = targetRole == NoellesRoleRegistry.MIMIC;
        if (!mimic && !state.isMarkedBy(queen.getUuid(), target.getUuid())) return;
        if (state.getAvailableRecruitments() <= 0) return;
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(queen);
        if (!mimic && shop.balance < KillerQueenConstants.RECRUIT_COST_COINS) return;
        ServerPlayerEntity assassinationTarget = KillerQueenManager.selectRecruitmentTarget(queen.getServerWorld(), target);
        if (assassinationTarget == null || !state.consumeRecruitmentOpportunity()) return;
        if (!mimic) shop.addToBalance(-KillerQueenConstants.RECRUIT_COST_COINS);
        AbilityPlayerComponent.KEY.get(queen).setCooldown(KillerQueenConstants.RECRUIT_COOLDOWN_TICKS);
        KillerQueenManager.dropAllRevolvers(target);
        target.giveItemStack(WatheItems.KNIFE.getDefaultStack());
        state.recruit(target.getUuid(), queen.getUuid(), assassinationTarget.getUuid());
        queen.sendMessage(Text.translatable("message.noellesroles.killer_queen.recruit_self", target.getDisplayName())
                .withColor(KillerQueenConstants.ROLE_COLOR), true);
        target.sendMessage(Text.translatable("message.noellesroles.killer_queen.recruit_target",
                queen.getDisplayName(), assassinationTarget.getDisplayName()).withColor(KillerQueenConstants.ROLE_COLOR), true);
        KillerQueenReplayHelper.record(queen, NoellesEventIds.KILLER_QUEEN_RECRUIT_EVENT,
                queen, target.getUuid(), null);
        KillerQueenReplayHelper.record(queen, NoellesEventIds.KILLER_QUEEN_RECRUIT_TARGET_EVENT,
                target, assassinationTarget.getUuid(), null);
    }
}
