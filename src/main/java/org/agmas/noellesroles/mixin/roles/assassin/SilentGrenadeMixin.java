package org.agmas.noellesroles.mixin.roles.assassin;

import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 无声手雷爆炸分支。
 *
 * <p>这里保留 Wathe 原版手雷的爆炸粒子、烟雾与击杀范围，
 * 唯一移除的是爆炸声音，并把真实的“无声手雷”物品信息写进死亡回放。</p>
 */
@Mixin(GrenadeEntity.class)
public abstract class SilentGrenadeMixin extends ThrownItemEntity {

    protected SilentGrenadeMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void noellesroles$handleSilentGrenadeCollision(HitResult hitResult, CallbackInfo ci) {
        if (!this.getStack().isOf(ModItems.SILENT_GRENADE)) {
            return;
        }

        super.onCollision(hitResult);

        if (this.getWorld() instanceof ServerWorld world) {
            ItemStack replayStack = this.getStack().isEmpty()
                    ? ModItems.SILENT_GRENADE.getDefaultStack()
                    : this.getStack().copyWithCount(1);

            world.spawnParticles(WatheParticles.BIG_EXPLOSION, this.getX(), this.getY() + .1F, this.getZ(), 1, 0, 0, 0, 0);
            world.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + .1F, this.getZ(), 100, 0, 0, 0, .2F);
            world.spawnParticles(
                    new ItemStackParticleEffect(ParticleTypes.ITEM, replayStack),
                    this.getX(),
                    this.getY() + .1F,
                    this.getZ(),
                    100,
                    0,
                    0,
                    0,
                    1F
            );

            NbtCompound replayItemData = GameFunctions.createReplayItemData(world, replayStack);
            for (ServerPlayerEntity player : world.getPlayers(serverPlayerEntity ->
                    this.getBoundingBox().expand(3F).contains(serverPlayerEntity.getPos())
                            && GameFunctions.isPlayerAliveAndSurvival(serverPlayerEntity))) {
                GameFunctions.killPlayer(
                        player,
                        true,
                        this.getOwner() instanceof PlayerEntity playerEntity ? playerEntity : null,
                        GameConstants.DeathReasons.GRENADE,
                        replayItemData
                );
            }

            this.discard();
        }

        ci.cancel();
    }
}
