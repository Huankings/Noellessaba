package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让原版 HUD 始终读取灵术师本人那份数据。
 *
 * <p>无论当前相机是灵魂相机还是附身宿主，
 * 血量、经验、饥饿值都应该继续显示“灵术师客户端当前同步到的那份状态”。</p>
 */
@Mixin(InGameHud.class)
public abstract class SpiritualistGuiMixin {

    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
    private void noellesroles$useLocalPlayerForHud(CallbackInfoReturnable<PlayerEntity> cir) {
        if (SpiritualistClientController.isProjectionActive() || SpiritualistClientController.isPossessionViewActive()) {
            cir.setReturnValue(MinecraftClient.getInstance().player);
        }
    }
}
