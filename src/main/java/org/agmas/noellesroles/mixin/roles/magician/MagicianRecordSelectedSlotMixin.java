package org.agmas.noellesroles.mixin.roles.magician;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
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
 * 录制快捷栏切槽。
 *
 * <p>这里选择挂在 {@link ServerPlayNetworkHandler#onUpdateSelectedSlot(UpdateSelectedSlotC2SPacket)} 的返回点，
 * 原因是：
 * 1. 到这里说明服务端已经接受了这次切槽；
 * 2. 可以天然避开 wathe 疯魔等逻辑里对非法切槽的提前拦截；
 * 3. 录制结果会和服务端真实认可的选槽完全一致。
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class MagicianRecordSelectedSlotMixin {

    @Shadow @Final public ServerPlayerEntity player;

    @Inject(method = "onUpdateSelectedSlot", at = @At("RETURN"))
    private void noellesroles$recordMagicianSelectedSlot(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        MagicianServerHooks.recordSelectSlot(this.player, packet.getSelectedSlot());
    }
}
