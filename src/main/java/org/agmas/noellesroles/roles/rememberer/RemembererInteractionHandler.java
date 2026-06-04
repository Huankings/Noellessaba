package org.agmas.noellesroles.roles.rememberer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

/**
 * 追忆者右键摸取回忆的服务端入口。
 */
public final class RemembererInteractionHandler {

    private static boolean initialized = false;

    private RemembererInteractionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity rememberer)) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }

            AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(rememberer);
            if (!validateRecallAttempt(rememberer, target, abilityComponent)) {
                return ActionResult.PASS;
            }

            RemembererPlayerComponent remembererComponent = RemembererPlayerComponent.KEY.get(rememberer);
            remembererComponent.clearAbilityStartCooldown();
            RemembererReplayBookBuilder.removeOldMemoryBooks(rememberer);
            rememberer.giveItemStack(RemembererReplayBookBuilder.createMemoryBook(rememberer, target));

            abilityComponent.setCooldown(RemembererConstants.RECALL_COOLDOWN_TICKS);

            GameRecordManager.recordGlobalEvent(
                    rememberer.getServerWorld(),
                    Noellesroles.REMEMBERER_RECALL_EVENT,
                    rememberer,
                    RemembererReplayBookBuilder.createRecallReplayExtra(target)
            );

            rememberer.sendMessage(
                    Text.translatable("message.noellesroles.rememberer.recalled", target.getDisplayName())
                            .withColor(Noellesroles.REMEMBERER.color()),
                    true
            );
            return ActionResult.CONSUME;
        });
    }

    /**
     * 统一给服务端与客户端准心判定复用的合法性检查。
     */
    public static boolean validateRecallAttempt(
            PlayerEntity rememberer,
            PlayerEntity target,
            AbilityPlayerComponent abilityComponent
    ) {
        if (rememberer == null || target == null || rememberer.isRemoved() || target.isRemoved()) {
            return false;
        }
        if (rememberer == target) {
            return false;
        }
        if (!rememberer.getWorld().getRegistryKey().equals(target.getWorld().getRegistryKey())) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(rememberer.getWorld());
        if (!gameWorld.isRole(rememberer, Noellesroles.REMEMBERER)) {
            return false;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(rememberer) || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return false;
        }
        if (!rememberer.getMainHandStack().isEmpty()) {
            return false;
        }
        if (abilityComponent.cooldown > 0) {
            return false;
        }
        if (!hasClearSight(rememberer, target)) {
            return false;
        }
        return validateRecallDistance(rememberer, target);
    }

    public static boolean validateRecallDistance(PlayerEntity rememberer, PlayerEntity target) {
        double maxDistance = RemembererConstants.RECALL_DISTANCE;
        if (!rememberer.getWorld().isClient()) {
            maxDistance += RemembererConstants.RECALL_SERVER_DISTANCE_TOLERANCE;
        }
        return rememberer.squaredDistanceTo(target) <= MathHelper.square(maxDistance);
    }

    public static boolean isRecallTargetEntity(PlayerEntity player, Entity entity) {
        return entity instanceof PlayerEntity target
                && validateRecallDistance(player, target)
                && hasClearSight(player, entity);
    }

    /**
     * 当前映射环境里没有直接暴露 PlayerEntity#hasLineOfSight，
     * 因此这里自行做一层“玩家到目标的方块遮挡”检测。
     *
     * <p>实现上会朝目标的几个代表点分别做一次方块射线：
     * 1. 眼睛位置；
     * 2. 碰撞箱中心；
     * 3. 下半身位置。
     *
     * <p>只要任意一条射线没有被方块挡住，就认为这次摸取在视觉上是成立的。
     * 这样既能避免被整堵墙隔着摸取，也能减少因为台阶、半砖等碰撞箱细节导致的误判。</p>
     */
    private static boolean hasClearSight(PlayerEntity observer, Entity target) {
        Vec3d start = observer.getCameraPosVec(1.0F);
        Box targetBox = target.getBoundingBox();
        Vec3d lowerBody = new Vec3d(target.getX(), targetBox.minY + target.getHeight() * 0.25D, target.getZ());

        return hasUnblockedRay(observer, start, target.getCameraPosVec(1.0F))
                || hasUnblockedRay(observer, start, targetBox.getCenter())
                || hasUnblockedRay(observer, start, lowerBody);
    }

    /**
     * 只检查“中途是否有方块阻挡”，不关心命中实体本身。
     * 因为上层已经确认目标实体就是当前准星指向/交互对象。
     */
    private static boolean hasUnblockedRay(PlayerEntity observer, Vec3d start, Vec3d end) {
        HitResult hitResult = observer.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                observer
        ));
        return hitResult.getType() == HitResult.Type.MISS;
    }
}
