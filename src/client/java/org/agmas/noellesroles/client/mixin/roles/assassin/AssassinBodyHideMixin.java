package org.agmas.noellesroles.client.mixin.roles.assassin;

import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.agmas.noellesroles.roles.assassin.AssassinVisibility;
import org.agmas.noellesroles.roles.assassin.HiddenBodiesWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 按观察者身份隐藏刺客击杀后留下的尸体。
 */
@Mixin(PlayerBodyEntityRenderer.class)
public abstract class AssassinBodyHideMixin {

    @Inject(
            method = "render(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$hideAssassinBodies(
            PlayerBodyEntity body,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer == null) {
            return;
        }
        if (localPlayer.isCreative() || localPlayer.isSpectator()) {
            return;
        }

        HiddenBodiesWorldComponent hiddenBodies = HiddenBodiesWorldComponent.KEY.get(localPlayer.getWorld());
        if (!hiddenBodies.isHidden(body.getUuid())) {
            return;
        }

        if (!AssassinVisibility.canPlayerSeeHiddenBodies(localPlayer)) {
            ci.cancel();
        }
    }
}
