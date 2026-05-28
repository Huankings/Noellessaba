package org.agmas.noellesroles.client.mixin.roles.prophet;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
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
import org.agmas.noellesroles.roles.prophet.ProphetConstants;
import org.agmas.noellesroles.roles.prophet.ProphetPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 先知右下角 HUD。
 */
@Mixin(InGameHud.class)
public abstract class ProphetHudMixin {

    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void renderProphetHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.PROPHET)) {
            return;
        }

        ProphetPlayerComponent prophetComponent = ProphetPlayerComponent.KEY.get(client.player);
        AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(client.player);
        PlayerShopComponent shopComponent = PlayerShopComponent.KEY.get(client.player);

        List<Text> lines = new ArrayList<>();
        if (prophetComponent.hasMarkedTarget()) {
            lines.add(Text.translatable("tip.prophet.target_marked", getMarkedTargetName(prophetComponent.getMarkedTarget())));
        } else {
            lines.add(Text.translatable("tip.prophet.random_reveal"));
        }

        if (abilityComponent.cooldown > 0) {
            lines.add(Text.translatable("tip.noellesroles.cooldown", Math.max(0, (abilityComponent.cooldown + 19) / 20)));
        } else if (shopComponent.balance < ProphetConstants.REVEAL_COST) {
            lines.add(Text.translatable("tip.prophet.not_enough_money", ProphetConstants.REVEAL_COST));
        } else {
            lines.add(Text.translatable("tip.prophet.use", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()));
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
                    Noellesroles.PROPHET.color()
            );
        }
    }

    private Text getMarkedTargetName(UUID markedTargetUuid) {
        if (markedTargetUuid == null) {
            return Text.translatable("message.noellesroles.prophet.unknown_player");
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return Text.translatable("message.noellesroles.prophet.unknown_player");
        }

        if (markedTargetUuid.equals(client.player.getUuid())) {
            return client.player.getDisplayName();
        }

        PlayerListEntry entry = client.player.networkHandler.getPlayerListEntry(markedTargetUuid);
        if (entry != null) {
            return Text.literal(entry.getProfile().getName());
        }

        return Text.translatable("message.noellesroles.prophet.unknown_player");
    }
}
