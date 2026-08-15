package org.agmas.noellesroles.mixin.roles.jason;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonWoundManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 杰森用匕首处决重伤倒地玩家时，不进入匕首冷却。
 *
 * <p>Wathe 的匕首冷却写在 payload receive 尾部。
 * 杰森死亡 handler 会在 DeathApi afterAttempt 阶段标记“本次确实处决成功”，
 * 这里在 Wathe 写完默认冷却后再清掉，避免未击杀或被护盾挡下时也白嫖无冷却。</p>
 */
@Mixin(KnifeStabPayload.Receiver.class)
public abstract class JasonKnifeExecutionCooldownMixin {
    @Inject(method = "receive", at = @At("TAIL"))
    private void noellesroles$clearJasonExecutionKnifeCooldown(
            @NotNull KnifeStabPayload payload,
            ServerPlayNetworking.@NotNull Context context,
            CallbackInfo ci
    ) {
        ServerPlayerEntity player = context.player();
        if (JasonWoundManager.consumeKnifeExecutionNoCooldown(player)) {
            player.getItemCooldownManager().set(WatheItems.KNIFE, 0);
        }
    }
}
