package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.block.DoorPartBlock;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.jetbrains.annotations.Nullable;

/** 迈尔斯的服务端视线检测。Wathe 门可以穿透，普通实心方块仍会阻挡。 */
public final class MyersTargeting {
    private MyersTargeting() {
    }

    /**
     * 查找释放瞬间准心对准的真实存活玩家。
     * 客户端只用于表现，真正目标必须在服务端再次通过该方法确认。
     */
    public static @Nullable PlayerEntity findTarget(PlayerEntity viewer, double range) {
        Vec3d start = viewer.getEyePos();
        Vec3d direction = viewer.getRotationVector().normalize();
        PlayerEntity best = null;
        double bestDistance = range + 1.0D;

        for (PlayerEntity target : viewer.getWorld().getPlayers()) {
            if (target == viewer
                    || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || TimekeeperPlayerComponent.KEY.get(target).isInTimeRift()
                    || !TargetVisibilityApi.canAttackPlayer(viewer, target)) {
                continue;
            }

            Vec3d targetPoint = target.getEyePos();
            Vec3d offset = targetPoint.subtract(start);
            double distance = offset.length();
            if (distance > range || distance <= 0.001D) {
                continue;
            }
            double dot = direction.dotProduct(offset.normalize());
            if (dot < MyersConstants.BUTCHER_KNIFE_TARGET_DOT_MIN || !hasLineOfSight(viewer.getWorld(), start, targetPoint)) {
                continue;
            }
            if (distance < bestDistance) {
                best = target;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** 统计 G 键 HUD/吸收逻辑当前可以合法吸收的玩家数。 */
    public static int countAbsorbablePlayers(PlayerEntity viewer) {
        int count = 0;
        for (PlayerEntity target : viewer.getWorld().getPlayers()) {
            if (target != viewer
                    && GameFunctions.isPlayerAliveAndSurvival(target)
                    && !TimekeeperPlayerComponent.KEY.get(target).isInTimeRift()
                    && viewer.getEyePos().distanceTo(target.getEyePos()) <= MyersConstants.ABSORB_RANGE_BLOCKS
                    && isInView(viewer, target)
                    && hasLineOfSight(viewer.getWorld(), viewer.getEyePos(), target.getEyePos())) {
                count++;
            }
        }
        return count;
    }

    private static boolean isInView(PlayerEntity viewer, PlayerEntity target) {
        Vec3d direction = viewer.getRotationVector().normalize();
        Vec3d offset = target.getEyePos().subtract(viewer.getEyePos()).normalize();
        return direction.dotProduct(offset) >= MyersConstants.ABSORB_VIEW_DOT_MIN;
    }

    /** 判断两点之间是否被普通方块遮挡；Wathe 门方块只作为透明节点跳过。 */
    public static boolean hasLineOfSight(World world, Vec3d start, Vec3d end) {
        Vec3d delta = end.subtract(start);
        double length = delta.length();
        int steps = Math.max(1, (int) Math.ceil(length * MyersConstants.LINE_OF_SIGHT_SAMPLES_PER_BLOCK));
        for (int i = 1; i < steps; i++) {
            Vec3d point = start.add(delta.multiply(i / (double) steps));
            BlockPos pos = BlockPos.ofFloored(point);
            if (world.getBlockEntity(pos) instanceof DoorBlockEntity) {
                continue;
            }
            if (world.getBlockState(pos).getBlock() instanceof DoorPartBlock) {
                continue;
            }
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
