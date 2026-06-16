package org.agmas.noellesroles.client.mixin.roles.magician;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.roles.magician.MagicianPlayerComponent;
import org.agmas.noellesroles.roles.magician.MagicianStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔术师右下角 HUD。
 */
@Mixin(InGameHud.class)
public abstract class MagicianHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderMagicianHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !GameFunctions.isPlayerAliveAndSurvival(client.player)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.MAGICIAN)) {
            return;
        }

        MagicianPlayerComponent component = MagicianPlayerComponent.KEY.get(client.player);
        AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(client.player);

        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("hud.noellesroles.magician.selected_target", Text.literal(component.getSelectedTargetName())));

        if (component.stage == MagicianStage.RECORDING) {
            lines.add(Text.translatable("hud.noellesroles.magician.recording", Math.max(0, (component.stageTicksRemaining + 19) / 20)));
            lines.add(Text.translatable("hud.noellesroles.magician.stop_recording", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
        } else if (component.stage == MagicianStage.READY_PLAYBACK) {
            lines.add(Text.translatable("hud.noellesroles.magician.start_playback", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
        } else if (component.stage == MagicianStage.PLAYING) {
            lines.add(Text.translatable("hud.noellesroles.magician.playing", Math.max(0, (component.stageTicksRemaining + 19) / 20)));
            lines.add(Text.translatable("hud.noellesroles.magician.stop_playback", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
        } else if (abilityComponent.cooldown > 0) {
            lines.add(Text.translatable("hud.noellesroles.magician.cooldown", Math.max(0, (abilityComponent.cooldown + 19) / 20)));
        } else {
            lines.add(Text.translatable("hud.noellesroles.magician.start_recording", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
        }

        int drawY = context.getScaledWindowHeight();
        for (int i = lines.size() - 1; i >= 0; --i) {
            Text line = lines.get(i);
            drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
            context.drawTextWithShadow(
                    getTextRenderer(),
                    line,
                    context.getScaledWindowWidth() - getTextRenderer().getWidth(line),
                    drawY,
                    Noellesroles.MAGICIAN.color()
            );
        }
    }
}
