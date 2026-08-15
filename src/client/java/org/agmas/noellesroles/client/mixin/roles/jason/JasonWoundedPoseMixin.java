package org.agmas.noellesroles.client.mixin.roles.jason;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonWoundedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 杰森重伤倒地时的客户端姿势稳定器。
 *
 * <p>服务端已经每 tick 把倒地玩家压成 SWIMMING，但本地客户端仍会先按原版
 * updatePose 把自己临时算回站立/潜行姿势，再等服务端数据同步压回爬行姿势，
 * 于是第一人称眼高会出现“站起一下又趴下”的回弹。这里在客户端本地姿势计算结束后
 * 立刻补回 SWIMMING，只改变视觉表现，不替代服务端的动作封锁和生死判定。</p>
 */
@Mixin(PlayerEntity.class)
public abstract class JasonWoundedPoseMixin {
    @Inject(method = "updatePose", at = @At("TAIL"))
    private void noellesroles$keepJasonWoundedCrawlingPose(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!GameFunctions.isPlayerAliveAndSurvival(player)
                || !JasonWoundedPlayerComponent.KEY.get(player).isWounded()
                || player.getPose() == EntityPose.SWIMMING) {
            return;
        }

        player.setPose(EntityPose.SWIMMING);
    }
}
