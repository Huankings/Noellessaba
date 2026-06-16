package org.agmas.noellesroles.mixin.roles.magician;

import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 普通手雷爆炸时，把爆炸范围内的播放体也一并判定为“被玩法伤害命中”。
 *
 * <p>假手雷不应该结束播放，因此这里显式排除；
 * 无声手雷则由它自己的 mixin 在独立分支里处理。
 */
@Mixin(GrenadeEntity.class)
public abstract class MagicianGrenadePlaybackHitMixin extends ThrownItemEntity {

    protected MagicianGrenadePlaybackHitMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onCollision", at = @At("HEAD"))
    private void noellesroles$explodePlaybackBodies(HitResult hitResult, CallbackInfo ci) {
        if (!(this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
            return;
        }
        if (this.getStack().isOf(ModItems.FAKE_GRENADE)) {
            return;
        }

        MagicianServerHooks.stopPlaybackInExplosion(
                serverWorld,
                this.getBoundingBox().expand(3.0F),
                this.getOwner() instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null,
                GameConstants.DeathReasons.GRENADE,
                MagicianServerHooks.getWeaponName(this.getStack())
        );
    }
}
