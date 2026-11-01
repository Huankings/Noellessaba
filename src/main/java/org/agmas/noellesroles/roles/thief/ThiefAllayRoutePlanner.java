package org.agmas.noellesroles.roles.thief;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * 小偷悦灵的分层路径规划器。
 *
 * <p>原版 {@link net.minecraft.entity.ai.pathing.BirdNavigation} 在目标隔着地板或天花板时，
 * 可能只返回“尽量靠近目标”的不完整路径。若每次目标移动都直接接受这种路径，悦灵就会沿着
 * 地板或墙面追逐目标投影，而不会保留一条通往楼梯、门洞或走廊的长路径。</p>
 *
 * <p>这里按三层处理：</p>
 * <p>1. 先尝试目标实体本身的完整路径；</p>
 * <p>2. 如果实体脚下节点不可达，一次性搜索目标周围的可飞行落点；</p>
 * <p>3. 长时间无进展时，抽取可达的中间绕行锚点，让悦灵先离开当前死角再重新锁定最终目标。</p>
 */
public final class ThiefAllayRoutePlanner {
    private ThiefAllayRoutePlanner() {
    }

    /** 规划最终目标路线；没有完整路线时仍返回原版不完整路径作为短期 fallback。 */
    public static @Nullable PlannedRoute planDestination(
            @NotNull ThiefAllayEntity allay,
            @NotNull Entity destination
    ) {
        ThiefAllayNavigation navigation = (ThiefAllayNavigation) allay.getNavigation();
        Path directPath = navigation.findExtendedPathTo(destination, ThiefConstants.ALLAY_DIRECT_PATH_ACCURACY);
        if (directPath != null && directPath.reachesTarget()) {
            return new PlannedRoute(directPath, directPath.getTarget(), true);
        }

        Path approachPath = findTargetApproachPath(allay, destination);
        if (approachPath != null && approachPath.reachesTarget()) {
            return new PlannedRoute(approachPath, approachPath.getTarget(), true);
        }

        return directPath == null
                ? null
                : new PlannedRoute(directPath, directPath.getTarget(), false);
    }

    /**
     * 为卡住的悦灵寻找一个真正可达的中间点。
     * 首先再次尝试目标附近落点；失败后用原版无惩罚随机目标工具搜索朝向目标、但允许大角度偏转的位置，
     * 因而可以暂时远离目标投影去寻找楼梯或绕过墙角。
     */
    public static @Nullable PlannedRoute planRecovery(
            @NotNull ThiefAllayEntity allay,
            @NotNull Entity destination,
            @Nullable BlockPos previousWaypoint
    ) {
        Path approachPath = findTargetApproachPath(allay, destination);
        if (approachPath != null && approachPath.reachesTarget()) {
            return new PlannedRoute(approachPath, approachPath.getTarget(), true);
        }

        PlannedRoute towardRoute = findRandomRoute(
                allay,
                destination,
                previousWaypoint,
                true,
                ThiefConstants.ALLAY_RECOVERY_TOWARD_ATTEMPTS
        );
        if (towardRoute != null) {
            return towardRoute;
        }

        return findRandomRoute(
                allay,
                destination,
                previousWaypoint,
                false,
                ThiefConstants.ALLAY_RECOVERY_ESCAPE_ATTEMPTS
        );
    }

