package org.agmas.noellesroles.client.mixin.roles.winder;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.roles.winder.WinderPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 风灵师右下角 HUD。
 */
@Mixin(InGameHud.class)
public abstract class WinderHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void renderWinderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorldComponent.isRole(client.player, Noellesroles.WINDER)) {
            return;
        }

        WinderPlayerComponent winderComponent = WinderPlayerComponent.KEY.get(client.player);
        AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(client.player);

        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("tip.winder.selected", getSelectedPlayerName(winderComponent.getSelectedTarget())));

        if (winderComponent.isFloatingActive()) {
            lines.add(Text.translatable("tip.winder.stop", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
            lines.add(Text.translatable("tip.winder.active", Math.max(0, (winderComponent.getFloatingTicksRemaining() + 19) / 20)));
        } else if (abilityComponent.cooldown > 0) {
            lines.add(Text.translatable("tip.noellesroles.cooldown", Math.max(0, (abilityComponent.cooldown + 19) / 20)));
        } else {
            lines.add(Text.translatable("tip.winder.use", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
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
                    Noellesroles.WINDER.color()
            );
        }
    }

    private Text getSelectedPlayerName(UUID selectedUuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return Text.literal("未知玩家");
        }

        if (selectedUuid.equals(client.player.getUuid())) {
            return client.player.getDisplayName();
        }

        PlayerListEntry entry = client.player.networkHandler.getPlayerListEntry(selectedUuid);
        if (entry != null) {
            return Text.literal(entry.getProfile().getName());
        }

        return Text.literal("未知玩家");
    }
}
