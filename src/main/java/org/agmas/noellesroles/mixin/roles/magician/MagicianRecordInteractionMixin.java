package org.agmas.noellesroles.mixin.roles.magician;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 录制服务端真正接受的右键动作。
 *
 * <p>这里直接挂在 {@link ServerPlayerInteractionManager} 上，而不是包接收器：
 * 1. 这样可以只记录“服务端确实允许执行”的交互；
 * 2. 能同时覆盖普通物品使用、对方块使用两条主链；
 * 3. 不会因为客户端重复发包或预测行为，把无效右键也录进去。
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class MagicianRecordInteractionMixin {

    @Shadow @Final protected ServerPlayerEntity player;

    @Inject(method = "interactItem", at = @At("RETURN"))
    private void noellesroles$recordMagicianInteractItem(
            ServerPlayerEntity player,
            World world,
            ItemStack stack,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (cir.getReturnValue().isAccepted()) {
            MagicianServerHooks.recordUse(this.player, hand, stack);
        }
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void noellesroles$recordMagicianInteractBlock(
            ServerPlayerEntity player,
            World world,
            ItemStack stack,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (cir.getReturnValue().isAccepted()) {
            MagicianServerHooks.recordUseBlock(this.player, hand, stack, hitResult);
        }
    }
}
