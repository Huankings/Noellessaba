package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

/** 悦灵选择、复核和重新锁定玩家目标的统一判定。 */
public final class ThiefAllayTargeting {
    private ThiefAllayTargeting() {
    }

    /**
     * 正常存活目标除了 Wathe 的存活定义外，还必须显式排除时间狭缝。
     * 时间狭缝会通过 PlayerLifeStateApi 让旁观玩家暂时继续被 isPlayerAliveAndSurvival 视为存活，
     * 但本需求明确要求悦灵把时间狭缝玩家当作已死亡目标。
     */
    public static boolean isValidTarget(@Nullable PlayerEntity target, @NotNull UUID ownerUuid) {
        return target != null
                && !target.isRemoved()
                && !ownerUuid.equals(target.getUuid())
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && !GameFunctions.isPlayerSpectatingOrCreative(target)
                && !(target instanceof ServerPlayerEntity serverTarget
                && TimekeeperPlayerComponent.KEY.get(serverTarget).isInTimeRift());
    }

    /** 小偷主人死亡、掉线或进入时间狭缝后，所属悦灵必须结束并掉落手持物。 */
    public static boolean isValidOwner(@Nullable ServerPlayerEntity owner) {
        return owner != null
                && !owner.isRemoved()
                && GameFunctions.isPlayerAliveAndSurvival(owner)
                && !GameFunctions.isPlayerSpectatingOrCreative(owner)
                && !TimekeeperPlayerComponent.KEY.get(owner).isInTimeRift();
    }

    /** 从悦灵当前所在世界选择距离最近的合法玩家；不同悦灵可因位置不同而选中不同目标。 */
    public static @Nullable ServerPlayerEntity findNearestTarget(
            @NotNull ThiefAllayEntity allay,
            @NotNull ServerWorld world
    ) {
        UUID ownerUuid = allay.getOwnerUuid();
        return world.getPlayers(player -> isValidTarget(player, ownerUuid)).stream()
                .min(Comparator.comparingDouble(allay::squaredDistanceTo))
                .orElse(null);
    }
}
