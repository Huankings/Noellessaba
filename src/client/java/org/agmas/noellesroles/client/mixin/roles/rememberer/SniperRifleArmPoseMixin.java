package org.agmas.noellesroles.client.mixin.roles.rememberer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让狙击枪在第三人称与他人视角下，直接使用“托举瞄准”的手臂姿态。
 *
 * <p>用户想要的观感接近飞斧蓄力/Wathe 球棒那种“手臂架起来”的状态，
 * 但狙击枪本身并不是长按蓄力物品，所以不能单靠 {@code UseAction.BOW} 自动得到这个姿态。
 * 这里直接在玩家渲染器的 arm pose 判定末尾特判狙击枪，
 * 统一改成原版现成的 {@link BipedEntityModel.ArmPose#CROSSBOW_CHARGE}，
 * 这样别人看你，以及你切第三人称看自己时，都会稳定呈现出托举枪械的姿势。</p>
 *
 * <p>另外这里刻意保留“如果前面的 mixin 已经把这只手处理成空手，就不再强行覆盖”的判断，
 * 避免和控制者、隐形持物等已有的手臂姿态隐藏逻辑打架。</p>
 */
@Mixin(PlayerEntityRenderer.class)
public class SniperRifleArmPoseMixin {

    @Inject(method = "getArmPose", at = @At("TAIL"), cancellable = true)
    private static void noellesroles$raiseSniperRifle(AbstractClientPlayerEntity player,
                                                      Hand hand,
                                                      CallbackInfoReturnable<BipedEntityModel.ArmPose> cir) {
        if (cir.getReturnValue() == BipedEntityModel.ArmPose.EMPTY) {
            return;
        }
        if (player.getStackInHand(hand).isOf(ModItems.SNIPER_RIFLE)) {
            cir.setReturnValue(BipedEntityModel.ArmPose.CROSSBOW_CHARGE);
        }
    }
}
