package org.agmas.noellesroles.mixin.roles.spring_trap;

import dev.doctor4t.wathe.block.SmallDoorBlock;
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
 * 普通 Wathe 小门交互时记录开门者。
 *
 * <p>记录只存在于本次 onUse 调用栈，门音效播放结束后立即清掉，不会影响自动关门或其他玩家开门。</p>
 */
@Mixin(SmallDoorBlock.class)
public abstract class SpringTrapSmallDoorUseMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$beginSpringTrapDoorSoundContext(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        SpringTrapDoorSoundContext.begin(player);
        ActionResult pryResult = ColorfulAxeItem.tryPryWatheDoor(world, pos, player);
        if (pryResult != ActionResult.PASS) {
            SpringTrapDoorSoundContext.end();
            cir.setReturnValue(pryResult);
        }
    }

    @Inject(method = "onUse", at = @At("RETURN"))
    private void noellesroles$endSpringTrapDoorSoundContext(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        SpringTrapDoorSoundContext.end();
    }
}
