package org.agmas.noellesroles.roles.spring_trap;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Wathe 门普通开关声的调用上下文。
 *
 * <p>DoorBlockEntity#toggle(boolean) 自身没有“是谁开的门”参数。
 * 因此在 SmallDoorBlock / TrainDoorBlock 的 onUse 调用栈里临时记录本次交互玩家，
 * World#playSound 播放门声时再读取这个上下文，判断是否应该因为弹簧陷阱状态而静音。</p>
 */
public final class SpringTrapDoorSoundContext {
    private static final ThreadLocal<PlayerEntity> CURRENT_OPENER = new ThreadLocal<>();

    private SpringTrapDoorSoundContext() {
    }

    public static void begin(@Nullable PlayerEntity player) {
        if (player == null) {
            CURRENT_OPENER.remove();
        } else {
            CURRENT_OPENER.set(player);
        }
    }

    public static void end() {
        CURRENT_OPENER.remove();
    }

    public static boolean shouldSuppressForCurrentOpener(World world) {
        PlayerEntity player = CURRENT_OPENER.get();
        return player != null
                && player.getWorld() == world
                && SpringTrapSoundRules.shouldSuppressSounds(player);
    }
}
