package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.tray.TrayEffectHandler;
import dev.doctor4t.wathe.api.tray.TrayEffectRegistry;
import dev.doctor4t.wathe.api.tray.TrayEffectReplayInfo;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 酸液通过 Wathe 托盘公开 API 接入的拿取、消费和回放逻辑。 */
public final class KillerQueenTrayEffectHandler implements TrayEffectHandler {
    @Override
    public Identifier effectId() {
        return NoellesEventIds.KILLER_QUEEN_ACID_TRAY_EFFECT;
    }

    @Override
    public Item additiveItem() {
        return ModItems.ACID_SOLUTION;
    }

    @Override
    public TrayEffectReplayInfo replayDisplay() {
        return new TrayEffectReplayInfo("item.noellesroles.acid_solution", "Acid Solution");
    }

    @Override
    public void applyToTray(BeveragePlateBlockEntity plate, ServerPlayerEntity player, ItemStack heldStack, BlockPos pos) {
        /* 保持托盘默认互斥：不覆盖毒药，也不覆盖其它扩展试剂。 */
        TrayEffectRegistry.applyStandardEffect(player, heldStack, plate, pos, effectId(), false, false);
    }

    @Override
    public void onTakeFromTray(ServerPlayerEntity player, ItemStack takenStack, @Nullable UUID applierUuid, NbtCompound replayExtra) {
        if (applierUuid == null || !KillerQueenRules.isLivingGoodOrIndependent(player)) return;
        ServerPlayerEntity queen = player.getServer().getPlayerManager().getPlayer(applierUuid);
        if (queen == null || !KillerQueenRules.isKillerQueen(queen)) return;
        KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(player.getServerWorld());
        if (state.addMarker(queen.getUuid(), player.getUuid(), false)) {
            KillerQueenReplayHelper.record(player, NoellesEventIds.KILLER_QUEEN_ACID_MARK_EVENT,
                    player, player.getUuid(), queen.getUuid());
        }
    }

    @Override
    public void onConsume(ServerPlayerEntity player, ItemStack consumedSnapshot, String consumeType, @Nullable UUID applierUuid) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getServerWorld());
        boolean speedRecipient = gameWorld.isRole(player, NoellesRoleRegistry.KILLER_QUEEN)
                || gameWorld.isRole(player, NoellesRoleRegistry.ROBOT);
        if (speedRecipient) {
            KillerQueenPlayerComponent.KEY.get(player).startSpeedBonus();
            KillerQueenReplayHelper.record(player, NoellesEventIds.KILLER_QUEEN_SPEED_START_EVENT, player, null, null);
        } else {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,
                    KillerQueenConstants.ACID_DAZE_DURATION_TICKS, KillerQueenConstants.ACID_NAUSEA_AMPLIFIER,
                    false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS,
                    KillerQueenConstants.ACID_DAZE_DURATION_TICKS, KillerQueenConstants.ACID_BLINDNESS_AMPLIFIER,
                    false, false, true));
            KillerQueenPlayerComponent.KEY.get(player).startDaze();
            KillerQueenReplayHelper.record(player, NoellesEventIds.KILLER_QUEEN_DAZE_START_EVENT, player, null, null);
        }

        /* 合法阵营消费者本人在实际吃/喝时获得奖励；拿取托盘物品本身不发放消费奖励。 */
        if (!KillerQueenRules.isGoodOrIndependent(gameWorld.getRole(player))) return;
        PlayerShopComponent.KEY.get(player).addToBalance(KillerQueenConstants.ACID_CONSUMER_REWARD_COINS);

        if (applierUuid == null) return;
        ServerPlayerEntity queen = player.getServer().getPlayerManager().getPlayer(applierUuid);
        if (queen != null && KillerQueenRules.isKillerQueen(queen)
                && GameFunctions.isPlayerAliveAndSurvival(queen)
                && !org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent.KEY.get(queen).isInTimeRift()) {
            PlayerShopComponent.KEY.get(queen).addToBalance(KillerQueenConstants.ACID_CONSUME_REWARD_COINS);
        }
    }
}
