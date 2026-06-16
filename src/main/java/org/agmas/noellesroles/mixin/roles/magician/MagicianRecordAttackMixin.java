package org.agmas.noellesroles.mixin.roles.magician;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 录制普通左键攻击，并额外让球棒可以打碎魔术师播放体。
 *
 * <p>刺刀左键已经被客户端单独改造成专属数据包，因此不会再走这里。
 * 普通拳击、球棒左键、以及未来其他沿用原版 attack 的近战，则统一由这里兜底。
 */
@Mixin(PlayerEntity.class)
public abstract class MagicianRecordAttackMixin extends LivingEntity {

    @Shadow
    public abstract float getAttackCooldownProgress(float baseTime);

    protected MagicianRecordAttackMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$recordMagicianAttackAndBreakPlayback(Entity target, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        MagicianServerHooks.recordAttack(serverPlayer);

        if (serverPlayer.getMainHandStack().isOf(WatheItems.BAT)
                && this.getAttackCooldownProgress(0.5F) >= 1.0F
                && MagicianServerHooks.stopPlaybackByWeaponTarget(
                target,
                serverPlayer,
                dev.doctor4t.wathe.game.GameConstants.DeathReasons.BAT,
                MagicianServerHooks.getWeaponName(serverPlayer.getMainHandStack())
        )) {
            serverPlayer.getWorld().playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    WatheSounds.ITEM_BAT_HIT,
                    SoundCategory.PLAYERS,
                    3.0F,
                    1.0F
            );
            ci.cancel();
        }
    }
}
