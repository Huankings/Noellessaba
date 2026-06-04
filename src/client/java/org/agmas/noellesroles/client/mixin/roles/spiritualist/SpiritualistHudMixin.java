package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistConstants;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistTargeting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 灵术师右下角 HUD。
 */
@Mixin(InGameHud.class)
public abstract class SpiritualistHudMixin {

    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderSpiritualistHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.SPIRITUALIST)
                || !GameFunctions.isPlayerAliveAndSurvival(client.player)) {
            return;
        }

        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(client.player);
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(client.player);
        List<Text> lines = new ArrayList<>();

        lines.add(Text.translatable(
                "hud.noellesroles.spiritualist.state",
                Text.translatable(getStateTranslationKey(component))
        ));

        if (ability.cooldown > 0) {
            lines.add(Text.translatable("tip.noellesroles.cooldown", Math.max(1, ability.cooldown / 20)));
        } else if (component.isProjecting()) {
            lines.add(Text.translatable(
                    "hud.noellesroles.spiritualist.end_projection",
                    NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
            ));
        } else if (component.isPossessing()) {
            lines.add(Text.translatable(
                    "hud.noellesroles.spiritualist.end_possession",
                    NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
            ));
        } else if (isPossessionAim(client.player)) {
            lines.add(Text.translatable(
                    "hud.noellesroles.spiritualist.start_possession",
                    NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
            ));
        } else {
            lines.add(Text.translatable(
                    "hud.noellesroles.spiritualist.start_projection",
                    NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
            ));
        }

        int drawY = context.getScaledWindowHeight();
        for (int index = lines.size() - 1; index >= 0; index--) {
            MutableText line = lines.get(index).copy().withColor(SpiritualistConstants.ROLE_COLOR);
            drawY -= this.getTextRenderer().fontHeight;
            context.drawTextWithShadow(
                    this.getTextRenderer(),
                    line,
                    context.getScaledWindowWidth() - this.getTextRenderer().getWidth(line),
                    drawY,
                    SpiritualistConstants.ROLE_COLOR
            );
        }
    }

    private static boolean isPossessionAim(PlayerEntity player) {
        return SpiritualistTargeting.isPossessionAim(player);
    }

    private static String getStateTranslationKey(SpiritualistPlayerComponent component) {
        if (component.isProjecting()) {
            return "hud.noellesroles.spiritualist.state.projecting";
        }
        if (component.isPossessing()) {
            return "hud.noellesroles.spiritualist.state.possessing";
        }
        return "hud.noellesroles.spiritualist.state.normal";
    }
}
