package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 灵术师特殊相机断线清理。
 *
 * <p>攻击、使用、选取方块、挖掘和热键栏限制已经迁移到 Wathe ClientInputApi，
 * 此处只保留 MinecraftClient disconnect 生命周期，避免为了无关生命周期再新增输入 mixin。</p>
 */
@Mixin(MinecraftClient.class)
public abstract class SpiritualistMinecraftMixin {

    @Inject(method = "disconnect()V", at = @At("HEAD"))
    private void noellesroles$cleanupDetachedCameraOnDisconnect(CallbackInfo ci) {
        SpiritualistClientController.reset();
    }
}
