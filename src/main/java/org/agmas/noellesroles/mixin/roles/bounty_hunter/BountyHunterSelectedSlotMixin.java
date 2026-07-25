package org.agmas.noellesroles.mixin.roles.bounty_hunter;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 赏金模式期间的服务端选槽保护。
 *
 * <p>Wathe 原疯魔锁槽只允许切到球棒；赏金模式复用疯魔状态但武器不是球棒。
 * 因此这里额外拦截“切到任何非模式德林加格子”的包，避免玩家通过数字键切到球棒或其他物品。</p>
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class BountyHunterSelectedSlotMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void noellesroles$lockBountyModeSelectedSlot(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        BountyHunterPlayerComponent bountyHunter = BountyHunterPlayerComponent.KEY.get(this.player);
        if (!bountyHunter.isBountyModeActive()) {
            return;
        }
        int selectedSlot = packet.getSelectedSlot();
        if (selectedSlot < 0
                || selectedSlot >= this.player.getInventory().size()
                || !BountyHunterPlayerComponent.isModeGrantedDerringer(this.player.getInventory().getStack(selectedSlot))) {
            ci.cancel();
        }
    }
}
