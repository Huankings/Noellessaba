package org.agmas.noellesroles.mixin.roles.jason;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.agmas.noellesroles.roles.jason.JasonWoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 重伤倒地期间的服务端左键兜底。
 *
 * <p>客户端会先阻止攻击键，但不能把客户端输入当作权限边界。Wathe 的匕首本身允许
 * 存活玩家攻击，因此这里在服务端 {@link ServerPlayerEntity#attack(Entity)} 入口取消倒地玩家
 * 的攻击，防止延迟包、宏或篡改客户端绕过限制。背包整理不经过本入口，仍被保留。</p>
 */
@Mixin(ServerPlayerEntity.class)
public abstract class JasonWoundedAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonWoundedAttack(Entity target, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (JasonWoundManager.isWoundedActionLocked(player) || JasonAbilityRules.isAbilityActionLocked(player)) {
            ci.cancel();
        }
    }
}
