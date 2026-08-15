package org.agmas.noellesroles.client.roles.jason;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.roles.jason.JasonAbilityPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 杰森无恶不在的本地第一人称手臂动画。
 *
 * <p>这只负责客户端视觉：服务端仍然通过输入锁和交互回调禁止左右键。
 * 动画进度直接读同步来的 jason_ability 组件，进入 / 退出时长严格使用对应常量，
 * 因此手臂完全收起、完全回来的时间会和雾效过渡时间保持一致。</p>
 */
public final class JasonAbilityArmAnimator {
    private JasonAbilityArmAnimator() {
    }

    public static boolean shouldAnimate(@NotNull ClientPlayerEntity player) {
        /*
         * 输入限制和隐藏都只对 Wathe 存活玩家生效。
         * 非存活 / 创造 / 旁观调试视角不会因为组件残留而把第一人称手臂收走。
         */
        return GameFunctions.isPlayerAliveAndSurvival(player)
                && JasonAbilityPlayerComponent.KEY.get(player).isActiveLike();
    }

    public static float getArmVisibility(@NotNull ClientPlayerEntity player, float tickDelta) {
        JasonAbilityPlayerComponent component = JasonAbilityPlayerComponent.KEY.get(player);
        if (component.isEntering()) {
            return 1.0F - progress(component.getPhaseTicks(), tickDelta, JasonConstants.ABILITY_ENTER_TICKS);
        }
        if (component.isFullyActive()) {
            return 0.0F;
        }
        if (component.isExiting()) {
            return progress(component.getPhaseTicks(), tickDelta, JasonConstants.ABILITY_EXIT_TICKS);
        }
        return 1.0F;
    }

    public static void applyFirstPersonTransform(@NotNull MatrixStack matrices, float visibility) {
        float hidden = 1.0F - MathHelper.clamp(visibility, 0.0F, 1.0F);
        if (hidden <= 0.0F) {
            return;
        }

        /*
         * smoothstep 让手臂起步和收尾都更柔和，但 0 和 1 的端点不变，
         * 所以不会改变用户指定的进入 / 退出总时长。
         */
        float easedHidden = hidden * hidden * (3.0F - 2.0F * hidden);
        float scale = MathHelper.lerp(easedHidden, 1.0F, JasonConstants.ABILITY_ARM_HIDDEN_SCALE);

        /*
         * 先把整段第一人称手持渲染推向屏幕下方和镜头远处，再做整体缩放。
         * 这样空手、持物、双手地图等都跟着同一条手臂动画离开画面，而不是只把皮肤手臂单独缩掉。
         */
        matrices.translate(
                0.0D,
                JasonConstants.ABILITY_ARM_HIDE_TRANSLATE_Y * easedHidden,
                JasonConstants.ABILITY_ARM_HIDE_TRANSLATE_Z * easedHidden
        );
        matrices.scale(scale, scale, scale);
    }

    private static float progress(int phaseTicks, float tickDelta, int durationTicks) {
        if (durationTicks <= 0) {
            return 1.0F;
        }
        return MathHelper.clamp((phaseTicks + tickDelta) / (float) durationTicks, 0.0F, 1.0F);
    }
}
