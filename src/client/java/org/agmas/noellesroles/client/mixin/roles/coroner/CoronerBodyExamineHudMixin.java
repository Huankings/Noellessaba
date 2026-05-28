package org.agmas.noellesroles.client.mixin.roles.coroner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RoleNameRenderer.class)
public abstract class CoronerBodyExamineHudMixin {

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void renderCoronerExamineHud(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());

        if (gameWorldComponent.isRole(player, Noellesroles.CORONER)) {
            // 检查玩家是否在看着尸体
            float range = 3.0F; // 3格距离
            HitResult hit = ProjectileUtil.getCollision(player,
                    entity -> entity instanceof PlayerBodyEntity,
                    range);

            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof PlayerBodyEntity body) {
                CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(player);

                // 检查是否已经检查过这个尸体
                boolean alreadyExamined = coronerComp.examinedBodies.contains(body.getUuid());

                // 在屏幕中央显示提示
                context.getMatrices().push();
                context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F,
                        context.getScaledWindowHeight() / 2.0F + 20.0F, 0.0F);
                context.getMatrices().scale(0.6F, 0.6F, 1.0F);

                if (alreadyExamined) {
                    // 已检查过
                    Text text = Text.literal("§7已检查过此尸体");
                    context.drawTextWithShadow(renderer, text,
                            -renderer.getWidth(text) / 2, 0, Colors.GRAY);
                } else {
                    // 未检查过，显示检查提示
                    Text text = Text.literal("§e靠近检查尸体 §7(获得金币)");
                    context.drawTextWithShadow(renderer, text,
                            -renderer.getWidth(text) / 2, 0, Colors.YELLOW);

                    // 显示统计信息
                    Text stats = Text.literal(String.format("§7已检查: %d具尸体 | 总计: %d金币",
                            coronerComp.totalBodiesExamined,
                            coronerComp.totalGoldEarned));
                    context.drawTextWithShadow(renderer, stats,
                            -renderer.getWidth(stats) / 2, 12, Colors.LIGHT_GRAY);
                }

                context.getMatrices().pop();
            }
        }
    }
}
