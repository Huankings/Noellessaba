package org.agmas.noellesroles.mixin.roles.thief;

import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.roles.thief.ThiefAllayEntity;
import org.agmas.noellesroles.roles.thief.ThiefConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 真手雷和无声手雷爆炸时对范围内悦灵执行一击必杀；假手雷明确排除。 */
@Mixin(GrenadeEntity.class)
public abstract class ThiefAllayGrenadeHitMixin extends ThrownItemEntity {
    protected ThiefAllayGrenadeHitMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onCollision", at = @At("HEAD"))
    private void noellesroles$killThiefAllays(HitResult hitResult, CallbackInfo ci) {
        if (!(this.getWorld() instanceof ServerWorld world) || this.getStack().isOf(ModItems.FAKE_GRENADE)) {
            return;
        }
        ItemStack weapon = this.getStack().isOf(ModItems.SILENT_GRENADE)
                ? ModItems.SILENT_GRENADE.getDefaultStack()
                : WatheItems.GRENADE.getDefaultStack();
        for (ThiefAllayEntity allay : world.getEntitiesByType(
                NoellesRolesEntities.THIEF_ALLAY_ENTITY_TYPE,
                entity -> entity.isOperationallyAlive()
                        && this.getBoundingBox().expand(ThiefConstants.ALLAY_GRENADE_RADIUS)
                        .intersects(entity.getBoundingBox())
        )) {
            allay.killByWeapon(
                    this.getOwner() instanceof ServerPlayerEntity player ? player : null,
                    weapon,
                    GameConstants.DeathReasons.GRENADE
            );
        }
    }
}
