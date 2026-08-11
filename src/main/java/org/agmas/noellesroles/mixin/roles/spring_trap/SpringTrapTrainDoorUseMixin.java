package org.agmas.noellesroles.mixin.roles.spring_trap;

import dev.doctor4t.wathe.block.TrainDoorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.item.ColorfulAxeItem;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapDoorSoundContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 车厢门覆写了 SmallDoorBlock#onUse，所以需要单独记录交互玩家。
 */
@Mixin(TrainDoorBlock.class)
public abstract class SpringTrapTrainDoorUseMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$beginSpringTrapTrainDoorSoundContext(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        SpringTrapDoorSoundContext.begin(player);
        ActionResult pryResult = ColorfulAxeItem.tryPryWatheDoor(world, pos, player);
        if (pryResult != ActionResult.PASS) {
            SpringTrapDoorSoundContext.end();
            cir.setReturnValue(pryResult);
        }
    }

    @Inject(method = "onUse", at = @At("RETURN"))
    private void noellesroles$endSpringTrapTrainDoorSoundContext(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        SpringTrapDoorSoundContext.end();
    }
}
