package org.agmas.noellesroles.client.mixin.roles.jason;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.agmas.noellesroles.roles.jason.JasonAbilityBlindnessComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 只放行“杰森自有失明”导致的疾跑限制，保留原版其它疾跑条件。
 */
@Mixin(ClientPlayerEntity.class)
public abstract class JasonAbilityBlindnessSprintMixin {
    @Redirect(
            method = "canStartSprinting",
            at = @At(
                    value = "INVOKE",
                    /*
                     * ClientPlayerEntity#canStartSprinting 的实际字节码以当前类作为
                     * invokevirtual owner；Redirect 必须精确匹配 owner，不能写成声明
                     * 该方法的 LivingEntity，否则运行时会扫描到 0 个目标并阻止启动。
                     */
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"
            )
    )
    private boolean noellesroles$allowSprintThroughJasonBlindness(
            ClientPlayerEntity player,
            RegistryEntry<StatusEffect> effect
    ) {
        if (effect.equals(StatusEffects.BLINDNESS)
                && JasonAbilityBlindnessComponent.KEY.get(player).isOwnedAndActive()) {
            return false;
        }
        return player.hasStatusEffect(effect);
    }
}
