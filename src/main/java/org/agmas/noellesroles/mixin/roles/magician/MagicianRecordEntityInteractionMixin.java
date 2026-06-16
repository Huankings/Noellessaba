package org.agmas.noellesroles.mixin.roles.magician;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 录制“对实体右键”的交互。
 *
 * <p>像风之印记、定时炸弹这类“对准玩家右键”的道具，
 * 不一定会走进 {@link net.minecraft.server.network.ServerPlayerInteractionManager#interactItem}，
 * 因此这里要额外从实体交互包入口补录一次。
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class MagicianRecordEntityInteractionMixin {

    @Shadow @Final public ServerPlayerEntity player;

    @Inject(method = "onPlayerInteractEntity", at = @At("RETURN"))
    private void noellesroles$recordMagicianInteractEntity(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
            @Override
            public void interact(Hand hand) {
                MagicianServerHooks.recordUse(MagicianRecordEntityInteractionMixin.this.player, hand);
            }

            @Override
            public void interactAt(Hand hand, net.minecraft.util.math.Vec3d pos) {
                MagicianServerHooks.recordUse(MagicianRecordEntityInteractionMixin.this.player, hand);
            }

            @Override
            public void attack() {
                // 普通左键攻击统一交给 PlayerEntity.attack 的录制钩子处理。
            }
        });
    }
}
