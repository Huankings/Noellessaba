package org.agmas.noellesroles.mixin.roles.vecna;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.vecna.VecnaPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 维克那用匕首攻击已被颠倒标记的目标时，不进入 Wathe 默认匕首冷却。
 *
 * <p>Wathe 当前没有暴露“本次匕首命中是否应跳过冷却”的 API，
 * 因此这里只对 KnifeStabPayload.Receiver 做窄注入；真正的距离、可攻击性、死亡流程
 * 仍完全由 Wathe 原接收器负责，避免复制整段网络处理逻辑。</p>
 */
@Mixin(targets = "dev.doctor4t.wathe.util.KnifeStabPayload$Receiver")
public abstract class VecnaKnifeCooldownMixin {
    @Inject(method = "receive", at = @At("TAIL"))
    private void noellesroles$removeVecnaMarkedKnifeCooldown(
            KnifeStabPayload payload,
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        ServerPlayerEntity attacker = context.player();
        if (!(attacker.getServerWorld().getEntityById(payload.target()) instanceof PlayerEntity target)) {
            return;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(attacker.getWorld());
        VecnaPlayerComponent mark = VecnaPlayerComponent.KEY.get(target);
        if (game.isRole(attacker, NoellesRoleRegistry.VECNA)
                && mark.isMarked()
                && attacker.getUuid().equals(mark.getMarker())) {
            attacker.getItemCooldownManager().remove(dev.doctor4t.wathe.index.WatheItems.KNIFE);
        }
    }
}
