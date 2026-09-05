package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * 杰森“无恶不在”的通用状态判断。
 *
 * <p>服务端管理器、客户端渲染、声音静音和方块碰撞 mixin 都需要判断“这个玩家是否真的是局内存活杰森，
 * 并且是否处于无恶不在阶段”。这些判断集中到这里，可以避免某个入口只看组件残留，
 * 导致上一局或调试状态的非杰森玩家被错误隐藏、加速或穿门。</p>
 */
public final class JasonAbilityRules {
    private JasonAbilityRules() {
    }

    /**
     * 判断玩家是否是 Wathe 定义的局内存活杰森。
     *
     * <p>这里使用 {@link GameFunctions#isPlayerAliveAndSurvival(PlayerEntity)}，
     * 是为了贯彻用户要求：限制类机制只对局内存活玩家生效，死亡旁观或创造调试视角不应被组件残留影响。</p>
     */
    public static boolean isAliveJason(@Nullable PlayerEntity player) {
        return player != null
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.JASON);
    }

    /**
     * 判断玩家是否处于无恶不在的任意可感知阶段。
     *
     * <p>ENTERING / ACTIVE / EXITING 都算 active-like：
     * 进入过渡开始后存活玩家就不应再看到杰森；退出过渡结束前也仍应保留幽魂限制。</p>
     */
    public static boolean isAbilityActiveLike(@Nullable PlayerEntity player) {
        return isAliveJason(player) && JasonAbilityPlayerComponent.KEY.get(player).isActiveLike();
    }

    /**
     * 判断玩家是否已经完成进入过渡，处于完整幽魂状态。
     *
     * <p>主要给“7 秒后才允许主动解除”和少量客户端显示使用；大多数限制应使用 active-like。</p>
     */
    public static boolean isAbilityFullyActive(@Nullable PlayerEntity player) {
        return isAliveJason(player) && JasonAbilityPlayerComponent.KEY.get(player).isFullyActive();
    }

    /**
     * 判断杰森是否应被锁住左键、右键和背包入口。
     *
     * <p>用户补充确认：无恶不在只锁鼠标左右键和背包 E 键，不锁 G 键或其它键。
     * 具体“锁哪些键”由客户端输入类处理；这里仅回答“该玩家是否进入了需要锁动作的阶段”。</p>
     */
    public static boolean isAbilityActionLocked(@Nullable PlayerEntity player) {
        return isAbilityActiveLike(player);
    }

    public static boolean hasActiveAbilityInWorld(@Nullable net.minecraft.world.World world) {
        if (world == null) {
            return false;
        }
        for (PlayerEntity player : world.getPlayers()) {
            if (isAbilityActiveLike(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算无恶不在对环境语音 / 雾效这类“强度型表现”的平滑进度。
     *
     * <p>ENTERING 阶段从 0 平滑到 1，ACTIVE 阶段保持 1，EXITING 阶段再从 1 平滑退回 0。
     * 语音衰减、初始音量和传播距离都读取这个值做插值，避免发动或解除能力时突然从正常语音跳到压制语音。</p>
     */
    public static float getAbilityTransitionProgress(@Nullable PlayerEntity player) {
        if (!isAliveJason(player)) {
            return 0.0F;
        }

        JasonAbilityPlayerComponent component = JasonAbilityPlayerComponent.KEY.get(player);
        return switch (component.getPhase()) {
            case ENTERING -> safeProgress(component.getPhaseTicks(), JasonConstants.ABILITY_ENTER_TICKS);
            case ACTIVE -> 1.0F;
            case EXITING -> 1.0F - safeProgress(component.getPhaseTicks(), JasonConstants.ABILITY_EXIT_TICKS);
            case IDLE -> 0.0F;
        };
    }

    private static float safeProgress(int elapsedTicks, int totalTicks) {
        if (totalTicks <= 0) {
            return 1.0F;
        }
        return clamp01(elapsedTicks / (float) totalTicks);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