    private static @Nullable Path findTargetApproachPath(
            @NotNull ThiefAllayEntity allay,
            @NotNull Entity destination
    ) {
        Set<BlockPos> candidates = new HashSet<>();
        BlockPos targetPos = destination.getBlockPos();
        int radius = ThiefConstants.ALLAY_TARGET_APPROACH_HORIZONTAL_RADIUS;

        for (int yOffset = -ThiefConstants.ALLAY_TARGET_APPROACH_VERTICAL_BELOW;
             yOffset <= ThiefConstants.ALLAY_TARGET_APPROACH_VERTICAL_ABOVE;
             yOffset++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    /* 只取每一圈的边界和目标附近一圈，减少候选数量同时保留各个绕入方向。 */
                    int horizontalRadius = Math.max(Math.abs(xOffset), Math.abs(zOffset));
                    if (horizontalRadius > 1 && horizontalRadius != radius) {
                        continue;
                    }
                    BlockPos candidate = targetPos.add(xOffset, yOffset, zOffset);
                    if (isUsableFlightPosition(allay, candidate)
                            && hasClearApproachLine(allay, candidate, destination)) {
                        candidates.add(candidate);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }
        return ((ThiefAllayNavigation) allay.getNavigation()).findExtendedPathTo(
                candidates,
                ThiefConstants.ALLAY_RECOVERY_PATH_ACCURACY
        );
    }

    private static @Nullable PlannedRoute findRandomRoute(
            @NotNull ThiefAllayEntity allay,
            @NotNull Entity destination,
            @Nullable BlockPos previousWaypoint,
            boolean towardDestination,
            int attempts
    ) {
        Set<BlockPos> candidates = new HashSet<>();

        for (int attempt = 0; attempt < attempts; attempt++) {
            Vec3d candidateVec = towardDestination
                    ? NoPenaltyTargeting.findTo(
                    allay,
                    ThiefConstants.ALLAY_RECOVERY_HORIZONTAL_RANGE,
                    ThiefConstants.ALLAY_RECOVERY_VERTICAL_RANGE,
                    destination.getPos(),
                    ThiefConstants.ALLAY_RECOVERY_MAX_ANGLE_RADIANS
            )
                    : NoPenaltyTargeting.find(
                    allay,
                    ThiefConstants.ALLAY_RECOVERY_HORIZONTAL_RANGE,
                    ThiefConstants.ALLAY_RECOVERY_VERTICAL_RANGE
            );
            if (candidateVec == null
                    || candidateVec.squaredDistanceTo(allay.getPos())
                    < ThiefConstants.ALLAY_RECOVERY_MIN_WAYPOINT_DISTANCE_SQUARED) {
                continue;
            }

            BlockPos candidate = BlockPos.ofFloored(candidateVec);
            if ((previousWaypoint != null && previousWaypoint.getSquaredDistance(candidate) <= 1.0D)
                    || !isUsableFlightPosition(allay, candidate)) {
                continue;
            }
            candidates.add(candidate);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        /*
         * 所有候选一次性交给 PathNodeNavigator，避免对每个随机点分别运行一次32格路径搜索。
         * 导航器会选出其中实际可达且路径成本最低的目标，最多只产生一轮昂贵寻路。
         */
        Path path = ((ThiefAllayNavigation) allay.getNavigation()).findExtendedPathTo(
                candidates,
                ThiefConstants.ALLAY_RECOVERY_PATH_ACCURACY
        );
        return path != null && path.reachesTarget() && path.getLength() > 1
                ? new PlannedRoute(path, path.getTarget(), false)
                : null;
    }

    private static boolean isUsableFlightPosition(@NotNull ThiefAllayEntity allay, @NotNull BlockPos pos) {
        if (pos.getY() <= allay.getWorld().getBottomY() || pos.getY() >= allay.getWorld().getTopY()) {
            return false;
        }
        Vec3d candidateCenter = Vec3d.ofCenter(pos);
        Box candidateBox = allay.getBoundingBox().offset(candidateCenter.subtract(allay.getBoundingBox().getCenter()));
        return allay.getWorld().isSpaceEmpty(allay, candidateBox);
    }

    /**
     * 目标附近落点必须能直接看见目标实体中心。
     * 这会排除“坐标很近但仍隔着楼板”的上层节点，避免把贴着天花板的位置误认成已找到完整路线。
     */
    private static boolean hasClearApproachLine(
            @NotNull ThiefAllayEntity allay,
            @NotNull BlockPos candidate,
            @NotNull Entity destination
    ) {
        Vec3d start = Vec3d.ofCenter(candidate);
        Vec3d end = destination.getBoundingBox().getCenter();
        if (start.squaredDistanceTo(end) <= ThiefConstants.ALLAY_TARGET_DISTANCE_EPSILON) {
            return true;
        }
        HitResult hit = allay.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                allay
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    public record PlannedRoute(@NotNull Path path, @NotNull BlockPos waypoint, boolean reachesDestinationArea) {
    }
}
