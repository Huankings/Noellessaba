package org.agmas.noellesroles.mixin.roles.rememberer;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.rememberer.RemembererPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在服务端收到快捷栏切槽包的同一时刻，立刻建立狙击枪部署冷却。
 *
 * <p>原先部署冷却只在 {@link RemembererPlayerComponent#serverTick()} 里轮询选槽，
 * 因此会留下一个非常短但真实存在的空窗：
 * 玩家切到狙击枪后，如果在服务端下一次 tick 到来之前立刻开火，
 * 就可能抢在“部署冷却真正建立”前完成瞬发。
 *
 * <p>这个 mixin 把时机前移到原版处理切槽包之后的第一时间，
 * 让服务端一旦确认“你现在选中的格子就是狙击枪”，就马上写入部署冷却。</p>
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class SniperRifleSelectedSlotCooldownMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onUpdateSelectedSlot", at = @At("TAIL"))
    private void noellesroles$applySniperDeployCooldownImmediately(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        RemembererPlayerComponent.KEY.get(this.player).syncSniperSelectionStateNow(packet.getSelectedSlot());
    }
}
