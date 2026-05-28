package org.agmas.noellesroles.client.mixin.roles.executioner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.client.gui.MoodRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(MoodRenderer.class)
public class ExecutionerMoodRenderer {

    @Shadow public static float moodOffset;

    @Shadow public static float moodTextWidth;

    @Shadow public static float moodRender;

    @Shadow public static float moodAlpha;
    @Shadow public static Random random;
    @Unique private static final Identifier EXECUTIONER_MOOD = Identifier.of(Noellesroles.MOD_ID, "hud/mood_executioner");

    @Inject(method = "renderKiller", at = @At("HEAD"), cancellable = true)
    private static void executionerMood(TextRenderer textRenderer, DrawContext context, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(MinecraftClient.getInstance().player);
        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.EXECUTIONER)) {
        /*
         * 这里继续保留对原版 killer mood 的接管，但把“图标”和“进度条”拆开处理。
         *
         * 你这次测试反馈得很准确：
         * 上一版修复把“没有任务时直接 return”放得太早了，导致仇杀客在无任务阶段
         * 不只是下方心情值条消失，连左侧自定义 mood 图标也一起被跳过了。
         *
         * 这次改成更符合需求的行为：
         * 1. 先 cancel 原版渲染，避免退回默认杀手 mood 图标；
         * 2. 无论有没有任务，都始终绘制仇杀客自己的 mood 图标；
         * 3. 只有在 tasks 不为空时，才继续绘制下面那条心情值进度条。
         *
         * 这样最终效果就是：
         * - 没刷任务时：只保留图标，不显示进度条；
         * - 刷出任务后：图标和进度条一起显示。
         */
        ci.cancel();
        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 3.0F * moodOffset, 0.0F);
        context.drawGuiTexture(EXECUTIONER_MOOD, 5, 6, 14, 17);
        context.getMatrices().pop();

        /*
         * 这里专门控制“下方心情值进度条”的显示时机。
         * 如果当前没有任何挂着的任务，就不要再绘制这条横向进度条，
         * 但上面已经画好的仇杀客图标会继续保留在 HUD 上。
         */
        if (moodComponent.tasks.isEmpty()) {
            return;
        }

        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 10.0F * moodOffset, 0.0F);
        MatrixStack var10000 = context.getMatrices();
        var10000.translate(26.0F, (float)(8 + 9), 0.0F);
        context.getMatrices().scale((moodTextWidth - 8.0F) * moodRender, 1.0F, 1.0F);
        context.fill(0, 0, 1, 1, Noellesroles.EXECUTIONER.color() | (int)(moodAlpha * 255.0F) << 24);
        context.getMatrices().pop();
        }
    }
}
