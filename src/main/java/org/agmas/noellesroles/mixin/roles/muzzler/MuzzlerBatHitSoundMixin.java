package org.agmas.noellesroles.mixin.roles.muzzler;

import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.muzzler.MuzzlerPsychoUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 静语者疯魔状态下屏蔽球棒命中音效。
 */
@Mixin(value = World.class, priority = 1500)
public class MuzzlerBatHitSoundMixin {
    @Inject(
            method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$muteBatHitForMuzzlerPsycho(PlayerEntity player,
                                                         double x,
                                                         double y,
                                                         double z,
                                                         SoundEvent sound,
                                                         SoundCategory category,
                                                         float volume,
                                                         float pitch,
                                                         CallbackInfo ci) {
        /*
         * 先按 Wathe 的 bat_hit 声音收窄范围，再判断声源玩家是不是静语者疯魔。
         * 这样不会误伤其它普通音效，也不会影响非静语者使用疯魔模式时的反馈。
         */
        if (sound == WatheSounds.ITEM_BAT_HIT && MuzzlerPsychoUtil.isMuzzlerPsycho(player)) {
            ci.cancel();
        }
    }
}
