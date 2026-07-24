package org.agmas.noellesroles.mixin.roles.robot;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.bed.BedEffectRegistry;
import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(BedEffectRegistry.class)
public abstract class RobotBedPoisonMixin {
    @Inject(method = "triggerBedEffect", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$cancelRobotBedPoison(ServerPlayerEntity player, CallbackInfo ci) {
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.ROBOT)) {
            return;
        }

        TrimmedBedBlockEntity bed = BedEffectRegistry.findTriggeredBedEffect(player.getWorld(), player.getBlockPos());
        if (bed == null || !bed.hasScorpion()) {
            return;
        }

        /*
         * 当前 Wathe 的床效果入口已经是 BedEffectRegistry.triggerBedEffect。
         * 在这里提前清掉蝎子并取消后续触发，机器人就不会收到中毒 overlay，也不会进入毒组件。
         */
        UUID poisoner = bed.getPoisoner();
        bed.setHasScorpion(false, null);
        if (poisoner != null) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("poisoner", poisoner);
            GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.ROBOT_BED_POISON_IMMUNE_EVENT, player, extra);
        }
        ci.cancel();
    }
}
