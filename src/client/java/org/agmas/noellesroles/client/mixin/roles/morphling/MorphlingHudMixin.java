package org.agmas.noellesroles.client.mixin.roles.morphling;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 变形怪右下角 HUD。
 *
 * <p>显示逻辑和你之前要求的幻灵 HUD 一样放在右下角，
 * 但这里根据 morphTicks 的正负来区分：
 * 1. 0 = 就绪；
 * 2. 正数 = 正在变形；
 * 3. 负数 = 冷却中。</p>
 */
@Mixin(InGameHud.class)
public abstract class MorphlingHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void noellesroles$renderMorphlingHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.MORPHLING)) {
            return;
        }

        MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(client.player);
        Text line;
        if (morphComp.getMorphTicks() > 0) {
            line = Text.translatable("hud.noellesroles.morphling.active", Math.max(1, (int) Math.ceil(morphComp.getMorphTicks() / 20.0)));
        } else if (morphComp.getMorphTicks() < 0) {
            line = Text.translatable("hud.noellesroles.morphling.cooldown", Math.max(1, (int) Math.ceil((-morphComp.getMorphTicks()) / 20.0)));
        } else {
            line = Text.translatable("hud.noellesroles.morphling.ready");
        }

        int drawY = context.getScaledWindowHeight() - getTextRenderer().getWrappedLinesHeight(line, 999999);
        context.drawTextWithShadow(
                getTextRenderer(),
                line,
                context.getScaledWindowWidth() - getTextRenderer().getWidth(line),
                drawY,
                Noellesroles.MORPHLING.color()
        );
    }
}
