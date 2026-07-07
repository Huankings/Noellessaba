package org.agmas.noellesroles.client.roles.rememberer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.item.SniperRifleItem;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;
import org.agmas.noellesroles.roles.rememberer.RemembererInteractionHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 追忆者客户端侧的共用状态与判定帮助。
 *
 * <p>这里统一收口三类只和本地显示/手感有关的逻辑：</p>
 * <p>1. “摸取回忆”准星需要复用的目标检测；</p>
 * <p>2. 狙击枪准星需要复用的可视目标检测；</p>
 * <p>3. 狙击枪转向迟缓与惯性的残留状态；</p>
 * <p>4. 狙击枪左键开镜的纯客户端动画进度。</p>
 */
public final class RemembererClientEffects {

    private static double sniperSmoothedLookX = 0.0D;
    private static double sniperSmoothedLookY = 0.0D;
    private static float prevSniperScopeProgress = 0.0F;
    private static float sniperScopeProgress = 0.0F;
    private static float sniperScopeAnimationStartProgress = 0.0F;
    private static int sniperScopeAnimationTicks = 0;
    private static int sniperScopeAnimationDurationTicks = 1;
    private static boolean sniperScopeOpening = false;
    private static boolean sniperScopeAttackPressedLastTick = false;

    private RemembererClientEffects() {
    }

    public static void tick(MinecraftClient client) {
        tickSniperScope(client);
        tickSniperAimReset(client);
    }

    public static void reset() {
        resetSniperAimInertia();
        prevSniperScopeProgress = 0.0F;
        sniperScopeProgress = 0.0F;
        sniperScopeAnimationStartProgress = 0.0F;
        sniperScopeAnimationTicks = 0;
        sniperScopeAnimationDurationTicks = 1;
        sniperScopeOpening = false;
        sniperScopeAttackPressedLastTick = false;
    }

