package org.agmas.noellesroles.client.mixin.roles.jester;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Random;

@Mixin(MoodRenderer.class)
public class JesterMoodRenderer {

    @Shadow public static float moodOffset;

    @Shadow public static float moodTextWidth;

    @Shadow public static float moodRender;

    @Shadow public static float moodAlpha;
    @Shadow public static Random random;
    @Unique private static final Identifier JESTER_MOOD = Identifier.of(Noellesroles.MOD_ID, "hud/mood_jester");

    @Inject(method = "renderKiller", at = @At("HEAD"), cancellable = true)
    private static void jesterMood(TextRenderer textRenderer, DrawContext context, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(MinecraftClient.getInstance().player);
        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.JESTER)) {
        /*
         * 狂信者这里和仇杀客一样，也需要把“图标显示”和“进度条显示”分离开。
         *
         * 上一版虽然修掉了“任务做完后进度条还挂着”的问题，
         * 但因为 tasks 为空时过早 return，导致狂信者自己的自定义 mood 图标也一起不见了。
         *
         * 这次改成下面这套逻辑：
         * 1. 先 cancel 原版 killer mood，保证不会回退成默认杀手图标；
         * 2. 不管有没有任务，都始终绘制狂信者的自定义 mood 图标；
         * 3. 仅当 tasks 里还有任务时，才绘制下方心情值进度条。
         *
         * 这样就能和你的需求完全一致：
         * - 无任务阶段只留图标；
         * - 有任务阶段图标和进度条都显示。
         */
        ci.cancel();
        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 3.0F * moodOffset, 0.0F);
        context.drawGuiTexture(JESTER_MOOD, 5, 6, 14, 17);
        context.getMatrices().pop();

        /*
         * 这里单独拦住“下方横条”的绘制。
         * 任务列表为空时，说明现在只是暂时没有刷新出新任务，
         * 所以下方进度条应隐藏，但左侧图标继续保留。
         */
        if (moodComponent.tasks.isEmpty()) {
            return;
        }

        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 10.0F * moodOffset, 0.0F);
        MatrixStack var10000 = context.getMatrices();
        var10000.translate(26.0F, (float)(8 + 9), 0.0F);
        context.getMatrices().scale((moodTextWidth - 8.0F) * moodRender, 1.0F, 1.0F);
        context.fill(0, 0, 1, 1, Noellesroles.JESTER.color() | (int)(moodAlpha * 255.0F) << 24);
        context.getMatrices().pop();
        }
    }
}
