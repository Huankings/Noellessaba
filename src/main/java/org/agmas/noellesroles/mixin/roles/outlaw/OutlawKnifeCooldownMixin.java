package org.agmas.noellesroles.mixin.roles.outlaw;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wathe 匕首包末尾会写默认冷却；亡命徒在返回前精准清除该冷却。 */
@Mixin(KnifeStabPayload.Receiver.class)
public abstract class OutlawKnifeCooldownMixin {
    @Inject(method = "receive", at = @At("RETURN"))
    private void noellesroles$removeOutlawKnifeCooldown(
            KnifeStabPayload payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        ServerPlayerEntity player = context.player();
        if (GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.OUTLAW)) {
            player.getItemCooldownManager().remove(WatheItems.KNIFE);
        }
    }
}
