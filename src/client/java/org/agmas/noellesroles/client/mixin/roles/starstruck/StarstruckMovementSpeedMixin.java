package org.agmas.noellesroles.client.mixin.roles.starstruck;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.starstruck.StarstruckConstants;
import org.agmas.noellesroles.roles.starstruck.StarstruckPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 星界能力期间的移动速度覆盖。
 */
@Mixin(value = PlayerEntity.class, priority = 1500)
public abstract class StarstruckMovementSpeedMixin extends LivingEntity {
    protected StarstruckMovementSpeedMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float noellesroles$starstruckMovementSpeed(float original) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!StarstruckConstants.ABILITY_AFFECTS_MOVEMENT_SPEED
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.STARSTRUCK)
                || StarstruckPlayerComponent.KEY.get(player).ticks <= 0) {
            return original;
        }

        /*
         * StarryExpress 原实现使用固定速度值，而不是在当前速度上乘倍率。
         * 这里照搬该语义，确保星界能力期间行走/冲刺手感一致。
         */
        return player.isSprinting()
                ? StarstruckConstants.ABILITY_SPRINT_SPEED
                : StarstruckConstants.ABILITY_WALK_SPEED;
    }
}
