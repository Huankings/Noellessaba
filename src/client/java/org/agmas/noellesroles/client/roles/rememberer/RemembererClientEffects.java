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
 * <p>3. 狙击枪转向迟缓与惯性的残留状态。</p>
 */
public final class RemembererClientEffects {

    private static double sniperSmoothedLookX = 0.0D;
    private static double sniperSmoothedLookY = 0.0D;

    private RemembererClientEffects() {
    }

    public static void tick(MinecraftClient client) {
        tickSniperAimReset(client);
    }

    public static void reset() {
        sniperSmoothedLookX = 0.0D;
        sniperSmoothedLookY = 0.0D;
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
            reset();
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
            reset();
        }
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