    public static boolean isRememberer(@Nullable PlayerEntity player) {
        return player != null && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.REMEMBERER);
    }

    public static boolean shouldRenderRemembererHud(@Nullable PlayerEntity player) {
        return player != null && isRememberer(player) && GameFunctions.isPlayerAliveAndSurvival(player);
    }

    public static boolean shouldRenderRemembererCrosshair(@Nullable PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return shouldRenderRemembererHud(player)
                && client.options.getPerspective().isFirstPerson()
                && player != null
                && player.getMainHandStack().isEmpty();
    }

    /**
     * 追忆者的专用准星只在“真的对准了一个可摸取的目标玩家”时才接管原版准星渲染。
     *
     * <p>这样做有两个好处：
     * 1. 和小偷那类近距离交互职业的交互反馈更一致；
     * 2. 玩家在空手乱看时，不会一直看到一个具有误导性的“可交互图标”。</p>
     */
    public static boolean shouldShowRecallCrosshair(@Nullable PlayerEntity player) {
        return player != null
                && shouldRenderRemembererCrosshair(player)
                && getRecallTarget(player) != null;
    }

    public static boolean canRecallNow(@Nullable PlayerEntity player) {
        return shouldShowRecallCrosshair(player)
                && player != null
                && AbilityPlayerComponent.KEY.get(player).cooldown <= 0;
    }

    public static int getRecallCooldownTotalTicks(@NotNull PlayerEntity player) {
        return RemembererPlayerComponent.KEY.get(player).isUsingAbilityStartCooldown()
                ? RemembererConstants.RECALL_START_COOLDOWN_TICKS
                : RemembererConstants.RECALL_COOLDOWN_TICKS;
    }

    public static float getRecallCooldownProgress(@NotNull PlayerEntity player, float tickDelta) {
        int totalTicks = getRecallCooldownTotalTicks(player);
        int remainingTicks = AbilityPlayerComponent.KEY.get(player).cooldown;
        if (totalTicks <= 0) {
            return 1.0F;
        }
        return MathHelper.clamp((totalTicks - Math.max(0.0F, remainingTicks - tickDelta)) / totalTicks, 0.0F, 1.0F);
    }

    public static @Nullable PlayerEntity getRecallTarget(@NotNull PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.crosshairTarget instanceof EntityHitResult entityHitResult)) {
            return null;
        }
        if (!(entityHitResult.getEntity() instanceof PlayerEntity target)) {
            return null;
        }
        return RemembererInteractionHandler.isRecallTargetEntity(player, target) ? target : null;
    }

    public static boolean shouldRenderSniperCrosshair(@Nullable PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return player != null
                && client.options.getPerspective().isFirstPerson()
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && player.getMainHandStack().isOf(ModItems.SNIPER_RIFLE);
    }

    public static boolean hasVisibleSniperTarget(@NotNull PlayerEntity player) {
        return SniperRifleItem.getVisibleTarget(player) instanceof EntityHitResult;
    }

    public static boolean isSniperScopeVisible() {
        return prevSniperScopeProgress > 0.0F || sniperScopeProgress > 0.0F;
    }

    /**
     * 取得当前帧用于渲染的开镜进度。
     *
     * <p>客户端逻辑每 tick 只更新一次进度，渲染却可能一秒跑很多帧；
     * 这里用上一 tick 和当前 tick 的进度做插值，让 FOV 与遮罩放大过程都保持顺滑。</p>
     */
    public static float getSniperScopeProgress(float tickDelta) {
        return MathHelper.clamp(
                MathHelper.lerp(tickDelta, prevSniperScopeProgress, sniperScopeProgress),
                0.0F,
                1.0F
        );
    }

    /**
     * 取得套在原始 FOV 上的狙击镜倍率。
     *
     * <p>倍率从 1.0 平滑过渡到望远镜同款 0.1，
     * 所以不会在按下左键的瞬间突然跳变，松开左键时也会自然退回。</p>
     */
    public static float getSniperScopeFovMultiplier(float tickDelta) {
        return MathHelper.lerp(getSniperScopeProgress(tickDelta), 1.0F, RemembererConstants.SNIPER_SCOPE_FOV_MULTIPLIER);
    }

    public static boolean shouldApplySniperAim(@Nullable PlayerEntity player) {
        return player instanceof ClientPlayerEntity clientPlayer
                && MinecraftClient.getInstance().player == clientPlayer
                && shouldRenderSniperCrosshair(clientPlayer);
    }

    /**
     * 包装 changeLookDirection 时，把原始鼠标输入喂给一个“有损低通”模型。
     *
     * <p>这里和上一版最大的区别在于：不再把“损失掉的输入”通过残量慢慢补回原速。
     * 旧算法在持续转头时会渐渐接近正常速度，所以玩家看起来像“根本没减速”。
     *
     * <p>现在改成：
     * 1. 先把输入目标速度压到原来的 50%；
     * 2. 再保留上一帧 65% 的输出做平滑；
     * 3. 最终形成“起手慢、停手也慢一点、而且整体转速确实变慢”的重手感。</p>
     */
    public static double[] transformSniperLookInput(double cursorDeltaX, double cursorDeltaY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!shouldApplySniperAim(client.player)) {
            resetSniperAimInertia();
            return new double[]{cursorDeltaX, cursorDeltaY};
        }

        double targetX = cursorDeltaX * RemembererConstants.SNIPER_AIM_INPUT_SCALE;
        double targetY = cursorDeltaY * RemembererConstants.SNIPER_AIM_INPUT_SCALE;

        sniperSmoothedLookX = sniperSmoothedLookX * RemembererConstants.SNIPER_AIM_INERTIA_DAMPING
                + targetX * (1.0D - RemembererConstants.SNIPER_AIM_INERTIA_DAMPING);
        sniperSmoothedLookY = sniperSmoothedLookY * RemembererConstants.SNIPER_AIM_INERTIA_DAMPING
                + targetY * (1.0D - RemembererConstants.SNIPER_AIM_INERTIA_DAMPING);
        trimResidualLook();
        return new double[]{sniperSmoothedLookX, sniperSmoothedLookY};
    }

    /**
     * 低通平滑值只要一离开“本地玩家第一人称持枪瞄准”场景，就应该立刻清零。
     *
     * <p>否则玩家放下狙击枪、切菜单、或切出第一人称后再回来时，
     * 旧的平滑残量还会残留在内存里，下一次重新瞄准会突然自己轻微偏转。</p>
     */
    private static void tickSniperAimReset(MinecraftClient client) {
        if (!shouldApplySniperAim(client.player)) {
            resetSniperAimInertia();
        }
    }

    private static void resetSniperAimInertia() {
        sniperSmoothedLookX = 0.0D;
        sniperSmoothedLookY = 0.0D;
    }

    /**
     * 更新左键开镜进度。
     *
     * <p>开镜是纯客户端视觉效果，不检查狙击枪冷却和弹药；
     * 只要本地玩家第一人称手持狙击枪并按住攻击键，就可以开始放大。
     * 如果只是松开左键，则单独启动一次“收镜”动画；如果切物品、切视角、死亡或打开界面，
     * 说明当前画面已经不适合继续保留狙击镜，直接清零避免黑屏残留。</p>
     */
    private static void tickSniperScope(MinecraftClient client) {
        if (!canKeepSniperScopeScene(client)) {
            resetSniperScope();
            return;
        }

        prevSniperScopeProgress = sniperScopeProgress;
        boolean attackPressed = client.options.attackKey.isPressed();
        if (attackPressed != sniperScopeAttackPressedLastTick) {
            startSniperScopeAnimation(attackPressed);
            sniperScopeAttackPressedLastTick = attackPressed;
        }

        updateSniperScopeAnimation();
    }

    private static boolean canKeepSniperScopeScene(@NotNull MinecraftClient client) {
        return client.currentScreen == null && shouldRenderSniperCrosshair(client.player);
    }

    /**
     * 启动一次开镜或收镜动画。
     *
     * <p>这里记录“动作开始时的当前进度”，而不是强制从 0 或 1 重新开始。
     * 这样玩家快速点按左键、半路反向收镜时，动画会从当前画面继续过渡，不会突然跳帧。</p>
     */
    private static void startSniperScopeAnimation(boolean opening) {
        sniperScopeOpening = opening;
        sniperScopeAnimationStartProgress = sniperScopeProgress;
        sniperScopeAnimationTicks = 0;
        sniperScopeAnimationDurationTicks = Math.max(
                1,
                secondsToTicks(opening
                        ? RemembererConstants.SNIPER_SCOPE_OPEN_ANIMATION_SECONDS
                        : RemembererConstants.SNIPER_SCOPE_CLOSE_ANIMATION_SECONDS)
        );
    }

    /**
     * 根据当前动画方向推进开镜进度。
     *
     * <p>每次“开镜”和“收镜”都从 0 到 1 走一次 ease-out。
     * 因为收镜不是简单把开镜曲线倒放，所以收镜同样会呈现“先快后慢”，
     * 不会变成先慢后快。</p>
     */
    private static void updateSniperScopeAnimation() {
        if (sniperScopeOpening && sniperScopeProgress >= 1.0F) {
            sniperScopeProgress = 1.0F;
            return;
        }
        if (!sniperScopeOpening && sniperScopeProgress <= 0.0F) {
            sniperScopeProgress = 0.0F;
            return;
        }

        sniperScopeAnimationTicks++;
        float animationProgress = MathHelper.clamp(
                sniperScopeAnimationTicks / (float) sniperScopeAnimationDurationTicks,
                0.0F,
                1.0F
        );
        float easedProgress = easeOutSniperScopeProgress(animationProgress);
        float targetProgress = sniperScopeOpening ? 1.0F : 0.0F;
        sniperScopeProgress = MathHelper.lerp(easedProgress, sniperScopeAnimationStartProgress, targetProgress);
        /*
         * 收镜最后几百分之一的进度在视觉上几乎没有意义，
         * 但因为 HUD/FOV 渲染会继续用 prevSniperScopeProgress 插值，
         * 这一点残留会让玩家感觉“已经收完了，却还等了一下才恢复普通准心”。
         * 所以只在收镜接近 0 时立即清空当前帧和上一帧进度，让普通状态马上接管。
         */
        if (!sniperScopeOpening && sniperScopeProgress <= RemembererConstants.SNIPER_SCOPE_CLOSE_FINISH_PROGRESS) {
            resetSniperScope();
        }
    }

    private static void resetSniperScope() {
        prevSniperScopeProgress = 0.0F;
        sniperScopeProgress = 0.0F;
        sniperScopeAnimationStartProgress = 0.0F;
        sniperScopeAnimationTicks = 0;
        sniperScopeAnimationDurationTicks = 1;
        sniperScopeOpening = false;
        sniperScopeAttackPressedLastTick = false;
    }

    private static int secondsToTicks(float seconds) {
        return Math.round(Math.max(0.0F, seconds) * 20.0F);
    }

    /**
     * ease-out cubic 缓动：开始时变化最快，越接近终点越慢。
     *
     * <p>开镜时视野会更快进入放大状态，最后轻轻贴到目标倍率；
     * 收镜时也会快速恢复大部分视野，再慢慢回到正常画面。</p>
     */
    public static float easeOutSniperScopeProgress(float progress) {
        float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
        float inverse = 1.0F - clamped;
        return 1.0F - inverse * inverse * inverse;
    }

    private static void trimResidualLook() {
        if (Math.abs(sniperSmoothedLookX) <= RemembererConstants.SNIPER_AIM_EPSILON) {
            sniperSmoothedLookX = 0.0D;
        }
        if (Math.abs(sniperSmoothedLookY) <= RemembererConstants.SNIPER_AIM_EPSILON) {
            sniperSmoothedLookY = 0.0D;
        }
    }
}
