package org.agmas.noellesroles.mixin.roles.spiritualist;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
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
 * 附身期间拦截宿主本人客户端送来的冲刺/潜行等控制命令。
 *
 * <p>之前我们只拦了位置移动包，没有拦这一类“状态命令包”，
 * 于是宿主或灵术师本体客户端偶尔仍可能把服务端正在维持的姿态顶回去，
 * 表现出来就是：
 * 1. 蹲下右键时蹲起抖动；
 * 2. 冲刺 / 潜行状态偶尔被莫名改回；
 * 3. 本体脱体期间仍偷偷改到自己的服务端姿态。
 *
 * <p>灵术师附身设计里，这两名玩家在这段时间都不应该再用自己的原生客户端命令
 * 直接修改服务端状态，因此这里统一挡掉。</p>
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class SpiritualistPossessedClientCommandBlockMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onClientCommand", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockDetachedClientCommands(ClientCommandC2SPacket packet, CallbackInfo ci) {
        if (SpiritualistHostComponent.KEY.get(this.player).possessed
                || SpiritualistPlayerComponent.KEY.get(this.player).hasDetachedBodyState()) {
            ci.cancel();
        }
    }
}
