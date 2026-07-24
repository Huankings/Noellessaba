package org.agmas.noellesroles.client.mixin.roles.convener;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.convener.ConvenerConstants;
import org.agmas.noellesroles.roles.convener.ConvenerMomentumComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 召集者成功召集后的本地移速覆盖。
 */
@Mixin(value = PlayerEntity.class, priority = 1600)
public abstract class ConvenerMovementSpeedMixin extends LivingEntity {
    protected ConvenerMovementSpeedMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float noellesroles$boostConvenerSpeed(float original) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.CONVENER)
                || ConvenerMomentumComponent.KEY.get(player).getTicks() <= 0) {
            return original;
        }

        /*
         * Wathe 会在 getMovementSpeed 链路里固定列车玩法基础速度。
         * 因此召集者爆发也在同一返回值上乘倍率，保证客户端移动手感和服务端状态同步。
         */
        return original * (float) (1.0D + ConvenerConstants.SUMMON_SPEED_MULTIPLIER_BONUS);
    }
}
