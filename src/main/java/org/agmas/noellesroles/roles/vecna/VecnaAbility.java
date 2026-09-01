package org.agmas.noellesroles.roles.vecna;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 维克那 G 键颠倒标记能力。 */
public final class VecnaAbility {
    private VecnaAbility() {}

    public static void handle(ServerPlayerEntity player, int clientTargetId) {
        GameWorldComponent world = GameWorldComponent.KEY.get(player.getWorld());
        if (!world.isRunning() || !world.isRole(player, NoellesRoleRegistry.VECNA)
                || !GameFunctions.isPlayerAliveAndSurvival(player)) return;
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) return;
        PlayerEntity target = resolveTarget(player, clientTargetId);
        if (target == null) target = player;
        VecnaPlayerComponent.KEY.get(target).applyMark(player);
        ability.setCooldown(VecnaConstants.ABILITY_COOLDOWN_TICKS);
        net.minecraft.text.MutableText message = target == player
                ? Text.translatable("message.noellesroles.vecna.self_mark")
                : Text.translatable("message.noellesroles.vecna.marked", target.getDisplayName());
        player.sendMessage(message.withColor(VecnaConstants.ROLE_COLOR), true);
        NbtCompound extra = new NbtCompound();
        extra.putUuid("target_player", target.getUuid());
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.VECNA_MARK_APPLIED_EVENT, player, extra);
    }

    private static PlayerEntity resolveTarget(ServerPlayerEntity player, int clientTargetId) {
        if (clientTargetId >= 0 && player.getServerWorld().getEntityById(clientTargetId) instanceof PlayerEntity target
                && target != player && GameFunctions.isPlayerAliveAndSurvival(target)
                && target.squaredDistanceTo(player) <= VecnaConstants.MARK_RANGE_BLOCKS * VecnaConstants.MARK_RANGE_BLOCKS) {
            return target;
        }
        EntityHitResult hit = WeaponTargetingApi.getVisibleAlivePlayerTarget(player, VecnaConstants.MARK_RANGE_BLOCKS);
        return hit != null && hit.getEntity() instanceof PlayerEntity target && target != player ? target : null;
    }
}
