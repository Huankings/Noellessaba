package org.agmas.noellesroles.client.mixin.roles.myers;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.myers.MyersPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 让第一人称 HeldItemRenderer 直接把强制举刀视为主手使用状态。 */
@Mixin(HeldItemRenderer.class)
public abstract class MyersHeldItemRendererMixin {
    @Inject(method = "getUsingItemHandRenderType", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderForcedButcher(
            ClientPlayerEntity player,
            CallbackInfoReturnable<Object> cir
    ) {
        if (!MyersPlayerComponent.KEY.get(player).isForcedHold()
                || !player.getMainHandStack().isOf(ModItems.BUTCHER_KNIFE)) {
            return;
        }
        Object renderMainHand = noellesroles$mainHandRenderType();
        if (renderMainHand != null) {
            cir.setReturnValue(renderMainHand);
        }
    }

    @Unique
    private static Object noellesroles$mainHandRenderType() {
        try {
            Class<?> type = Class.forName("net.minecraft.client.render.item.HeldItemRenderer$HandRenderType");
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object result = Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), "RENDER_MAIN_HAND_ONLY");
            return result;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
