package org.agmas.noellesroles.mixin.roles.robot;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerPoisonComponent.class)
public abstract class RobotNoPoisonMixin {
    @Shadow @Final private PlayerEntity player;

    @Inject(method = "setDetailedPoisonTicks", at = @At("HEAD"), cancellable = true)
    private void noellesroles$cancelRobotPoison(int ticks, @Nullable UUID poisoner, Identifier source, @Nullable NbtCompound extra, @NotNull CallbackInfo ci) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        boolean realPoison = GameConstants.DeathReasons.POISON.equals(source) || GameConstants.DeathReasons.BED_POISON.equals(source);
        if (!gameWorld.isRole(this.player, NoellesRoleRegistry.ROBOT) || !realPoison) {
            return;
        }

        if (this.player instanceof ServerPlayerEntity serverPlayer && GameConstants.DeathReasons.POISON.equals(source)) {
            /*
             * 普通带毒物品命中机器人时，保留一条免疫回放。
             * extra 里可能已有 item / item_name，复制后再补 poisoner，避免污染调用方原始 NBT。
             */
            NbtCompound replayData = extra == null ? new NbtCompound() : extra.copy();
            if (poisoner != null) {
                replayData.putUuid("poisoner", poisoner);
            }
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), NoellesEventIds.ROBOT_POISON_IMMUNE_EVENT, serverPlayer, replayData);
        }
        ci.cancel();
    }
}
