package org.agmas.noellesroles.client.mixin.roles.rememberer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 狙击枪手持时的本地移动减速。
 *
 * <p>这次特意改成和 stupidexpress 的 ConvenerMovementSpeedMixin 同一路数：
 * 1. 放到 client mixin 配置里；
 * 2. 直接改写 PlayerEntity#getMovementSpeed 的返回值；
 * 3. 提高优先级，确保它发生在 Wathe 已经把活人基础速度钉成 0.07 / 0.1 之后。
 *
 * <p>之前放在 common mixin 且优先级偏低时，返回值会被 Wathe 自己的速度覆盖链重新压回去，
 * 玩家体感上就会变成“明明写了倍率，但实际上完全没减速”。</p>
 */
@Mixin(value = PlayerEntity.class, priority = 1600)
public abstract class SniperRifleMovementSpeedMixin extends LivingEntity {

    protected SniperRifleMovementSpeedMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float noellesroles$slowSniperHolder(float original) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return original;
        }
        if (!player.getMainHandStack().isOf(ModItems.SNIPER_RIFLE)) {
            return original;
        }
        return original * RemembererConstants.SNIPER_SPEED_MULTIPLIER;
    }
}
