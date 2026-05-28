package org.agmas.noellesroles.client.mixin.roles.angel;

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
import org.agmas.noellesroles.roles.angel.AngelAbility;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 天使右下角 HUD。
 */
@Mixin(InGameHud.class)
public abstract class AngelHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void renderAngelHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.ANGEL)) {
            return;
        }

        AngelPlayerComponent angelComponent = AngelPlayerComponent.KEY.get(client.player);
        AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(client.player);
        boolean guardMode = AngelAbility.isGuardMode(client.player);

        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable(
                "hud.noellesroles.angel.mode",
                Text.translatable(guardMode ? "hud.noellesroles.angel.mode.guard" : "hud.noellesroles.angel.mode.soothe")
        ));
        lines.add(Text.translatable("hud.noellesroles.angel.guarded", getGuardedTargetName(angelComponent.getGuardedTarget())));

        if (abilityComponent.cooldown > 0) {
            lines.add(Text.translatable("tip.noellesroles.cooldown", Math.max(0, (abilityComponent.cooldown + 19) / 20)));
        } else if (guardMode) {
            lines.add(Text.translatable("hud.noellesroles.angel.use_guard", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
        } else {
            lines.add(Text.translatable("hud.noellesroles.angel.use_soothe", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
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
                    Noellesroles.ANGEL.color()
            );
        }
    }

    private Text getGuardedTargetName(UUID guardedUuid) {
        if (guardedUuid == null) {
            return Text.translatable("hud.noellesroles.angel.none");
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return Text.translatable("hud.noellesroles.angel.none");
        }

        if (guardedUuid.equals(client.player.getUuid())) {
            return client.player.getDisplayName();
        }

        PlayerListEntry entry = client.player.networkHandler.getPlayerListEntry(guardedUuid);
        if (entry != null) {
            return Text.literal(entry.getProfile().getName());
        }

        return Text.translatable("hud.noellesroles.angel.none");
    }
}
