package org.agmas.noellesroles.client.mixin.roles.assassin;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.agmas.noellesroles.roles.assassin.AssassinVisibility;
import org.agmas.noellesroles.roles.assassin.HiddenBodiesWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让看不见刺客尸体的玩家，准心也不会选中这些尸体。
 */
@Mixin(LivingEntity.class)
public abstract class AssassinBodyCanHitMixin {

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void noellesroles$disableHiddenAssassinBodyTargeting(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof PlayerBodyEntity body)) {
            return;
        }
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer == null) {
            return;
        }
        if (localPlayer.isCreative() || localPlayer.isSpectator()) {
            return;
        }

        HiddenBodiesWorldComponent hiddenBodies = HiddenBodiesWorldComponent.KEY.get(localPlayer.getWorld());
        if (hiddenBodies.isHidden(body.getUuid()) && !AssassinVisibility.canPlayerSeeHiddenBodies(localPlayer)) {
            cir.setReturnValue(false);
        }
    }
}
