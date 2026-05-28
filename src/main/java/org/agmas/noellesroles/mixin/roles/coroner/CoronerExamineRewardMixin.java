package org.agmas.noellesroles.mixin.roles.coroner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class CoronerExamineRewardMixin {

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void onPlayerTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // 只在服务器端执行，并且每秒检查一次（20 tick = 1秒）
        if (!player.getWorld().isClient() && player.age % 20 == 0) {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());

            // 检查是否是验尸官
            if (gameWorldComponent.isRole(player, Noellesroles.CORONER)) {
                // 查找玩家周围1格内的尸体
                player.getWorld().getEntitiesByClass(
                        PlayerBodyEntity.class,
                        player.getBoundingBox().expand(1),
                        body -> {
                            // 检查是否已经检查过这个尸体
                            CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(player);
                            if (!coronerComp.examinedBodies.contains(body.getUuid())) {
                                // 给予金币奖励
                                boolean correctIdentification = true;
                                boolean rewarded = coronerComp.examineBody(body.getUuid(), correctIdentification);

                                if (rewarded) {
                                    int reward = correctIdentification ?
                                            CoronerPlayerComponent.BASE_REWARD + CoronerPlayerComponent.BONUS_REWARD :
                                            CoronerPlayerComponent.BASE_REWARD;

                                    String message = String.format("§a检查尸体获得 §e%d§a 金币! (总计: %d具尸体, %d金币)",
                                            reward,
                                            coronerComp.totalBodiesExamined,
                                            coronerComp.totalGoldEarned);

                                    player.sendMessage(Text.literal(message), false);
                                }
                                return true;
                            }
                            return false;
                        }
                );
            }
        }
    }
}