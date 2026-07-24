package org.agmas.noellesroles.client.mixin.roles.robot;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.roles.robot.RobotConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class RobotHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderRobotAbilityHud(@NotNull DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, NoellesRoleRegistry.ROBOT) || !WatheClient.isPlayerAliveAndInSurvival()) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(client.player);
        Text line = ability.cooldown > 0
                ? Text.translatable("tip.noellesroles.cooldown", ability.cooldown / 20)
                /*
                 * 机器人能力固定是夜视，不再复用通用“使用能力”文案。
                 * 单独拆 key 后，后续如果机器人能力 HUD 需要继续细分，也不会影响其它职业的通用提示。
                 */
                : Text.translatable("tip.noellesroles.robot.night_vision", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
        int drawY = context.getScaledWindowHeight() - getTextRenderer().getWrappedLinesHeight(line, 999999);
        context.drawTextWithShadow(
                getTextRenderer(),
                line,
                context.getScaledWindowWidth() - getTextRenderer().getWidth(line),
                drawY,
                RobotConstants.ROLE_COLOR
        );
    }
}
