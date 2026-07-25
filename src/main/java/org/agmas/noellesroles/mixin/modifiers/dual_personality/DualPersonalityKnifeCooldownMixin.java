package org.agmas.noellesroles.mixin.modifiers.dual_personality;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KnifeStabPayload.Receiver.class)
public class DualPersonalityKnifeCooldownMixin {

    @Inject(method = "receive", at = @At("TAIL"))
    private void noellesroles$shortenDoubleActiveKnifeCooldown(
            @NotNull KnifeStabPayload payload,
            ServerPlayNetworking.@NotNull Context context,
            CallbackInfo ci
    ) {
        ServerPlayerEntity player = context.player();
        if (DualPersonalityComponent.KEY.get(player.getWorld()).isDoubleActive(player.getUuid())) {
            /*
             * Wathe 原本会在匕首命中后设置自己的冷却。
             * 注入 TAIL 后再写一次较短冷却，相当于只在双活阶段覆盖成 1 秒。
             */
            player.getItemCooldownManager().set(WatheItems.KNIFE, DualPersonalityConstants.DOUBLE_ACTIVE_KNIFE_COOLDOWN_TICKS);
        }
    }
}
