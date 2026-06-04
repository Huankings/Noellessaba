package org.agmas.noellesroles.client.mixin.roles.rememberer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 追忆者自定义心情图标与双色渐变条。
 */
@Mixin(MoodRenderer.class)
public abstract class RemembererMoodRenderer {

    @Shadow public static float arrowProgress;
    @Shadow public static float moodOffset;
    @Shadow public static float moodTextWidth;
    @Shadow public static float moodRender;
    @Shadow public static float moodAlpha;
    @Shadow private static float currentWarningProgress;
    @Shadow private static float currentShakeX;
    @Shadow private static float currentShakeY;
    @Shadow private static int getWarningColour(float warningProgress) {
        throw new AssertionError();
    }

    @Unique private static final Identifier REMEMBERER_ARROW_UP = Identifier.of(Noellesroles.MOD_ID, "hud/rememberer_arrow_up");
    @Unique private static final Identifier REMEMBERER_ARROW_DOWN = Identifier.of(Noellesroles.MOD_ID, "hud/rememberer_arrow_down");
    @Unique private static final Identifier REMEMBERER_MOOD_HAPPY = Identifier.of(Noellesroles.MOD_ID, "hud/rememberer_mood_happy");
    @Unique private static final Identifier REMEMBERER_MOOD_MID = Identifier.of(Noellesroles.MOD_ID, "hud/rememberer_mood_mid");
    @Unique private static final Identifier REMEMBERER_MOOD_DEPRESSIVE = Identifier.of(Noellesroles.MOD_ID, "hud/rememberer_mood_depressive");

    @Inject(method = "renderCivilian", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderRemembererMood(
            @NotNull TextRenderer textRenderer,
            @NotNull DrawContext context,
            float prevMood,
            @NotNull CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (!GameWorldComponent.KEY.get(client.player.getWorld()).isRole(client.player, Noellesroles.REMEMBERER)) {
            return;
        }

        ci.cancel();

        context.getMatrices().push();
        context.getMatrices().translate(currentShakeX, currentShakeY, 0.0F);
        context.getMatrices().translate(0.0F, 3.0F * moodOffset, 0.0F);

        Identifier moodTexture = REMEMBERER_MOOD_HAPPY;
        if (moodRender < GameConstants.DEPRESSIVE_MOOD_THRESHOLD) {
            moodTexture = REMEMBERER_MOOD_DEPRESSIVE;
        } else if (moodRender < GameConstants.MID_MOOD_THRESHOLD) {
            moodTexture = REMEMBERER_MOOD_MID;
        }

        if (arrowProgress < 0.1F) {
            if (prevMood >= GameConstants.DEPRESSIVE_MOOD_THRESHOLD && moodRender < GameConstants.DEPRESSIVE_MOOD_THRESHOLD) {
                arrowProgress = -1.0F;
            } else if (prevMood >= GameConstants.MID_MOOD_THRESHOLD && moodRender < GameConstants.MID_MOOD_THRESHOLD) {
                arrowProgress = -1.0F;
            }
        }

        context.drawGuiTexture(moodTexture, 5, 6, 14, 17);
        if (Math.abs(arrowProgress) > 0.01F) {
            boolean up = arrowProgress > 0.0F;
            Identifier arrowTexture = up ? REMEMBERER_ARROW_UP : REMEMBERER_ARROW_DOWN;
            float arrowAlpha = (float) Math.sin(Math.abs(arrowProgress) * Math.PI);
            context.getMatrices().push();
            if (!up) {
                context.getMatrices().translate(0.0F, 4.0F, 0.0F);
            }
            context.getMatrices().translate(0.0F, arrowProgress * 4.0F, 0.0F);
            // 这里直接恢复到 Wathe 原版 civilian mood 的 drawSprite(..., alpha) 路径。
            // 之前为了绕开私有 guiAtlasManager 改成了 drawGuiTexture，
            // 结果虽然箭头能画出来，但丢掉了原本按 alpha 做淡入淡出的那条绘制链，
            // 所以你测试时会看到箭头存在、却没有真正渐隐。
            context.drawSprite(
                    7,
                    6,
                    0,
                    10,
                    13,
                    ((DrawContextAccessor) (Object) context).noellesroles$getGuiAtlasManager().getSprite(arrowTexture),
                    1.0F,
                    1.0F,
                    1.0F,
                    arrowAlpha
            );
            context.getMatrices().pop();
        }
        context.getMatrices().pop();

        context.getMatrices().push();
        context.getMatrices().translate(currentShakeX, currentShakeY, 0.0F);
        context.getMatrices().translate(0.0F, 10.0F * moodOffset, 0.0F);
        context.getMatrices().translate(26.0F, 8.0F + textRenderer.fontHeight, 0.0F);
        context.getMatrices().scale(Math.max(1.0F, moodTextWidth - 8.0F) * moodRender, 1.0F, 1.0F);
        context.fill(0, 0, 1, 1, noellesroles$getRemembererMoodBarColor() | ((int) (moodAlpha * 255.0F) << 24));
        context.getMatrices().pop();

        if (currentWarningProgress > 0.0F) {
            int pulseColour = getWarningColour(currentWarningProgress);
            int warningTextY = 12 + textRenderer.fontHeight;
            context.getMatrices().push();
            context.getMatrices().translate(currentShakeX, currentShakeY, 0.0F);
            context.getMatrices().translate(0.0F, 10.0F * moodOffset, 0.0F);
            context.drawTextWithShadow(
                    textRenderer,
                    Text.translatable("hud.mood.breakdown_warning"),
                    22,
                    warningTextY,
                    pulseColour | ((int) (moodAlpha * 255.0F) << 24)
            );
            context.getMatrices().pop();
        }
    }

    @Unique
    private static int noellesroles$getRemembererMoodBarColor() {
        if (moodRender >= GameConstants.MID_MOOD_THRESHOLD) {
            float delta = (moodRender - GameConstants.MID_MOOD_THRESHOLD) / (1.0F - GameConstants.MID_MOOD_THRESHOLD);
            return noellesroles$lerpColor(RemembererConstants.MOOD_BAR_MID_COLOR, RemembererConstants.MOOD_BAR_HAPPY_COLOR, delta);
        }
        if (moodRender >= GameConstants.DEPRESSIVE_MOOD_THRESHOLD) {
            float delta = (moodRender - GameConstants.DEPRESSIVE_MOOD_THRESHOLD)
                    / (GameConstants.MID_MOOD_THRESHOLD - GameConstants.DEPRESSIVE_MOOD_THRESHOLD);
            return noellesroles$lerpColor(RemembererConstants.MOOD_BAR_DEPRESSIVE_COLOR, RemembererConstants.MOOD_BAR_MID_COLOR, delta);
        }
        return RemembererConstants.MOOD_BAR_DEPRESSIVE_COLOR;
    }

    @Unique
    private static int noellesroles$lerpColor(int from, int to, float delta) {
        delta = MathHelper.clamp(delta, 0.0F, 1.0F);
        int fromR = (from >> 16) & 0xFF;
        int fromG = (from >> 8) & 0xFF;
        int fromB = from & 0xFF;
        int toR = (to >> 16) & 0xFF;
        int toG = (to >> 8) & 0xFF;
        int toB = to & 0xFF;
        int red = MathHelper.floor(MathHelper.lerp(delta, fromR, toR));
        int green = MathHelper.floor(MathHelper.lerp(delta, fromG, toG));
        int blue = MathHelper.floor(MathHelper.lerp(delta, fromB, toB));
        return (red << 16) | (green << 8) | blue;
    }
}
