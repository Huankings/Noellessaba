package org.agmas.noellesroles.mixin.roles.spiritualist;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 阻止被附身者自己的移动包把灵术师的控制结果顶回去。
 *
 * <p>当前附身设计不是“把宿主客户端真的变成灵术师客户端”，
 * 而是“服务端代替灵术师驱动宿主”。
 * 因此宿主本人客户端继续上送的位置包，在附身期间只能视为噪音，
 * 否则服务器刚推动一步，下一帧又会被宿主自己的旧位置同步拖回去。</p>
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class SpiritualistPossessedMovementBlockMixin {

    @Shadow public ServerPlayerEntity player;

    @Shadow public abstract void syncWithPlayerPosition();

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockPossessedMovementPackets(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (SpiritualistHostComponent.KEY.get(this.player).possessed
                || SpiritualistPlayerComponent.KEY.get(this.player).hasDetachedBodyState()) {
            /*
             * 同步一下服务端“上一次合法位置”的记录，
             * 避免后续校验仍然拿旧坐标做基准。
             */
            this.syncWithPlayerPosition();
            ci.cancel();
        }
    }
}
