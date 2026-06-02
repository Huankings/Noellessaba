package org.agmas.noellesroles.mixin.roles.spiritualist;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵术师脱体期间的本体伤害兜底。
 *
 * <p>这里把两种状态分开处理：</p>
 * <p>1. 出窍时：本体依旧会吃到真实伤害，因此只要受到有效伤害就立刻回魂；</p>
 * <p>2. 附身时：本体必须完全免疫普通 damage 链路，
 *    否则环境伤害或别的模组直接调 LivingEntity#damage 仍可能把空气壳本体磨死。</p>
 */
@Mixin(LivingEntity.class)
public abstract class SpiritualistProjectionDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void noellesroles$cancelProjectionOnDamage(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (amount <= 0 || !((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }

        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(player);
        if (component.isPossessing()) {
            cir.setReturnValue(false);
            return;
        }

        if (component.isProjecting()) {
            SpiritualistManager.endProjection(player, true);
        }
    }
}
