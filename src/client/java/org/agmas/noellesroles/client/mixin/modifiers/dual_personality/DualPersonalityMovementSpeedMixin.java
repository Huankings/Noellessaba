package org.agmas.noellesroles.client.mixin.modifiers.dual_personality;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerEntity.class, priority = 1700)
public abstract class DualPersonalityMovementSpeedMixin extends LivingEntity {

    protected DualPersonalityMovementSpeedMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float noellesroles$boostDoubleActiveSpeed(float original) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!DualPersonalityClientState.isDoubleActive(player)) {
            return original;
        }
        // 双活阶段给两个玩家 1.9 倍移动速度；普通轮换阶段不改变任何速度。
        return original * 1.9F;
    }
}
