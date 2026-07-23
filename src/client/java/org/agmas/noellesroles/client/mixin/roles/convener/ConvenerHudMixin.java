package org.agmas.noellesroles.client.mixin.roles.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.roles.convener.ConvenerDisguiseResolver;
import org.agmas.noellesroles.roles.convener.ConvenerConstants;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.agmas.noellesroles.roles.convener.ConvenerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 召集者右下角状态 HUD。
 */
@Mixin(InGameHud.class)
public abstract class ConvenerHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderConvenerStatus(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.CONVENER)) {
            return;
        }

        ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(client.player);
        ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(client.player);

        List<Text> lines = new ArrayList<>();
        if (ConvenerConstants.COUNTER_SHIELD_ENABLED) {
            lines.add(Text.translatable("hud.noellesroles.convener.counter_shield_layers", convener.getCounterShieldLayers()));
        }

        lines.add(convener.hasUnlockedMorphs()
                ? Text.translatable("hud.noellesroles.convener.current_disguise", resolveHudDisguiseName(client.player, disguise.getDisguiseUuid()))
                : Text.translatable("hud.noellesroles.convener.locked"));
        lines.add(Text.translatable("hud.noellesroles.convener.progress", convener.getSummonCount(), convener.getRequiredSummons()));

        if (ConvenerConstants.COUNTER_SHIELD_ENABLED) {
            lines.add(Text.translatable("hud.noellesroles.convener.tasks_to_next_shield", convener.getTasksRemainingForNextShield()));
        }

        /*
         * 按从下往上绘制，和 Noelles 现有职业 HUD 保持一致。
         * 文本只展示召集者自己需要持续关注的状态，不把操作说明塞进右下角。
         */
        int drawY = context.getScaledWindowHeight();
        for (int i = lines.size() - 1; i >= 0; i--) {
            Text line = lines.get(i);
            drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
            context.drawTextWithShadow(
                    getTextRenderer(),
                    line,
                    context.getScaledWindowWidth() - getTextRenderer().getWidth(line),
                    drawY,
                    Noellesroles.CONVENER.color()
            );
        }
    }

    private Text resolveHudDisguiseName(ClientPlayerEntity player, UUID disguiseUuid) {
        Text disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(player, disguiseUuid);
        return disguiseName != null ? disguiseName : Text.translatable("hud.noellesroles.convener.waiting");
    }
}
