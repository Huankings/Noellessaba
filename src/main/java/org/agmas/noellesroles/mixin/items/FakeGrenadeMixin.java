package org.agmas.noellesroles.mixin.items;

import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheParticles;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 假手雷爆炸拦截。
 *
 * <p>这里不去改写 wathe 的投掷流程，而是在 GrenadeEntity 真正碰撞结算时做分流：
 * 如果这枚手雷携带的是 fake_grenade 的 ItemStack，就照常播放爆炸音效、粒子与物品碎片，
 * 但跳过原版那段会对周围玩家调用 killPlayer 的代码。
 *
 * <p>这样能把“表现”和“伤害”拆开处理：
 * 表现继续保持原版一致，伤害则只在假手雷分支里被精准取消。
 */
@Mixin(GrenadeEntity.class)
public abstract class FakeGrenadeMixin extends ThrownItemEntity {

    protected FakeGrenadeMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void noellesroles$handleFakeGrenadeCollision(HitResult hitResult, CallbackInfo ci) {
        if (!this.getStack().isOf(ModItems.FAKE_GRENADE)) {
            return;
        }

        // 先保留原始 thrown entity 的基础碰撞处理，避免漏掉原版的通用逻辑。
        super.onCollision(hitResult);

        if (this.getWorld() instanceof ServerWorld world) {
            // 音效、爆炸粒子和烟雾全部照搬真手雷，保证观感一致。
            world.playSound(
                    null,
                    this.getBlockPos(),
                    WatheSounds.ITEM_GRENADE_EXPLODE,
                    SoundCategory.PLAYERS,
                    5f,
                    1f + this.getRandom().nextFloat() * .1f - .05f
            );
            world.spawnParticles(WatheParticles.BIG_EXPLOSION, this.getX(), this.getY() + .1f, this.getZ(), 1, 0, 0, 0, 0);
            world.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + .1f, this.getZ(), 100, 0, 0, 0, .2f);
            world.spawnParticles(
                    new ItemStackParticleEffect(ParticleTypes.ITEM, WatheItems.THROWN_GRENADE.getDefaultStack()),
                    this.getX(),
                    this.getY() + .1f,
                    this.getZ(),
                    100,
                    0,
                    0,
                    0,
                    1f
            );

            // 假手雷最重要的区别就在这里：
            // 不执行 wathe 原版的 killPlayer 循环，只做爆炸表现，然后直接销毁实体。
            this.discard();
        }

        ci.cancel();
    }
}
