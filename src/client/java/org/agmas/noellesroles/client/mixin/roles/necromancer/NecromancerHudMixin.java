package org.agmas.noellesroles.client.mixin.roles.necromancer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.roles.necromancer.NecromancerWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RoleNameRenderer.class)
public class NecromancerHudMixin {
    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void noellesroles$renderNecromancerHud(
            TextRenderer renderer,
            ClientPlayerEntity player,
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        PlayerBodyEntity targetBody = findTargetBody(player);
        if (targetBody == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.NECROMANCER) || WatheClient.isPlayerSpectatingOrCreative()) {
            return;
        }

        Text status = Text.translatable("hud.noellesroles.necromancer.possible_revive");
        NecromancerWorldComponent necromancerWorld = NecromancerWorldComponent.KEY.get(player.getWorld());
        if (necromancerWorld.getAvailableRevives() < 1) {
            status = Text.translatable("hud.noellesroles.necromancer.no_possible_revive");
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) {
            status = Text.translatable("hud.noellesroles.necromancer.cooldown", ability.cooldown / 20);
        }

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F, context.getScaledWindowHeight() / 2.0F + 6.0F, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        context.drawTextWithShadow(renderer, status, -renderer.getWidth(status) / 2, 32, Noellesroles.NECROMANCER.color());
        context.getMatrices().pop();
    }

    private static PlayerBodyEntity findTargetBody(ClientPlayerEntity player) {
        /*
         * NoellesRoles 已经有 targetBody 缓存给验尸官/秃鹫使用。
         * 这里仍然自己做一次窄射线，避免死灵法师 HUD 依赖另一个 mixin 的注入顺序。
         */
        float range = GameFunctions.isPlayerSpectatingOrCreative(MinecraftClient.getInstance().player) ? 8.0F : 2.0F;
        HitResult hitResult = ProjectileUtil.getCollision(player, entity -> entity instanceof PlayerBodyEntity, range);
        NoellesrolesClient.targetBody = null;
        if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerBodyEntity body) {
            NoellesrolesClient.targetBody = body;
            return body;
        }
        return null;
    }
}
