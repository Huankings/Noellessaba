package org.agmas.noellesroles.mixin.roles.magician;

import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 录制“松开使用键”动作。
 *
 * <p>像飞斧、手雷、风之印记、水晶球这类带蓄力释放的物品，
 * 都依赖这条语义动作来在回放时正确触发 {@code onStoppedUsing}。
 *
 * <p>同时也在这里录制纯视觉挥手包。
 * 空挥/攻击键本身不一定会进入 attack 或 use 逻辑，但播放体仍然需要还原手臂动作。</p>
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class MagicianRecordPlayerActionMixin {

    @Shadow @Final public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction", at = @At("RETURN"))
    private void noellesroles$recordMagicianReleaseUse(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
            MagicianServerHooks.recordReleaseUse(this.player);
        }
    }

    @Inject(method = "onHandSwing", at = @At("RETURN"))
    private void noellesroles$recordMagicianHandSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
        MagicianServerHooks.recordSwing(this.player, packet.getHand());
    }
}
